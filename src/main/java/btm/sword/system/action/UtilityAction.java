package btm.sword.system.action;

import btm.sword.Sword;
import btm.sword.system.entity.Combatant;
import btm.sword.system.entity.SwordEntity;
import btm.sword.system.entity.SwordPlayer;
import btm.sword.system.entity.aspect.AspectType;
import btm.sword.system.entity.SwordEntityArbiter;
import btm.sword.system.entity.display.InteractiveItemArbiter;
import btm.sword.util.Cache;
import btm.sword.util.HitboxUtil;
import btm.sword.util.ParticleWrapper;
import btm.sword.util.VectorUtil;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.*;
import org.bukkit.inventory.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public class UtilityAction extends SwordAction {
	
	public static void grab(Combatant executor) {
		cast(executor, 0L,
			new BukkitRunnable() {
			@Override
			public void run() {
				int baseDuration = 60;
				double baseGrabRange = 3;
				double baseGrabThickness = 0.3;
				
				long duration = (long) executor.calcValueAdditive(AspectType.MIGHT, 100L, baseDuration, 0.2);
				double range = executor.calcValueAdditive(AspectType.WILLPOWER, 4.5, baseGrabRange, 0.1);
				double grabThickness = executor.calcValueAdditive(AspectType.WILLPOWER, 0.75, baseGrabThickness, 0.1);
				
				LivingEntity ex = executor.entity();
				Location o = ex.getEyeLocation();

				LivingEntity target = HitboxUtil.lineFirst(ex, o, o.getDirection(), range, grabThickness);
				if (target == null) {
					return;
				}
				
				if (target.getType() == EntityType.ARMOR_STAND) {
					BlockData lodgedBlockData = Material.AIR.createBlockData();
					RayTraceResult result = target.getWorld().rayTraceBlocks(target.getLocation(), new Vector(0, -1, 0), 2);
					Block b = null;
					if (result != null) b = result.getHitBlock();
					if (b != null) lodgedBlockData = b.getBlockData();
					InteractiveItemArbiter.onPickup((ArmorStand) target, executor, lodgedBlockData);
					return;
				}
				
				SwordEntity swordTarget = SwordEntityArbiter.getOrAdd(target.getUniqueId());
				
				executor.onGrab(swordTarget);
				
				final int[] ticks = {0};
				new BukkitRunnable() {
					@Override
					public void run() {
						if (ticks[0] >= duration - 1 || target.isDead()) {
							executor.onGrabLetGo();
							cancel();
							return;
						}
						if (!executor.isGrabbing()) {
							executor.onGrabThrow();
							cancel();
							return;
						}
						
						Vector v = ex.getVelocity();
						ex.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 2, 3));
						ex.setVelocity(new Vector(v.getX() * 0.2, v.getY(),v.getZ() * 0.2));
						
						double holdDist = 2;
						Vector direction = ex.getLocation().toVector().add(ex.getEyeLocation().getDirection().multiply(holdDist)).subtract(target.getLocation().toVector());
						double distanceSquared = direction.lengthSquared();
						double bufferDistance = 0.4;
						double pullSpeed = 0.6;
						
						if (distanceSquared < bufferDistance*bufferDistance) {
							target.setVelocity(new Vector(0,target.getVelocity().getY()*0.25,0));
						}
						else {
							double force = pullSpeed;
							if (Math.abs(target.getEyeLocation().getY() - ex.getEyeLocation().getY()) > 1.2) {
								force *= 2;
							}
							Vector velocity = direction.normalize().multiply(force);
							if (Double.isFinite(velocity.getX()) && Double.isFinite(velocity.getY()) && Double.isFinite(velocity.getZ())) {
								target.setVelocity(velocity);
							}
						}
						ticks[0]++;
					}
				}.runTaskTimer(Sword.getInstance(), 0, 1);
			}
		});
	}
	
	public static void throwReady(Combatant executor) {
		float scale = 1.05f;
		float xOffset = -0.75f;
		float yOffset = 0.1f;
		float zOffset = 0;
		
		int indexOfThrown = -1;
		
		executor.setAttemptingThrow(true);
		executor.setThrowCancelled(false);
		executor.setThrowSuccessful(false);
		
		if (executor.getItemTypeInHand(false) == Material.SHIELD) {
			executor.setItemTypeInHand(Material.BAMBOO_BUTTON, false);
		}
		
		ItemStack thrownItem = executor.getItemStackInHand(true);
		if (executor instanceof SwordPlayer sp) {
			sp.setThrownItemIndex();
			indexOfThrown = sp.getThrownItemIndex();
			executor.message("Index of item thrown: " + indexOfThrown);
			executor.setItemTypeInHand(Material.GUNPOWDER, true);
		}
		else {
			executor.setItemTypeInHand(Material.AIR, true);
		}
		executor.setThrownItemStack(thrownItem);
		
		LivingEntity ex = executor.entity();
		World world = ex.getWorld();
		
		ItemDisplay itemDisplay = (ItemDisplay) world.spawnEntity(ex.getEyeLocation(), EntityType.ITEM_DISPLAY);
		itemDisplay.setItemStack(thrownItem);
		
		executor.setThrownItemDisplay(itemDisplay);
		
		Transformation tr = new Transformation(
				new Vector3f(xOffset,yOffset,zOffset),
				new Quaternionf().rotateX((float) (-Math.PI/3)).rotateY((float) (Math.PI/1.5)),
				new Vector3f(scale,scale,scale),
				new Quaternionf());
		itemDisplay.setTransformation(tr);
		
		int[] step = {0};
		int index = indexOfThrown;
		new BukkitRunnable() {
			@Override
			public void run() {
				if (executor instanceof SwordPlayer sp && (sp.isThrowCancelled() || sp.isThrowSuccessful())) {
					if (sp.isThrowCancelled()) {
						itemDisplay.remove();
						throwCancel(executor);
					}
					else if (index != -1) {
						((Player) sp.entity()).getInventory().setItem(index, new ItemStack(Material.AIR));
					}
					cancel();
				}
				ex.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 1, 2));
				
				if (step[0] % 3 == 0) {
					itemDisplay.teleport(ex.getEyeLocation());
				}
				step[0]++;
			}
		}.runTaskTimer(Sword.getInstance(), 0L, 1L);
	}
	
	public static void throwCancel(Combatant executor) {
		executor.setAttemptingThrow(false);
		executor.setThrowCancelled(true);
		executor.setThrowSuccessful(false);
		if (executor instanceof SwordPlayer sp){
			sp.setItemAtIndex(sp.getThrownItemStack(), sp.getThrownItemIndex());
		}
		else {
			executor.setItemStackInHand(executor.getThrownItemStack(), true);
		}
	}
	
	public static void throwItem(Combatant executor) {
		executor.setAttemptingThrow(false);
		executor.setThrowSuccessful(true);
		cast(executor, 10L, new BukkitRunnable() {
			@Override
			public void run() {
				double force = 1.5;
				float scale = 1.05f;
				double xOffset = 0.75;
				double yOffset = 0.2;
				double zOffset = -0.2;
				
				LivingEntity ex = executor.entity();
				World world = ex.getWorld();
				
				ItemStack thrownItem = executor.getThrownItemStack();
				
				BlockData thrownBlockData = null;
				ParticleWrapper blockTrail = null;
				if (thrownItem.getType().isBlock()) {
					thrownBlockData = thrownItem.getType().createBlockData();
					blockTrail = new ParticleWrapper(Particle.BLOCK, 5, scale / 4, scale / 4, scale / 4, thrownBlockData);
				}
				BlockData data = thrownBlockData;
				ParticleWrapper bt = blockTrail;
				
				Location eyeLoc = ex.getEyeLocation();
				double pitchRads = Math.toRadians(eyeLoc.getPitch());
				double yawRads = Math.toRadians(eyeLoc.getYaw());
				
				Vector planeDir = new Vector(-Math.sin(yawRads), 0, Math.cos(yawRads));
				
				List<Vector> basis = VectorUtil.getBasis(eyeLoc, planeDir);
				Vector right = basis.getFirst();
				Vector up = basis.get(1);
				Vector forward = basis.getLast();
				
				Location throwOrigin = eyeLoc.clone()
						.add(right.clone().multiply(xOffset))
						.add(up.clone().multiply(yOffset))
						.add(forward.clone().multiply(zOffset))
						.setDirection(planeDir);
				
				ItemDisplay itemDisplay = executor.getThrownItemDisplay();
				itemDisplay.teleport(throwOrigin);
				
				Transformation tr = new Transformation(
						new Vector3f(0,0,0),
						new Quaternionf().rotateX((float) (Math.PI/2)).rotateY((float) (Math.PI/2)),
						new Vector3f(scale,scale,scale),
						new Quaternionf());
				itemDisplay.setTransformation(tr);
				
				Location[] prev = {eyeLoc};
				int[] step = {0};
				new BukkitRunnable() {
					@Override
					public void run() {
						double yVelocity = (force * Math.sin(-pitchRads) * step[0]);
						double yAcceleration = force/45 * step[0] * step[0];
						double y = yVelocity - yAcceleration;
						
						double forwardVelocity = force * Math.cos(pitchRads) * step[0];
						
						Vector displacement = planeDir.clone().multiply(forwardVelocity).add(up.clone().multiply(y));
						Location cur = throwOrigin.clone().add(displacement);
						
						Cache.throwTrailParticle.display(cur);
						if (step[0] % 3 == 0)
							if (bt != null)
								bt.display(cur);
						
						Vector velocity = cur.clone().subtract(prev[0]).toVector();
						
						Quaternionf lRotation = new Quaternionf();
						tr.getLeftRotation().slerp(tr.getLeftRotation().rotateZ((float) (Math.PI/4)).rotateX((float) (Math.PI/270)), 0.75f, lRotation);
						
						itemDisplay.setTransformation(new Transformation(
								new Vector3f(0, (float) y, (float) forwardVelocity),
								lRotation,
								tr.getScale(),
								tr.getRightRotation()
						));
						
						float v2 = (float) velocity.lengthSquared();
						
						boolean removeExecutor = step[0] <= 15;
						HashSet<LivingEntity> hit =
								prev[0].clone().subtract(cur).toVector().lengthSquared() > 0.001 ?
								HitboxUtil.secant(ex, prev[0], cur, scale/2.5f, removeExecutor) :
								HitboxUtil.sphere(ex, cur, scale/2.5f, removeExecutor);
						
						// check if the item was caught by the executor, and return it to their inventory if so
						if (hit.contains(ex)) {
							executor.giveItem(thrownItem);
							itemDisplay.remove();
							cancel();
							return;
						}
						
						// if entities other than the executor were caught in the path, hit them and create an explosion, and drop the item in the air
						if (!hit.isEmpty()) {
							for (LivingEntity target : hit) {
								if (target != ex) { // add other filters for armor stands, item displays, and undesirable targets
									Vector kb;
									if (!velocity.isZero() && v2 > 0.001f) {
										kb = velocity;
									} else {
										kb = target.getEyeLocation().subtract(ex.getEyeLocation().toVector()).toVector().normalize().multiply(force);
									}
									SwordEntityArbiter.getOrAdd(target.getUniqueId()).hit(executor, 2, 50, 12, kb);
								}
							}
							world.createExplosion(cur, 2, false, false);
							new BukkitRunnable() {
								@Override
								public void run() {
									thrownItem.damage(50, ex);
									world.dropItemNaturally(cur, thrownItem);
									itemDisplay.remove();
								}
							}.runTaskLater(Sword.getInstance(), 1L);
							
							if (data != null) {
								new ParticleWrapper(Particle.BLOCK, 30, 0.5,0.5,0.5, data).display(cur);
								new ParticleWrapper(Particle.DUST_PILLAR, 15, 0.5,0.5,0.5, data).display(cur);
							}
							
							cancel();
						}
						
						// ray trace in the direction of the velocity to check for blocks, if one exists, stop the movement
						if (!velocity.isZero() && v2 > 0.001f) {
							RayTraceResult block = world.rayTraceBlocks(cur, velocity, scale*force*0.5);
							if (block != null && block.getHitBlock() != null) {
								new ParticleWrapper(Particle.BLOCK, 60, 0.25,0.25,0.25, block.getHitBlock().getBlockData())
										.display(cur);
								new ParticleWrapper(Particle.DUST_PILLAR, 60, 0.5,0.5,0.5, block.getHitBlock().getBlockData())
										.display(cur);
								
								executor.message("  Lodged in the ground now");
								
								// create a marker to be used for retrieving the item with a 'grab' action
								ArmorStand marker = (ArmorStand) world.spawnEntity(cur, EntityType.ARMOR_STAND);
								marker.setInvulnerable(true);
								marker.setInvisible(true);
								marker.setCanMove(false);
								marker.setGravity(false);
								InteractiveItemArbiter.register(marker, itemDisplay);
								
								new BukkitRunnable() {
									@Override
									public void run() {
										if (!itemDisplay.isDead()) {
											itemDisplay.remove();
											InteractiveItemArbiter.remove(marker);
										}
									}
								}.runTaskLater(Sword.getInstance(), 1000L);
								if (data != null) {
									new ParticleWrapper(Particle.DUST_PILLAR, 60, 0.5,0.5,0.5, data).display(cur);
								}
								cancel();
							}
						}
						
						prev[0] = cur;
						step[0] += 1;
					}
				}.runTaskTimer(Sword.getInstance(), 0L, 1L);
			}
		});
	}
	
	public static void allowDrop(SwordPlayer executor) {
		cast(executor, 0L, new BukkitRunnable() {
			@Override
			public void run() {
				executor.setCanDrop(true);
				new BukkitRunnable() {
					@Override
					public void run() {
						executor.setCanDrop(false);
					}
				}.runTaskLater(Sword.getInstance(), 5L);
			}
		});
	}
	
	public static void death(Combatant executor) {
		cast(executor, 0L, new BukkitRunnable() {
			@Override
			public void run() {
				LivingEntity ex = executor.entity();
				Location l = executor.entity().getEyeLocation();
				RayTraceResult ray = ex.getWorld().rayTraceEntities(l, l.getDirection(), 6, entity -> entity.getUniqueId() != ex.getUniqueId());
				if (ray != null && ray.getHitEntity() != null) {
					Entity target = ray.getHitEntity();
					if (target instanceof LivingEntity le)
							SwordEntityArbiter.getOrAdd(le.getUniqueId()).hit(executor,
									1000, 20000,
									1, l.getDirection().multiply(100));
					else {
						target.getWorld().createExplosion(target.getLocation(), 5, true, true);
					}
				}
			}
		});
	}
}
