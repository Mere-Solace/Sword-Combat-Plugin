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
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MainHand;
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
				
				if (executor instanceof SwordPlayer sp) {
					Player p = ((Player) sp.entity());
					p.getInventory().setItem(p.getInventory().getHeldItemSlot(), new ItemStack(Material.AIR));
				}
				
				ItemDisplay display = (ItemDisplay) world.spawnEntity(executor.getChestLocation(), EntityType.ITEM_DISPLAY);
				display.setItemStack(new ItemStack(Material.NETHERITE_SWORD));
				Transformation t = new Transformation(
						new Vector3f(0,0,3),
						new Quaternionf(0,0,0,1),
						new Vector3f(1.1f,1.1f,1.1f),
						new Quaternionf(0,0,0,1));
				display.setTransformation(t);
				
				ItemDisplay display1 = (ItemDisplay) world.spawnEntity(executor.getChestLocation(), EntityType.ITEM_DISPLAY);
				display1.setItemStack(new ItemStack(Material.NETHERITE_SWORD));
				Transformation t1 = new Transformation(
						new Vector3f(2,0,1),
						new Quaternionf(0,0,1,0),
						new Vector3f(1.1f,1.1f,1.1f),
						new Quaternionf(0,0,0,1));
				display1.setTransformation(t1);
				
				ItemDisplay display2 = (ItemDisplay) world.spawnEntity(executor.getChestLocation(), EntityType.ITEM_DISPLAY);
				display2.setItemStack(new ItemStack(Material.NETHERITE_SWORD));
				Transformation t2 = new Transformation(
						new Vector3f(-2,0,1),
						new Quaternionf(0,1,0,0),
						new Vector3f(1.1f,1.1f,1.1f),
						new Quaternionf(0,0,0,1));
				display2.setTransformation(t2);
				
				ItemDisplay display3 = (ItemDisplay) world.spawnEntity(executor.getChestLocation(), EntityType.ITEM_DISPLAY);
				display3.setItemStack(new ItemStack(Material.NETHERITE_SWORD));
				Transformation t3 = new Transformation(
						new Vector3f(0,-1,0.5f),
						new Quaternionf(1,0,0,0),
						new Vector3f(1.1f,1.1f,1.1f),
						new Quaternionf(0,0,0,1));
				display3.setTransformation(t3);
				
				ItemDisplay display4 = (ItemDisplay) world.spawnEntity(executor.getChestLocation(), EntityType.ITEM_DISPLAY);
				display4.setItemStack(new ItemStack(Material.NETHERITE_SWORD));
				Transformation t4 = new Transformation(
						new Vector3f(0,0,1),
						new Quaternionf(0.7,0,0,0.7),
						new Vector3f(1.1f,1.1f,1.1f),
						new Quaternionf(0,0,0,1));
				display4.setTransformation(t4);
				
				int[] step = {0};
				new BukkitRunnable() {
					@Override
					public void run() {
//
//						display.setTransformation(new Transformation(
//								new Vector3f(0,0,1*step[0]*0.05f),
//								t.getLeftRotation(),
//								t.getScale(),
//								t.getRightRotation()
//						));
//
//						display1.setTransformation(new Transformation(
//								new Vector3f(0,0,1*step[0]*0.05f),
//								t1.getLeftRotation(),
//								t1.getScale(),
//								t1.getRightRotation()
//						));
//
//						display2.setTransformation(new Transformation(
//								new Vector3f(0,0,1*step[0]*0.05f),
//								t2.getLeftRotation(),
//								t2.getScale(),
//								t2.getRightRotation()
//						));
//
//						display3.setTransformation(new Transformation(
//								new Vector3f(0,0,1*step[0]*0.05f),
//								t3.getLeftRotation(),
//								t3.getScale(),
//								t3.getRightRotation()
//						));
//
//						display4.setTransformation(new Transformation(
//								new Vector3f(0,0,1*step[0]*0.05f),
//								t4.getLeftRotation(),
//								t4.getScale(),
//								t4.getRightRotation()
//						));
						
						if (step[0] > 500) {
							display.remove();
							cancel();
						}
						
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
