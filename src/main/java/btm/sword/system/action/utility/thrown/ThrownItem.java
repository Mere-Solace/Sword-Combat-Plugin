package btm.sword.system.action.utility.thrown;

import btm.sword.Sword;
import btm.sword.system.entity.Combatant;
import btm.sword.system.entity.SwordEntity;
import btm.sword.system.entity.SwordEntityArbiter;
import btm.sword.util.*;
import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
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

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

public class ThrownItem {
	private ItemDisplay display;
	private Combatant thrower;
	
	private final ParticleWrapper blockTrail;
	
	private Location origin;
	private Location cur;
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
		
		this.initialVelocity = initialVelocity;
		LivingEntity ex = executor.entity();
		Location o = ex.getEyeLocation();
		double phi = Math.toRadians(o.getPitch());
		double cosPhi = Math.cos(phi);
		double sinPhi = Math.sin(phi);
		double forwardCoeff = initialVelocity*cosPhi;
		double upwardCoeff = initialVelocity*sinPhi;
		origin = o.clone();
		Vector flatDir = executor.getFlatDir();
		Vector forwardVelocity = flatDir.clone().multiply(forwardCoeff);
		Vector upwardVelocity = VectorUtil.UP.clone().multiply(upwardCoeff);
		
		positionFunction = t -> flatDir.clone().multiply(forwardCoeff*t)
				.add(VectorUtil.UP.clone().multiply((upwardCoeff*t)-(initialVelocity*0.2*t*t)));
		
		velocityFunction = t -> forwardVelocity.clone()
				.add(upwardVelocity.clone().add(VectorUtil.UP.clone().multiply(-initialVelocity*0.1*t)));
		
		new BukkitRunnable() {
			@Override
			public void run() {
				evaluate();
				if (grounded || hit || caught || display.isDead()) {
					onEnd();
					cancel();
				}
				cur = origin.clone().add(positionFunction.apply(t));
				velocity = velocityFunction.apply(t);
				display.teleport(cur.setDirection(velocity));
				
				Cache.throwTrailParticle.display(cur);
				if (blockTrail != null && t % 3 == 0)
					blockTrail.display(cur);
				
				t++;
			}
		}.runTaskTimer(Sword.getInstance(), 0L, 1L);
	}
	
	public void onEnd() {
		if (caught) onCatch();
		else if (hit) onHit();
		
		if (grounded) onGrounded();
		
		thrower.message("Throw method is now over");
	}
	
	public void onGrounded() {
		int i = 0;
		Vector step = velocity.normalize().multiply(0.25);
		while (!cur.clone().add(step).getBlock().getType().isAir()) {
			cur.subtract(step);
			i++;
			if (i > 30) break;
		}
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
		LivingEntity hit = hitEntity.entity();
		if (display.getItemStack().getType().toString().endsWith("_SWORD")) {
			Vector to = hitEntity.getChestLocation().clone().subtract(cur).toVector().normalize();
			Vector kb = EntityUtil.isOnGround(hit) ? to.multiply(2) : VectorUtil.getProjOntoPlan(to, VectorUtil.UP).multiply(3);
			
			hitEntity.hit(thrower, 0, 2, 75, 50, kb);
			impale(hit);
			
			new BukkitRunnable() {
				@Override
				public void run() {
					RayTraceResult pinnedBlock = hit.getWorld().rayTraceBlocks(
							hitEntity.getChestLocation(), velocity, 0.5, FluidCollisionMode.NEVER, true);
					
					if (pinnedBlock == null || pinnedBlock.getHitBlock() == null || pinnedBlock.getHitBlock().getType().isAir()) return;
					
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
			
			double heightOffset = (cur.getY() - hit.getLocation().getY())*0.85;
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
					
					if (Bukkit.getCurrentTick() % 2 == 0)
						DisplayUtil.line(List.of(Cache.basicSwordBlueTransitionParticle),
								hit.getLocation().add(VectorUtil.UP.clone().multiply(heightOffset)),
								velocity,
								2, 0.2);
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
		thrower.giveItem(display.getItemStack());
		dispose();
	}
	
	public void evaluate() {
		hitCheck();
		groundedCheck();
	}
	
	public void groundedCheck() {
		RayTraceResult hitBlock = display.getWorld().rayTraceBlocks(cur, velocity, initialVelocity, FluidCollisionMode.NEVER, false);
		if (hitBlock == null || hitBlock.getHitBlock() == null || hitBlock.getHitBlock().getType().isAir())
			return;
		
		grounded = true;
		stuckBlock = hitBlock.getHitBlock();
	}
	
	public void hitCheck() {
		Predicate<Entity> filter = entity -> !entity.isDead() && entity.getType() != EntityType.ARMOR_STAND;
		Predicate<Entity> effFilter = t < 20 ? entity -> filter.test(entity) && entity.getUniqueId() != thrower.uuid() : filter;
		
		RayTraceResult hitEntity = display.getWorld().rayTraceEntities(cur, velocity, initialVelocity, 1, effFilter);
		
		if (hitEntity == null || hitEntity.getHitEntity() == null)
			return;
		
		if (hitEntity.getHitEntity().getUniqueId() == thrower.uuid()) {
			caught = true;
			onCatch();
		}
		else {
			hit = true;
			this.hitEntity = SwordEntityArbiter.getOrAdd(hitEntity.getHitEntity().getUniqueId());
		}
	}
	
	public void determineOrientation(ItemStack itemStack) {
		if (itemStack.getType().toString().endsWith("_SWORD")) {
			display.setTransformation(new Transformation(
					new Vector3f(),
					new Quaternionf()
							.rotateY((float) Math.PI/2)
							.rotateZ((float) Math.PI/2),
					new Vector3f(),
					new Quaternionf()
			));
		}
	}
	
	public void impale(LivingEntity hit) {
		hitEntity.addImpalement();
		Location h = hit.getLocation();

		double entityYawRads = Math.toRadians(hit.getBodyYaw());
		Vector entityDir = new Vector(-Math.sin(entityYawRads), 0, Math.cos(entityYawRads));
		
		List<Vector> basis = VectorUtil.getBasis(h, entityDir);
		Vector right = basis.getFirst();
		Vector forward = basis.getLast();
		
		Vector proj = VectorUtil.getProjOntoPlan(velocity, VectorUtil.UP).normalize();
		boolean clockwise = proj.dot(right) >= 0;
		double relativeYawOffset = Math.acos(forward.dot(proj));
		double effectiveOffset = clockwise ? relativeYawOffset : -1 * relativeYawOffset;
		
		double heightOffset = (cur.getY() - hit.getLocation().getY())*0.85;
		
		EntityUtil.itemDisplayFollowTest(hitEntity, display, heightOffset, effectiveOffset);
	}
	
	public void disposeNaturally() {
		display.getWorld().dropItemNaturally(cur, display.getItemStack());
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
