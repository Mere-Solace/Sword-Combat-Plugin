package btm.sword.system.action;

import btm.sword.Sword;
import btm.sword.system.entity.Combatant;
import btm.sword.system.entity.SwordEntity;
import btm.sword.system.entity.SwordPlayer;
import btm.sword.system.entity.aspect.AspectType;
import btm.sword.system.entity.SwordEntityArbiter;
import btm.sword.util.Cache;
import btm.sword.util.HitboxUtil;
import btm.sword.util.ParticleWrapper;
import btm.sword.util.VectorUtil;
import org.bukkit.*;
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

				LivingEntity target = HitboxUtil.firstInLineKnownLength(ex, o, o.getDirection(), range, grabThickness);
				if (target == null) {
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
		executor.setThrowCancelled(false);
		float scale = 1.1f;
		double rOffset = 0.75;
		double upOffset = -0.1;
		
		LivingEntity ex = executor.entity();
		World world = ex.getWorld();
		
		ItemStack thrownItem = executor.getItemStackInMainHand();
		
		Location eyeLoc = ex.getEyeLocation();
		double yawRads = Math.toRadians(eyeLoc.getYaw());
		
		Vector planeDir = new Vector(-Math.sin(yawRads), 0, Math.cos(yawRads));
		
		List<Vector> basis = VectorUtil.getBasis(eyeLoc, planeDir);
		Vector right = basis.getFirst();
		Vector up = basis.get(1);
		
		Location throwOrigin = eyeLoc.clone()
				.add(right.clone().multiply(rOffset))
				.add(up.clone().multiply(upOffset))
				.setDirection(planeDir);
		
		ItemDisplay itemDisplay = (ItemDisplay) world.spawnEntity(throwOrigin, EntityType.ITEM_DISPLAY);
		itemDisplay.setItemStack(thrownItem);
		
		executor.setThrownItemStack(thrownItem);
		executor.setThrownItemDisplay(itemDisplay);
		
		Transformation tr = new Transformation(
				new Vector3f(0,0,0),
				new Quaternionf().rotateX((float) (-Math.PI/2)).rotateY((float) (Math.PI/2)),
				new Vector3f(scale,scale,scale),
				new Quaternionf());
		itemDisplay.setTransformation(tr);
		
		int[] step = {0};
		new BukkitRunnable() {
			@Override
			public void run() {
				if (executor instanceof SwordPlayer sp && (!sp.isHoldingRight() || sp.isThrowCancelled())) {
					itemDisplay.remove();
					cancel();
				}
				ex.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 1, 2));
				
				if (step[0] % 3 == 0) {
					Location l = ex.getEyeLocation();
					List<Vector> basis = VectorUtil.getBasis(l, l.getDirection());
					Vector right = basis.getFirst();
					Vector up = basis.get(1);
					Location cur = ex.getEyeLocation()
							.add(right.clone().multiply(rOffset))
							.add(up.clone().multiply(upOffset));
					
					itemDisplay.teleport(cur);
					itemDisplay.setTransformation(new Transformation(
							new Vector3f(0, 0, 0),
							tr.getLeftRotation(),
							tr.getScale(),
							tr.getRightRotation()
					));
				}
				step[0]++;
			}
		}.runTaskTimer(Sword.getInstance(), 0L, 1L);
	}
	
	public static void throwCancel(Combatant executor) {
	
	}
	
	public static void throwItem(Combatant executor) {
		cast(executor, 10L, new BukkitRunnable() {
			@Override
			public void run() {
				double force = 1.5;
				float scale = 1.1f;
				
				LivingEntity ex = executor.entity();
				World world = ex.getWorld();
				
				ItemStack thrownItem;
				if (executor instanceof SwordPlayer sp) {
					Player p = ((Player) sp.entity());
					PlayerInventory inv = p.getInventory();
					thrownItem = inv.getItemInMainHand();
					inv.setItem(inv.getHeldItemSlot(), new ItemStack(Material.AIR));
				}
				else {
					thrownItem = Objects.requireNonNull(ex.getEquipment()).getItemInMainHand();
					Objects.requireNonNull(ex.getEquipment()).setItemInMainHand(new ItemStack(Material.AIR));
				}
				BlockData thrownBlockData = null;
				ParticleWrapper blockTrail = null;
				if (thrownItem.getType().isBlock()) {
					thrownBlockData = thrownItem.getType().createBlockData();
					blockTrail = new ParticleWrapper(Particle.BLOCK, 10, scale / 3, scale / 3, scale / 3, thrownBlockData);
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
				
				Location throwOrigin = eyeLoc.clone()
						.add(right.clone().multiply(0.45))
						.subtract(up.clone().multiply(0.3))
						.setDirection(planeDir);
				
				ItemDisplay itemDisplay = (ItemDisplay) world.spawnEntity(throwOrigin, EntityType.ITEM_DISPLAY);
				itemDisplay.setItemStack(thrownItem);
				
				Transformation tr = new Transformation(
						new Vector3f(0,0,1),
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
						double yAcceleration = force/50 * step[0] * step[0];
						double y = yVelocity - yAcceleration;
						
						double forwardVelocity = force * Math.cos(pitchRads) * step[0];
						
						Vector displacement = planeDir.clone().multiply(forwardVelocity).add(up.clone().multiply(y));
						Location cur = throwOrigin.clone().add(displacement);
						
						if (step[0] % 2 == 0) {
							Cache.throwTrailParticle.display(cur);
							if (bt != null)
								bt.display(cur);
						}
						
						Vector tangent = cur.clone().subtract(prev[0]).toVector();
						
						Quaternionf lRotation = new Quaternionf();
						tr.getLeftRotation().slerp(tr.getLeftRotation().rotateZ((float) (Math.PI/4)), 0.75f, lRotation);
						
						itemDisplay.setTransformation(new Transformation(
								new Vector3f(0, (float) y, (float) forwardVelocity),
								lRotation,
								tr.getScale(),
								tr.getRightRotation()
						));
						
						float v2 = (float) tangent.lengthSquared();
						
						HashSet<LivingEntity> hit = HitboxUtil.line(ex, prev[0], cur, scale/2, false);
						
						if (step[0] <= 20) {
							hit.remove(ex);
						}
						
						if (hit.contains(ex)) {
							executor.giveItem(thrownItem);
							itemDisplay.remove();
							cancel();
							return;
						}
						
						if (!hit.isEmpty()) {
							for (LivingEntity target : hit) {
								if (target != ex) {
									Vector kb;
									if (!tangent.isZero() && v2 > 0.001f) {
										kb = tangent;
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
									thrownItem.damage(15, ex);
									world.dropItemNaturally(cur, thrownItem);
									itemDisplay.remove();
								}
							}.runTaskLater(Sword.getInstance(), 1L);
							if (data != null) {
								new ParticleWrapper(Particle.DUST_PILLAR, 2, 0.1,0.1,0.1, data).display(cur);
							}
							
							cancel();
						}
						
						if (!tangent.isZero() && v2 > 0.001f) {
							RayTraceResult block = world.rayTraceBlocks(cur, tangent, scale*force*(v2/3));
							if (block != null && block.getHitBlock() != null) {
								new ParticleWrapper(Particle.BLOCK, 40, 0.2, 0.2, 0.2, block.getHitBlock().getBlockData())
										.display(cur);
								executor.message("  Lodged in the ground now");
								new BukkitRunnable() {
									@Override
									public void run() {
										itemDisplay.remove();
									}
								}.runTaskLater(Sword.getInstance(), 100L);
								if (data != null) {
									new ParticleWrapper(Particle.DUST_PILLAR, 2, 0.1,0.1,0.1, data).display(cur);
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
