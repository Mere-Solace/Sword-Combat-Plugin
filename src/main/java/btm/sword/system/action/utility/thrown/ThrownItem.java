package btm.sword.system.action.utility.thrown;

import btm.sword.Sword;
import btm.sword.system.entity.Combatant;
import btm.sword.system.entity.SwordEntity;
import btm.sword.system.entity.SwordEntityArbiter;
import btm.sword.system.entity.SwordPlayer;
import btm.sword.util.*;
import btm.sword.util.sound.SoundType;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Represents a thrown item entity that is actively simulated in the world.
 * <p>
 * Handles all aspects of the throw lifecycle — initialization, motion physics,
 * collision detection, and interaction outcomes such as hitting entities,
 * embedding in blocks, or being caught.
 */
@Getter
@Setter
public class ThrownItem {
	private final ItemDisplay display;
	private final Combatant thrower;
	private final ParticleWrapper blockTrail;
	
	private float xDisplayOffset;
	private float yDisplayOffset;
	private float zDisplayOffset;
	
	private Location origin;
	private Location cur;
	private Location prev;
	private Vector velocity;
	private double initialVelocity;
	
	int t = 0;
	private Function<Integer, Vector> positionFunction;
	private Function<Integer, Vector> velocityFunction;

	private boolean grounded;
	private Block stuckBlock;
	
	private boolean hit;
	private SwordEntity hitEntity;
	
	private boolean caught;
	
	private BukkitTask disposeTask;

    /**
     * Constructs a new thrown item instance with the given thrower and display.
     *
     * @param thrower The entity performing the throw.
     * @param display The {@link ItemDisplay} representing the thrown object.
     */
	public ThrownItem(Combatant thrower, ItemDisplay display) {
		this.thrower = thrower;
		this.display = display;
		
		blockTrail = display.getItemStack().getType().isBlock() ?
				new ParticleWrapper(Particle.BLOCK, 5, 0.25,  0.25,  0.25, display.getItemStack().getType().createBlockData()) :
				null;
		
		if (thrower instanceof SwordPlayer sp) {
			sp.setMainHandItemStackDuringThrow(sp.getMainItemStackAtTimeOfHold());
			sp.setOffHandItemStackDuringThrow(sp.getOffItemStackAtTimeOfHold());
		}
		else {
            thrower.setMainHandItemStackDuringThrow(thrower.getItemStackInHand(true));
            thrower.setOffHandItemStackDuringThrow(thrower.getItemStackInHand(false));
		}
        // Base values for where the ItemDisplay is held in relation to the player's eye location
		xDisplayOffset = -0.5f;
		yDisplayOffset = 0.1f;
		zDisplayOffset = 0.5f;
	}

