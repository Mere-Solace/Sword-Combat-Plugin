package btm.sword.system.action.utility.thrown;

import btm.sword.Sword;
import btm.sword.system.entity.Combatant;
import btm.sword.system.entity.SwordEntity;
import btm.sword.system.entity.SwordEntityArbiter;
import btm.sword.util.*;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.function.Function;
import java.util.function.Predicate;

public class ThrownItem {
	private ItemDisplay display;
	private Combatant thrower;
	
	private final ParticleWrapper blockTrail;
	
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

	public ThrownItem(Combatant thrower, ItemDisplay display) {
		this.thrower = thrower;
		this.display = display;
		
		blockTrail = display.getItemStack().getType().isBlock() ?
				new ParticleWrapper(Particle.BLOCK, 5, 0.25,  0.25,  0.25, display.getItemStack().getType().createBlockData()) :
				null;
	}
	
	// when the item is thrown
	public void onRelease(Combatant executor, double initialVelocity) {
		display = executor.getThrownItemDisplay();
		determineOrientation(display.getItemStack());
		thrower = executor;
		
		InteractiveItemArbiter.put(this);
		
		this.initialVelocity = initialVelocity;
		LivingEntity ex = executor.entity();
		Location o = ex.getEyeLocation();
		double phi = Math.toRadians(-1 * o.getPitch());
		executor.message("Pitch rads: " + phi);
		double cosPhi = Math.cos(phi);
		double sinPhi = Math.sin(phi);
		double forwardCoeff = initialVelocity*cosPhi;
		double upwardCoeff = initialVelocity*sinPhi;
		origin = o.clone();
		cur = origin.clone();
		prev = cur.clone();
		Vector flatDir = executor.getFlatDir();
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
		if (display.getItemStack().getType().toString().endsWith("_SWORD")) {
			newRotation = curRotation;
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
		else if (hit) onHit();
		
		if (grounded) onGrounded();
		
		thrower.message("Throw method is now over");
	}
	
	public void onGrounded() {
		thrower.message("Entered ground");
		int i = 0;
		Vector step = velocity.normalize().multiply(0.5);
		thrower.message("Lodged BlockType: " + cur.clone().add(step).getBlock().getType());
		while (!cur.clone().add(step).getBlock().getType().isAir()) {
			cur.subtract(step);
			i++;
			if (i > 30) {
				thrower.message("   Exceeded step limit of placement check");
				break;
			}
		}
		thrower.message("      tp'ing item display to the location");
		display.teleport(cur.setDirection(velocity));
		
		disposeTask = new BukkitRunnable() {
			@Override
			public void run() {
				if (!display.isDead()) {
					display.remove();
				}
			}
		}.runTaskLater(Sword.getInstance(), 1000L);
	}
	
	public void onHit() {
		thrower.message("Hit entity");
		if (hitEntity == null) return;
		
		LivingEntity hit = hitEntity.entity();
		if (display.getItemStack().getType().toString().endsWith("_SWORD")) {
			Vector to = hitEntity.getChestLocation().clone().subtract(cur).toVector().normalize();
			Vector kb = EntityUtil.isOnGround(hit) ? to.multiply(2) : VectorUtil.getProjOntoPlan(to, VectorUtil.UP).multiply(3);
			
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
			Vector kb = hitEntity.getChestLocation().clone().subtract(cur).toVector().normalize();
			hitEntity.hit(thrower, 0, 2, 75, 50, kb);
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
	
	public void determineOrientation(ItemStack itemStack) {
		String itemName = itemStack.getType().toString();
		if (itemName.endsWith("_SWORD")) {
			display.setTransformation(new Transformation(
					new Vector3f(),
					new Quaternionf()
							.rotateY((float) Math.PI/2)
							.rotateZ((float) Math.PI/2),
					new Vector3f(1,1,1),
					new Quaternionf()
			));
		}
		else if (itemName.endsWith("_AXE")) {
			display.setTransformation(new Transformation(
					new Vector3f(),
					new Quaternionf()
							.rotateY((float) Math.PI)
							.rotateZ((float) Math.PI/2),
					new Vector3f(1,1,1),
					new Quaternionf()
			));
		}
		else if (itemStack.getType() == Material.SHIELD) {
			display.setTransformation(new Transformation(
					new Vector3f(1, 0, 0),
					new Quaternionf()
							.rotateY((float) Math.PI)
							.rotateZ((float) Math.PI/2),
					new Vector3f(1,1,1),
					new Quaternionf()
			));
		}
		else {
			display.setTransformation(new Transformation(
					new Vector3f(),
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
	
	public Block getStuckBlock() {
		return stuckBlock;
	}
	
	public void setStuckBlock(Block stuckBlock) {
		this.stuckBlock = stuckBlock;
	}
	
	public boolean isCaught() {
		return caught;
	}
	
	public void setCaught(boolean caught) {
		this.caught = caught;
	}
	
	public boolean isHit() {
		return hit;
	}
	
	public void setHit(boolean hit) {
		this.hit = hit;
	}
	
	public boolean isGrounded() {
		return grounded;
	}
	
	public void setGrounded(boolean grounded) {
		this.grounded = grounded;
	}
	
	public Combatant getThrower() {
		return thrower;
	}
	
	public void setThrower(Combatant thrower) {
		this.thrower = thrower;
	}
	
	public Location getCur() {
		return cur;
	}
	
	public void setCur(Location cur) {
		this.cur = cur;
	}
}
