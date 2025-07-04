package btm.sword.system.action.utility.thrown;

import btm.sword.Sword;
import btm.sword.system.entity.Combatant;
import btm.sword.system.entity.SwordEntity;
import btm.sword.system.entity.SwordEntityArbiter;
import btm.sword.system.entity.SwordPlayer;
import btm.sword.util.*;
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

public class ThrownItem {
	private final ItemDisplay display;
	private final Combatant thrower;
	private final boolean mainHandThrow;
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

	public ThrownItem(Combatant thrower, ItemDisplay display, boolean mainHandThrow) {
		this.thrower = thrower;
		this.display = display;
		this.mainHandThrow = mainHandThrow;
		
		blockTrail = display.getItemStack().getType().isBlock() ?
				new ParticleWrapper(Particle.BLOCK, 5, 0.25,  0.25,  0.25, display.getItemStack().getType().createBlockData()) :
				null;
		
		thrower.setMainHandItemStackDuringThrow(thrower.getItemStackInHand(true));
		thrower.setOffHandItemStackDuringThrow(thrower.getItemStackInHand(false));
		xDisplayOffset = mainHandThrow ? -1 : 1;
		yDisplayOffset = 0.1f;
		zDisplayOffset = -0.25f;
	}
	
	public void onReady() {
		determineOrientation();
		
		if (thrower instanceof SwordPlayer sp) {
			sp.setThrownItemIndex();
		}
		
		thrower.setItemTypeInHand(Material.GUNPOWDER, true);
		thrower.setItemTypeInHand(Material.GUNPOWDER, false);
		
		LivingEntity ex = thrower.entity();
		
		int[] step = {0};
		new BukkitRunnable() {
			@Override
			public void run() {
				if (thrower.isThrowCancelled()) {
					display.remove();
					ThrowAction.throwCancel(thrower);
					thrower.setThrownItem(null);
					cancel();
				}
				else if (thrower.isThrowSuccessful()) {
					thrower.setItemTypeInHand(Material.AIR, mainHandThrow);
					ItemStack toReturn = !mainHandThrow ? thrower.getMainHandItemStackDuringThrow() : thrower.getOffHandItemStackDuringThrow();
					thrower.setItemStackInHand(toReturn, !mainHandThrow);
					cancel();
				}
				ex.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 1, 2));
				
				if (step[0] % 2 == 0) {
					display.teleport(ex.getEyeLocation());
				}
				step[0]++;
			}
		}.runTaskTimer(Sword.getInstance(), 0L, 1L);
	}
	
	public void onRelease(double initialVelocity) {
		InteractiveItemArbiter.put(this);
		xDisplayOffset = yDisplayOffset = zDisplayOffset = 0;
		determineOrientation();
		
		this.initialVelocity = initialVelocity;
		LivingEntity ex = thrower.entity();
		Location o = ex.getEyeLocation();
		List<Vector> basis = VectorUtil.getBasisWithoutPitch(o);
		double phi = Math.toRadians(-1 * o.getPitch());
		thrower.message("Pitch rads: " + phi);
		double cosPhi = Math.cos(phi);
		double sinPhi = Math.sin(phi);
		double forwardCoeff = initialVelocity*cosPhi;
		double upwardCoeff = initialVelocity*sinPhi;
		origin = o.add(basis.getFirst().multiply(mainHandThrow ? 1 : -1))
				.add(basis.get(1).multiply(0.1))
				.add(basis.getLast().multiply(-0.25));
		cur = origin.clone();
		prev = cur.clone();
		Vector flatDir = thrower.getFlatDir();
		Vector forwardVelocity = flatDir.clone().multiply(forwardCoeff);
		Vector upwardVelocity = VectorUtil.UP.clone().multiply(upwardCoeff);
		
		double gravDamper = 46;
		
		positionFunction = t -> flatDir.clone().multiply(forwardCoeff*t)
				.add(VectorUtil.UP.clone().multiply((upwardCoeff*t)-(initialVelocity*(1/gravDamper)*t*t)));
		
		velocityFunction = t -> forwardVelocity.clone()
				.add(upwardVelocity.clone().add(VectorUtil.UP.clone().multiply(-initialVelocity*(2/(gravDamper))*t)));
		
		new BukkitRunnable() {
			@Override
			public void run() {
				if (grounded || hit || caught || display.isDead()) {
					onEnd();
					cancel();
				}
				cur = origin.clone().add(positionFunction.apply(t));
				velocity = velocityFunction.apply(t);
				display.teleport(cur.setDirection(velocity));
				rotate();
				
				Cache.throwTrailParticle.display(cur);
				if (blockTrail != null && t % 3 == 0)
					blockTrail.display(cur);
				
				evaluate();
				prev = cur.clone();
				t++;
			}
		}.runTaskTimer(Sword.getInstance(), 0L, 1L);
	}
	
	private void rotate() {
		Transformation curTr = display.getTransformation();
		Quaternionf curRotation = curTr.getLeftRotation();
		Quaternionf newRotation;
		String name = display.getItemStack().getType().toString();
		if (name.endsWith("_SWORD")) {
			newRotation = curRotation;
		}
		else if (name.endsWith("_AXE")) {
			newRotation = curRotation.rotateZ((float) -Math.PI/8);
		}
		else if (display.getItemStack().getType() == Material.SHIELD) {
			newRotation = curRotation.rotateZ((float) -Math.PI/8);
		}
		else {
			newRotation = curRotation.rotateX((float) Math.PI/8);
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
	
	public void onEnd() {
		if (caught) onCatch();
		else if (grounded) onGrounded();
		else if (hit) onHit();
		
		thrower.message("Throw method is now over");
	}
	
	public void onGrounded() {
		thrower.message("Entered ground");
		
		Vector step = velocity.normalize().multiply(0.5);
		int[] i = {0};
		new BukkitRunnable() {
			@Override
			public void run() {
				if (i[0] > 10 || cur.clone().add(step).getBlock().isPassable()) {
					display.teleport(cur.setDirection(velocity));
					cancel();
				}
				
				cur.subtract(step);
				Cache.testSwingParticle.display(cur);
				
				
				i[0]++;
			}
		}.runTaskTimer(Sword.getInstance(), 0L, 1L);

		
		
		int[] x = {0};
		disposeTask = new BukkitRunnable() {
			@Override
			public void run() {
				if (display.isDead()) {
					thrower.message("   This item display has been slated for collection!");
					cancel();
				}
				
				if (x[0] >= 1000) {
					if (!display.isDead()) display.remove();
					cancel();
				}
				
				Cache.thrownItemMarkerParticle.display(cur);
				Cache.thrownItemMarkerParticle.display(cur.clone().subtract(step));

				x[0] += 5;
			}
		}.runTaskTimer(Sword.getInstance(), 0L, 5L);
	}
	
	public void onHit() {
		thrower.message("Hit entity");
		if (hitEntity == null) return;
		
		LivingEntity hit = hitEntity.entity();
		if (display.getItemStack().getType().toString().endsWith("_SWORD") || display.getItemStack().getType().toString().endsWith("_AXE")) {
			Vector kb = EntityUtil.isOnGround(hit) ?
					velocity.clone().multiply(0.7) :
					VectorUtil.getProjOntoPlan(velocity, VectorUtil.UP).multiply(1);
			
			impale(hit);
			hitEntity.hit(thrower, 0, 2, 75, 50, kb);
			
			new BukkitRunnable() {
				@Override
				public void run() {
					RayTraceResult pinnedBlock = hit.getWorld().rayTraceBlocks(
							hitEntity.getChestLocation(), velocity, 0.5, FluidCollisionMode.NEVER, true);
					
					if (pinnedBlock == null || pinnedBlock.getHitBlock() == null || pinnedBlock.getHitBlock().getType().isAir()) return;
					
					thrower.message("Pinned that infidel!");
					hitEntity.setPinned(true);
					new BukkitRunnable() {
						@Override
						public void run() {
							if (!display.isDead()) {
								hitEntity.setPinned(false);
								disposeNaturally();
							}
						}
					}.runTaskLater(Sword.getInstance(), 70L);
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
			disposeNaturally();
		}
	}
	
	public void onCatch() {
		thrower.message("Caught that thang");
		thrower.giveItem(display.getItemStack());
		dispose();
	}
	
	public void evaluate() {
		if (hit || grounded || caught) return;
		hitCheck();
		groundedCheck();
	}
	
	public void groundedCheck() {
		RayTraceResult hitBlock = display.getWorld().rayTraceBlocks(cur, velocity, initialVelocity, FluidCollisionMode.NEVER, false);
		
		if (hitBlock == null) return;
		
		if (hitBlock.getHitBlock() == null || hitBlock.getHitBlock().getType().isAir())
			return;
		
		grounded = true;
		stuckBlock = hitBlock.getHitBlock();
	}
	
	public void hitCheck() {
		Predicate<Entity> filter = entity -> (entity instanceof LivingEntity l) && !l.isDead() && l.getType() != EntityType.ARMOR_STAND;
		Predicate<Entity> effFilter = t < 20 ? entity -> filter.test(entity) && entity.getUniqueId() != thrower.uuid() : filter;
		
		RayTraceResult hitEntity = display.getWorld().rayTraceEntities(prev, velocity, initialVelocity, 0.6, effFilter);
		
		if (hitEntity == null) return;
		
		if (hitEntity.getHitEntity() == null) return;
		
		if (hitEntity.getHitEntity().getUniqueId() == thrower.uuid()) {
			caught = true;
		}
		else {
			hit = true;
			this.hitEntity = SwordEntityArbiter.getOrAdd(hitEntity.getHitEntity().getUniqueId());
		}
	}
	
	public void determineOrientation() {
		String itemName = display.getItemStack().getType().toString();
		Vector3f base = new Vector3f(xDisplayOffset, yDisplayOffset, zDisplayOffset);
		if (itemName.endsWith("_SWORD")) {
			display.setTransformation(new Transformation(
					base.add(new Vector3f()),
					new Quaternionf()
							.rotateY((float) Math.PI/2)
							.rotateZ((float) Math.PI/2),
					new Vector3f(1,1,1),
					new Quaternionf()
			));
		}
		else if (itemName.endsWith("_AXE")) {
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
					new Quaternionf(),
					new Vector3f(1,1,1),
					new Quaternionf()
			));
		}
		else {
			display.setTransformation(new Transformation(
					base.add(new Vector3f()),
					new Quaternionf(),
					new Vector3f(1,1,1),
					new Quaternionf()
			));
		}
	}
	
	public void impale(LivingEntity hit) {
		hitEntity.addImpalement();
		double max = hit.getEyeLocation().getY();
		double feet = hit.getLocation().getY();
		double min = feet + hitEntity.getEyeHeight()*0.2;
		double diff = cur.getY();
		double worldOffset = Math.min(Math.max(diff, min), max);
		double heightOffset = worldOffset - feet;
		
		EntityUtil.itemDisplayFollowTest(hitEntity, display,  velocity.clone().normalize(), heightOffset);
	}
	
	public void disposeNaturally() {
		Location dropLoc = hitEntity != null ? hitEntity.entity().getLocation() : display.getLocation();
		display.getWorld().dropItemNaturally(dropLoc, display.getItemStack());
		display.remove();
		if (disposeTask != null && !disposeTask.isCancelled()) disposeTask.cancel();
	}
	
	public void dispose() {
		display.remove();
		if (disposeTask != null && !disposeTask.isCancelled()) disposeTask.cancel();
	}
	
	public ItemDisplay getDisplay() {
		return display;
	}
	
	public boolean isMainHandThrow() {
		return mainHandThrow;
	}
}