    /**
     * Called when the item is primed to be thrown (held ready but not yet released).
     * <p>
     * Manages visual positioning, cancels premature throws, and displays in-hand effects.
     */
	public void onReady() {
        if (thrower instanceof SwordPlayer sp) {
            sp.setThrewItem(false);
            sp.setThrownItemIndex();

            // Interacting with an entity will cause the shield holding mechanic to falter
            if (sp.isInteractingWithEntity()) {
                sp.setAttemptingThrow(false);
                sp.setThrowSuccessful(true);
                // this throw should be weaker because it's automatic. Could turn into a lunge or thrust or smth else
                sp.getThrownItem().onRelease(2);
                thrower.setItemTypeInHand(Material.AIR, true);
                sp.endHoldingRight();
                sp.resetTree();
                return;
            }
        }

        determineOrientation();

		LivingEntity ex = thrower.entity();
		
		new BukkitRunnable() {
			int i = 0;
			int step = 0;
			@Override
			public void run() {
				if (thrower.isThrowCancelled()) {
					display.remove();
					ThrowAction.throwCancel(thrower);
					thrower.setThrownItem(null);
					cancel();
					return;
				}
				else if (thrower.isThrowSuccessful()) {
					thrower.setItemTypeInHand(Material.AIR, true);
//					ItemStack toReturn = thrower.getOffHandItemStackDuringThrow();
//					thrower.setItemStackInHand(toReturn, false);
					cancel();
					return;
				}
				
				if (thrower instanceof SwordPlayer sp) {
					if (!sp.isChangingHandIndex() && sp.getCurrentInvIndex() == sp.getThrownItemIndex()) {
						if (i < 10)
							sp.itemNameDisplay("- HURL IT AT 'EM SOLDIER! -", TextColor.color(100, 100, 100), null);
						else
							sp.itemNameDisplay("| HURL IT AT 'EM SOLDIER! |", TextColor.color(150, 150, 150), null);
						
						if (i > 20) i = 0;
						i++;
					}
				}
				
				ex.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 1, 2));

                DisplayUtil.smoothTeleport(display, 2);
                display.teleport(ex.getEyeLocation());

				step++;
			}
		}.runTaskTimer(Sword.getInstance(), 0L, 1L);
	}

    /**
     * Called when the item is released (actually thrown).
     * <p>
     * Initializes trajectory, physics parameters, and continuous motion updates.
     *
     * @param initialVelocity The starting velocity magnitude of the throw.
     */
	public void onRelease(double initialVelocity) {
        if (thrower instanceof SwordPlayer sp) {
            sp.setThrewItem(true);
            new BukkitRunnable() {
                @Override
                public void run() {
                    sp.setThrewItem(false);
                }
            }.runTaskLater(Sword.getInstance(), 2);
        }

        SoundUtil.playSound(thrower.entity(), SoundType.ENTITY_ENDER_DRAGON_FLAP, 0.5f, 0.4f);

        thrower.setItemStackInHand(ItemStack.of(Material.AIR), true);
		InteractiveItemArbiter.put(this);
		xDisplayOffset = yDisplayOffset = zDisplayOffset = 0;
		determineOrientation();

		this.initialVelocity = initialVelocity;
		LivingEntity ex = thrower.entity();
		Location o = ex.getEyeLocation();
		List<Vector> basis = VectorUtil.getBasisWithoutPitch(o);
		double phi = Math.toRadians(-1 * o.getPitch());
		double cosPhi = Math.cos(phi);
		double sinPhi = Math.sin(phi);
		double forwardCoefficient = initialVelocity*cosPhi;
		double upwardCoefficient = initialVelocity*sinPhi;
		origin = o.add(basis.getFirst().multiply(0.5))
				.add(basis.get(1).multiply(0.1))
				.add(basis.getLast().multiply(-0.25));
		cur = origin.clone();
		prev = cur.clone();
		Vector flatDir = thrower.getFlatDir().rotateAroundY(Math.PI/85);
		velocity = flatDir.clone();
		Vector forwardVelocity = flatDir.clone().multiply(forwardCoefficient);
		Vector upwardVelocity = VectorUtil.UP.clone().multiply(upwardCoefficient);
		
		double gravDamper = 46;
		
		positionFunction = t -> flatDir.clone().multiply(forwardCoefficient*t)
				.add(VectorUtil.UP.clone().multiply((upwardCoefficient*t)-(initialVelocity*(1/gravDamper)*t*t)));
		
		velocityFunction = t -> forwardVelocity.clone()
				.add(upwardVelocity.clone().add(VectorUtil.UP.clone().multiply(-initialVelocity*(2/(gravDamper))*t)));
		
		new BukkitRunnable() {
			@Override
			public void run() {
				if (grounded || hit || caught || display.isDead()) {
					onEnd();
					cancel();
					return;
				}
				
				cur = origin.clone().add(positionFunction.apply(t));
				velocity = velocityFunction.apply(t);

                if (!prev.equals(cur) && cur.clone().subtract(prev).toVector().dot(velocity) > 0) {
                    DisplayUtil.smoothTeleport(display, 1);
                }

                String name = display.getItemStack().getType().toString();
                if (name.endsWith("_SWORD")) {
                    display.teleport(cur.setDirection(velocity));
                }
                else {
                    display.teleport(cur.setDirection(basis.getLast()));
                }

                rotate();
				
				Cache.throwTrailParticle.display(cur);
				if (blockTrail != null && t % 3 == 0)
					blockTrail.display(cur);
				
				evaluate();
				prev = cur.clone();
				t++; // Step time value forward for next iteration
			}
		}.runTaskTimer(Sword.getInstance(), 0L, 1L);
	}

    /**
     * Applies appropriate rotation to the display based on item type.
     * <p>
     * Ensures visually realistic spin behavior per tool class.
     */
	private void rotate() {
		Transformation curTr = display.getTransformation();
		Quaternionf curRotation = curTr.getLeftRotation();
		Quaternionf newRotation;
		String name = display.getItemStack().getType().toString();
		if (name.endsWith("_SWORD")) {
			newRotation = curRotation;
		}
		else if (name.endsWith("_AXE") || name.endsWith("_HOE") || name.endsWith("_PICKAXE") || name.endsWith("_SHOVEL")) {
			newRotation = curRotation.rotateZ((float) -Math.PI/8);
		}
		else if (display.getItemStack().getType() == Material.SHIELD) {
			newRotation = curRotation.rotateX((float) -Math.PI/8);
		}
		else {
			newRotation = curRotation.rotateX((float) Math.PI/32);
		}
		
		display.setTransformation(
				new Transformation(
						curTr.getTranslation(),
						newRotation,
						curTr.getScale(),
						curTr.getRightRotation()
				)
		);
	}

    /**
     * Called once the throw has completed its trajectory or interaction.
     * <p>
     * Delegates to the correct outcome handler depending on state flags.
     */
	public void onEnd() {
		if (caught) onCatch();
		else if (hit) onHit();
		else if (grounded) onGrounded();
	}

    /**
     * Handles logic when the thrown item hits the ground or block.
     * <p>
     * Creates marker particles, positions the display, and schedules timed cleanup.
     */
	public void onGrounded() {
		if (stuckBlock != null)
			new ParticleWrapper(Particle.DUST_PILLAR, 50, 1, 1, 1, stuckBlock.getBlockData()).display(cur);

		double offset = 0.1;
		Vector step = velocity.normalize().multiply(offset);
		
		ArmorStand marker = (ArmorStand) display.getWorld().spawnEntity(cur, EntityType.ARMOR_STAND);
		marker.setGravity(false);
		marker.setVisible(false);
		
		int x = 1;
		while (!marker.getLocation().getBlock().isPassable()) { // changed from getType().isAir()
			marker.teleport(cur.clone().add(velocity.normalize().multiply(-0.1*x)));
			x++;
			if (x > 30) break;
		}
		
		new BukkitRunnable() {
			@Override
			public void run() {
				cur = marker.getLocation();
                DisplayUtil.smoothTeleport(display, 1);
				display.teleport(cur.clone().setDirection(velocityFunction.apply(t+1)));
				marker.remove();
			}
		}.runTaskLater(Sword.getInstance(), 1L);
		
		disposeTask = new BukkitRunnable() {
			int tick = 0;
			@Override
			public void run() {
				if (display.isDead()) {
//					thrower.message("   This item display has been slated for collection!");
					cancel();
				}
				
				if (tick >= 1000) {
					if (!display.isDead()) display.remove();
					cancel();
				}
				
				Cache.thrownItemMarkerParticle.display(cur.clone().add(step));
				Cache.thrownItemMarkerParticle.display(cur);
				Cache.thrownItemMarkerParticle.display(cur.clone().subtract(step));

				tick += 5;
			}
		}.runTaskTimer(Sword.getInstance(), 1L, 5L);
	}

    /**
     * Handles logic when the thrown item successfully hits a living entity.
     * <p>
     * Manages impalement, knockback, pinning, and delayed disposal.
     */
	public void onHit() {
		if (hitEntity == null) return;
		
		LivingEntity hit = hitEntity.entity();
		String name = display.getItemStack().getType().toString();
		if (name.endsWith("_SWORD") || name.endsWith("AXE")) {
			Vector kb = EntityUtil.isOnGround(hit) ?
					velocity.clone().multiply(0.7) :
					VectorUtil.getProjOntoPlane(velocity, VectorUtil.UP).multiply(1);
			
			impale(hit);
			hitEntity.hit(thrower, 0, 2, 75, 50, kb);
			
			new BukkitRunnable() {
				@Override
				public void run() {
					RayTraceResult pinnedBlock = hit.getWorld().rayTraceBlocks(
							hitEntity.getChestLocation(), velocity.clone().multiply(1.5),
                            0.5, FluidCollisionMode.NEVER,
                            true);
					
					if (pinnedBlock == null || pinnedBlock.getHitBlock() == null || pinnedBlock.getHitBlock().getType().isAir()) return;
					
					thrower.message("Pinned that infidel!");
					float yaw = cur.setDirection(velocity.clone().multiply(-1)).getYaw();
					hitEntity.entity().setBodyYaw(yaw);
					hitEntity.setPinned(true);
					new BukkitRunnable() {
						int i = 0;
						@Override
						public void run() {
							if (display.isDead() || i > 50) {
								hitEntity.setPinned(false);
								if (!display.isDead()) disposeNaturally();
								cancel();
							}
							hitEntity.entity().setBodyYaw(yaw);
							hitEntity.entity().setVelocity(new Vector());
							
							i += 2;
						}
					}.runTaskTimer(Sword.getInstance(), 0L, 2L);
				}
			}.runTaskLater(Sword.getInstance(), 3L);
			
			new BukkitRunnable() {
				@Override
				public void run() {
					if (display.isDead()) {
						hitEntity.removeImpalement();
						cancel();
					}
					else if (hitEntity.isDead()) {
						disposeNaturally();
						cancel();
					}
				}
			}.runTaskTimer(Sword.getInstance(), 0L, 1L);
		}
		else {
			hitEntity.hit(thrower, 0, 2, 75, 50, velocity.clone().multiply(0.7));
			hit.getWorld().createExplosion(hitEntity.getChestLocation(), 1, false, false);
            disposeNaturally();
		}
	}

    /**
     * Handles when the thrower catches their own thrown item mid-air.
     * <p>
     * Returns the item to inventory and disposes of the display.
     */
	public void onCatch() {
        thrower.message("Caught it!");
		thrower.giveItem(display.getItemStack());
		dispose();
	}

    /**
     * Evaluates the item’s current interaction state each tick.
     * <p>
     * Checks for collisions with entities or blocks.
     */
	public void evaluate() {
		if (hit || grounded || caught) return;
		hitCheck();
		groundedCheck();
	}

    /**
     * Checks if the thrown item has collided with a block and become grounded.
     */
	public void groundedCheck() {
		RayTraceResult hitBlock = display.getWorld().rayTraceBlocks(cur, velocity, initialVelocity, FluidCollisionMode.NEVER, true);
		
		if (hitBlock == null) return;
		
		if (hitBlock.getHitBlock() == null || hitBlock.getHitBlock().getType().isAir())
			return;
		
		grounded = true;
		stuckBlock = hitBlock.getHitBlock();
		cur = hitBlock.getHitPosition().toLocation(display.getWorld());
	}

    /**
     * Checks for collision with entities using ray tracing.
     * <p>
     * Determines whether the item hits an enemy or is caught by its thrower.
     */
	public void hitCheck() {
		Predicate<Entity> filter = entity -> (entity instanceof LivingEntity l) && !l.isDead() && l.getType() != EntityType.ARMOR_STAND;
		Predicate<Entity> effFilter = t < 20 ? entity -> filter.test(entity) && entity.getUniqueId() != thrower.getUniqueId() : filter;
		
		if (prev == null) disposeNaturally();
		
		RayTraceResult hitEntity = display.getWorld().rayTraceEntities(prev, velocity, initialVelocity, 0.5, effFilter);
		
		if (hitEntity == null) return;
		
		if (hitEntity.getHitEntity() == null) return;
		
		if (hitEntity.getHitEntity().getUniqueId() == thrower.getUniqueId()) {
			caught = true;
		}
		else {
			hit = true;
			this.hitEntity = SwordEntityArbiter.getOrAdd(hitEntity.getHitEntity().getUniqueId());
		}
	}

    /**
     * Determines the correct {@link Transformation} for the item display based on its type.
     * <p>
     * Ensures proper orientation in-hand and mid-flight.
     */
	public void determineOrientation() {
		String name = display.getItemStack().getType().toString();
		Vector3f base = new Vector3f(xDisplayOffset, yDisplayOffset, zDisplayOffset);
		if (name.endsWith("_SWORD")) {
			display.setTransformation(new Transformation(
					base.add(new Vector3f()),
					new Quaternionf()
							.rotateY((float) Math.PI/2)
							.rotateZ((float) Math.PI/2),
					new Vector3f(1,1,1),
					new Quaternionf()
			));
		}
		else if (name.endsWith("AXE") || name.endsWith("_HOE") || name.endsWith("_SHOVEL")) {
			display.setTransformation(new Transformation(
					base.add(new Vector3f()),
					new Quaternionf().rotateY((float) -Math.PI/2)
							.rotateZ((float) Math.PI/4),
					new Vector3f(1.5f,1.5f,1.5f),
					new Quaternionf()
			));
		}
		else if (display.getItemStack().getType() == Material.SHIELD) {
			display.setTransformation(new Transformation(
					base.add(new Vector3f(0,0,0)),
					new Quaternionf().rotateY((float) (Math.PI/1.01f) * 0),
					new Vector3f(1,1,1),
					new Quaternionf()
			));
		}
		else {
			display.setTransformation(new Transformation(
					base.add(new Vector3f()),
					new Quaternionf().rotateZ((float) Math.PI/8),
					new Vector3f(1,1,1),
					new Quaternionf()
			));
		}
	}

    /**
     * Impales a {@link LivingEntity} when struck, embedding the item visually and applying follow behavior.
     *
     * @param hit The living entity being impaled.
     */
	public void impale(LivingEntity hit) {
		hitEntity.addImpalement();
		
		double max = hit.getEyeLocation().getY();
		double feet = hit.getLocation().getY();
		double min = feet + hitEntity.getEyeHeight()*0.2;
		
		double diff = cur.getY();
		double worldOffset = Math.min(Math.max(diff, min), max);
		double heightOffset = worldOffset - feet;
		
		boolean followHead = hitEntity.entity().getType() != EntityType.SPIDER && diff >= max;
		EntityUtil.itemDisplayFollow(hitEntity, display,  velocity.clone().normalize(), heightOffset, followHead);
	}

    /**
     * Disposes of the item by naturally dropping its item form into the world.
     * <p>
     * Used after hitting entities or ending its trajectory naturally.
     */
	public void disposeNaturally() {
		Location dropLoc = hitEntity != null ? hitEntity.entity().getLocation() : display.getLocation();
        Item dropped = hitEntity.entity().getWorld().dropItemNaturally(hitEntity.entity().getLocation(), display.getItemStack());
        new BukkitRunnable() {
            @Override
            public void run() {
                if (dropped.isDead()) {
                    cancel();
                }
                Cache.thrownItemMarkerParticle2.display(dropped.getLocation());
            }
        }.runTaskTimer(Sword.getInstance(), 0L, 5L);
		display.remove();
		if (disposeTask != null && !disposeTask.isCancelled()) disposeTask.cancel();
	}

    /**
     * Cleanly disposes of the item display and cancels any running tasks.
     * <p>
     * Should be called when the thrown item is collected or deleted.
     */
	public void dispose() {
		display.remove();
		if (disposeTask != null && !disposeTask.isCancelled()) disposeTask.cancel();
	}
}
