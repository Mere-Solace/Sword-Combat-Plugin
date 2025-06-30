package btm.sword.system.action.utility;

import btm.sword.Sword;
import btm.sword.system.action.SwordAction;
import btm.sword.system.entity.Combatant;
import btm.sword.system.entity.SwordEntity;
import btm.sword.system.entity.SwordPlayer;
import btm.sword.system.entity.SwordEntityArbiter;
import btm.sword.system.entity.display.InteractiveItemArbiter;
import btm.sword.util.*;
import org.bukkit.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.*;
import org.bukkit.inventory.*;
import org.bukkit.persistence.PersistentDataType;
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
import java.util.function.Predicate;

public class ThrowAction extends SwordAction {
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
		
		Quaternionf lRotation;
		String itemType = thrownItem.getItemMeta().getPersistentDataContainer()
				.get(new NamespacedKey(Sword.getInstance(), "weapon"), PersistentDataType.STRING);
		if (Objects.equals(itemType, "long_sword")) {
			lRotation = new Quaternionf().rotateX((float) (Math.PI/2)).rotateY((float) (Math.PI/2));
		}
		else {
			lRotation = new Quaternionf().rotateX((float) (-Math.PI/1.75)).rotateY((float) (Math.PI/3)); // changed from 3 and 1.5
		}
		Transformation tr = new Transformation(
				new Vector3f(xOffset,yOffset,zOffset),
				lRotation,
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
	// TODO DONT ALLOW INPUT WHILE HOLDING THROW!
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
				
				//#region entity and item initialization
				LivingEntity ex = executor.entity();
				World world = ex.getWorld();
				
				ItemStack thrownItem = executor.getThrownItemStack();
				
				boolean[] spearLike = {false};
				String itemType = thrownItem.getItemMeta().getPersistentDataContainer()
						.get(new NamespacedKey(Sword.getInstance(), "weapon"), PersistentDataType.STRING);
				if (Objects.equals(itemType, "long_sword")) spearLike[0] = true;
				
				BlockData thrownBlockData = null;
				ParticleWrapper blockTrail = null;
				if (thrownItem.getType().isBlock()) {
					thrownBlockData = thrownItem.getType().createBlockData();
					blockTrail = new ParticleWrapper(Particle.BLOCK, 5, scale / 4, scale / 4, scale / 4, thrownBlockData);
				}
				BlockData data = thrownBlockData;
				ParticleWrapper bt = blockTrail;
				//#endregion
				
				//#region location, direction, and transform
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
				//#endregion
				
				Predicate<Entity> hitMask = entity ->
						!entity.isDead()
						&& entity.getUniqueId() != executor.uuid()
						&& !entity.getType().equals(EntityType.ITEM_DISPLAY)
						&& !entity.getType().equals(EntityType.ARMOR_STAND);
				
				Location[] prev = {eyeLoc};
				int[] step = {0};
				double yVelocityCoeff = force * Math.sin(-pitchRads);
				double yAccelerationCoeff = force/45;
				double forwardCoeff = force * Math.cos(pitchRads);
				// flight runnable
				new BukkitRunnable() {
					@Override
					public void run() {
						double yVelocity = yVelocityCoeff * step[0];
						double yAcceleration = yAccelerationCoeff * step[0] * step[0];
						double y = yVelocity - yAcceleration;
						
						double forwardVelocity = forwardCoeff * step[0];
						
						// calc displacement and then current pos
						Vector displacement = planeDir.clone().multiply(forwardVelocity).add(up.clone().multiply(y));
						Location cur = throwOrigin.clone().add(displacement);
						// calc velocity vector from cur and prev and then check if it's a valid vector
						Vector velocity = cur.clone().subtract(prev[0]).toVector();
						float v2 = (float) velocity.lengthSquared();
						boolean validVelocity = !velocity.isZero() && v2 > 0.001;
						Vector vNorm = validVelocity ? velocity.normalize() : velocity;
						
						Quaternionf lRotation = new Quaternionf();
						
						if (spearLike[0]) {
							if (validVelocity) {
								lRotation = new Quaternionf()
										.rotateY((float) Math.PI / 2f)
										.rotateZ((float) Math.acos(vNorm.dot(new Vector(0, 1, 0))));
							}
							else {
								lRotation = itemDisplay.getTransformation().getLeftRotation();
							}
						}
						else {
							tr.getLeftRotation().slerp(tr.getLeftRotation().rotateZ((float) (Math.PI / 4)), 0.75f, lRotation);
						}
						itemDisplay.setTransformation(new Transformation(
								new Vector3f(0, (float) y, (float) forwardVelocity),
								lRotation,
								tr.getScale(),
								tr.getRightRotation()
						));
						
						boolean removeExecutor = step[0] <= 15;
						
						HashSet<LivingEntity> hit = validVelocity ?
								HitboxUtil.secant(ex, prev[0], cur, scale/2.5f, removeExecutor) :
								HitboxUtil.sphere(ex, cur, scale/2.5f, removeExecutor);
						
						// check if the item was caught by the executor, and return it to their inventory if so
						if (hit.contains(ex)) {
							executor.giveItem(thrownItem);
							itemDisplay.remove();
							cancel();
							return;
						}
						
						//#region if an impaling-type weapon, calc ray trace
						if (spearLike[0]) {
							RayTraceResult result = validVelocity ?
									world.rayTraceEntities(cur, vNorm, scale*force*0.5, hitMask) :
									world.rayTraceEntities(cur, forward.clone().subtract(up), scale*force*0.5, hitMask);
							
							LivingEntity impaled = null;

							if (result != null && result.getHitEntity() != null) {
								impaled = (LivingEntity) result.getHitEntity();
							}
							else if (!hit.isEmpty()) {
								impaled = hit.stream().toList().getFirst();
							}
							
							if (impaled != null) {
								SwordEntity swordImpaled = SwordEntityArbiter.getOrAdd(impaled.getUniqueId());
								
								if (swordImpaled != null) {
									swordImpaled.addImpalement();
									executor.message("This lil guy is impaled by: " + swordImpaled.getNumberOfImpalements());
									Vector offset = spearLike[0] ?
											cur.clone().subtract(vNorm.multiply(scale/2)).subtract(impaled.getLocation()).toVector() :
											cur.clone().subtract(impaled.getLocation()).toVector();
									
									double entityYawRads = Math.toRadians(impaled.getBodyYaw());
									Vector entityDir = new Vector(-Math.sin(entityYawRads), 0, Math.cos(entityYawRads));
									
									List<Vector> eBasis = VectorUtil.getBasis(impaled.getLocation(), entityDir);
									Vector eRight = eBasis.getFirst();
									Vector eForward = eBasis.getLast();
									
									Vector projIntoRightPlane = VectorUtil.getProjOntoPlan(velocity, VectorUtil.UP).normalize();
									boolean check = projIntoRightPlane.dot(eRight) >= 0;
									double relativeYawOffset = Math.acos(eForward.dot(projIntoRightPlane));
									
//									impaled.setAI(false);
									Transformation orientation = new Transformation(
											new Vector3f ((float) offset.getX() * 0.15f, (float) offset.getY(), (float) offset.getZ() * 0.15f),
											itemDisplay.getTransformation().getLeftRotation(),
											new Vector3f(scale),
											new Quaternionf()
									);
									// TODO fix angle of sword sticking out of impaled targets
									EntityUtil.itemDisplayFollow(swordImpaled, itemDisplay, relativeYawOffset, check, orientation);
									swordImpaled.hit(executor, 1, 2, 75,60,velocity.clone().normalize().multiply(3));
									
									ArmorStand marker = (ArmorStand) world.spawnEntity(cur, EntityType.ARMOR_STAND);
									InteractiveItemArbiter.register(marker, itemDisplay);
									Location testLoc = impaled.getLocation().add(VectorUtil.UP.clone().multiply(offset.getY()));
									new BukkitRunnable() {
										@Override
										public void run() {
											if (marker.isDead() || itemDisplay.isDead()) {
												swordImpaled.removeImpalement();
												executor.message("This lil guy is impaled by: " + swordImpaled.getNumberOfImpalements());
												executor.message("Item was retrieved");
												itemDisplay.remove();
												marker.remove();
												cancel();
											}
											
											DisplayUtil.line(List.of(Cache.basicSwordBlueTransitionParticle), testLoc, projIntoRightPlane, 2, 0.2);
											DisplayUtil.line(List.of(Cache.testSwingParticle), testLoc, entityDir, 2, 0.2);
											
											Location itemPos = itemDisplay.getLocation().add(new Vector(0, offset.getY(), 0));
											Cache.basicSwordWhiteTransitionParticle.display(itemPos);
											
											marker.teleport(itemPos);
											
											if (swordImpaled.isDead()) {
												swordImpaled.entity().getWorld().dropItemNaturally(itemPos, itemDisplay.getItemStack());
												itemDisplay.remove();
												marker.remove();
												cancel();
											}
										}
									}.runTaskTimer(Sword.getInstance(), 0L, 1L);
									cancel();
									return;
								}
							}
						}
						//#endregion
						
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
									SwordEntityArbiter.getOrAdd(target.getUniqueId()).hit(executor, 3, 2, 50, 12, kb);
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
						if (validVelocity) {
							RayTraceResult block = world.rayTraceBlocks(cur, velocity, scale*force*0.5);
							if (block != null && block.getHitBlock() != null) {
								new ParticleWrapper(Particle.BLOCK, 60, 0.25,0.25,0.25, block.getHitBlock().getBlockData())
										.display(cur);
								new ParticleWrapper(Particle.DUST_PILLAR, 60, 0.5,0.5,0.5, block.getHitBlock().getBlockData())
										.display(cur);
								
								executor.message("  Lodged in the ground now");

								// create a marker to be used for retrieving the item with a 'grab' action
								ArmorStand marker = (ArmorStand) world.spawnEntity(cur, EntityType.ARMOR_STAND);
								InteractiveItemArbiter.register(marker, itemDisplay);
								
								int x = 1;
								while (!marker.getLocation().getBlock().getType().isAir()) {
									marker.teleport(cur.clone().add(velocity.normalize().multiply(-0.25*x)));
									x++;
									if (x > 30)
										executor.message("Some wacky stuff just happened");
								}
								executor.message(marker.getLocation().toString());
								itemDisplay.teleport(marker.getLocation().setDirection(forward));
								Transformation curTr = itemDisplay.getTransformation();
								Transformation newTr = new Transformation(
										new Vector3f(),
										curTr.getLeftRotation(),
										curTr.getScale(),
										new Quaternionf()
								);
								itemDisplay.setTransformation(newTr);
								
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
						
						Cache.throwTrailParticle.display(cur);
						if (step[0] % 3 == 0)
							if (bt != null)
								bt.display(cur);
						
						prev[0] = cur;
						step[0] += 1;
					}
				}.runTaskTimer(Sword.getInstance(), 0L, 1L);
			}
		});
	}
}
