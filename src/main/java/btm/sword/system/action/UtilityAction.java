package btm.sword.system.action;

import btm.sword.Sword;
import btm.sword.system.combat.GroundedAffliction;
import btm.sword.system.entity.Combatant;
import btm.sword.system.entity.SwordEntity;
import btm.sword.system.entity.SwordPlayer;
import btm.sword.system.entity.aspect.AspectType;
import btm.sword.system.entity.SwordEntityArbiter;
import btm.sword.system.item.prefab.Prefab;
import btm.sword.util.Cache;
import btm.sword.util.HitboxUtil;
import btm.sword.util.ParticleWrapper;
import btm.sword.util.VectorUtil;
import io.papermc.paper.math.Rotations;
import net.kyori.adventure.util.TriState;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.*;
import org.bukkit.inventory.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;
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
	
	public static void throwItem(Combatant executor) {
		cast(executor, 10L, new BukkitRunnable() {
			@Override
			public void run() {
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
				
				Location eyeLoc = ex.getEyeLocation();
				double pitchRads = Math.toRadians(eyeLoc.getPitch());
				double yawRads = Math.toRadians(eyeLoc.getYaw());
				
				Vector planeDir = new Vector(-Math.sin(yawRads), 0, Math.cos(yawRads));
				
				List<Vector> basis = VectorUtil.getBasis(eyeLoc, planeDir);
				Vector right = basis.getFirst();
				Vector up = basis.get(1);
				
				Location throwOrigin = eyeLoc.clone();
				
				throwOrigin.add(right.clone().multiply(0.45))
						.subtract(up.clone().multiply(0.25))
						.setDirection(planeDir);
				
				double force = 1.5;
				float scale = 1.1f;
				
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
						Cache.testObsidianTearParticle.display(cur);
						
						Vector tangent = cur.clone().subtract(prev[0].toVector()).toVector();
						Cache.testLavaDripParticle.display(cur.clone().add(tangent));
						
						Quaternionf lRotation = new Quaternionf();
						tr.getLeftRotation().slerp(tr.getLeftRotation().rotateZ((float) (Math.PI/6)), 0.75f, lRotation);
						
						itemDisplay.setTransformation(new Transformation(
								new Vector3f(0, (float) y, (float) forwardVelocity),
								lRotation,
								tr.getScale(),
								tr.getRightRotation()
						));
						
						float v2 = (float) tangent.lengthSquared();
						
						HashSet<LivingEntity> hit = HitboxUtil.line(ex, prev[0], cur, scale/2);
						
						if (!hit.isEmpty()) {
							for (LivingEntity target : hit) {
								Vector kb;
								if (!tangent.isZero() && v2 > 0.001f) {
									kb = tangent;
								}
								else {
									kb = target.getEyeLocation().subtract(ex.getEyeLocation().toVector()).toVector().normalize().multiply(force);
								}
								SwordEntityArbiter.getOrAdd(target.getUniqueId()).hit(executor, 2, 50, 12, kb);
							}
							world.createExplosion(cur, 2, false, false);
							new BukkitRunnable() {
								@Override
								public void run() {
									thrownItem.damage(15, ex);
									world.dropItemNaturally(cur, thrownItem);
									itemDisplay.remove();
								}
							}.runTaskLater(Sword.getInstance(), 2L);
							cancel();
						}
						
						if (!tangent.isZero() && v2 > 0.001f) {
							RayTraceResult block = world.rayTraceBlocks(cur, tangent, scale*force*(v2/5));
							if (block != null && block.getHitBlock() != null) {
								new ParticleWrapper(Particle.BLOCK, 40, 0.2, 0.2, 0.2, block.getHitBlock().getBlockData());
								executor.message("  Lodged in the ground now");
								new BukkitRunnable() {
									@Override
									public void run() {
										itemDisplay.remove();
									}
								}.runTaskLater(Sword.getInstance(), 100L);
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
