package btm.sword.system.action;

import btm.sword.Sword;
import btm.sword.system.SwordScheduler;
import btm.sword.system.action.type.AttackType;
import btm.sword.system.entity.Combatant;
import btm.sword.system.entity.SwordEntity;
import btm.sword.system.entity.SwordEntityArbiter;
import btm.sword.system.entity.SwordPlayer;
import btm.sword.system.entity.aspect.AspectType;
import btm.sword.util.*;
import btm.sword.util.sound.SoundType;
import org.apache.logging.log4j.util.BiConsumer;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class AttackAction extends SwordAction {
	private static final Map<String, BiConsumer<Combatant, AttackType>> attackMap = Map.of(
            "_SWORD", AttackAction::basicSlash,
            "_SHOVEL", AttackAction::basicSlash,
            "_AXE", AttackAction::basicSlash,
            "SHIELD", AttackAction::basicSlash
	);
	
	public static void basicAttack(Combatant executor, AttackType type) {
		Material item = executor.getItemTypeInHand(true);
		double dot = executor.entity().getEyeLocation().getDirection().dot(VectorUtil.UP);
		
		if (executor.isGrounded()) {
			for (var entry : attackMap.entrySet()) {
				if (item.name().endsWith(entry.getKey())) {
					entry.getValue().accept(executor, type);
					return;
				}
			}
		}
		else {
			((SwordPlayer) executor).resetTree(); // can't combo aerials
			
			AttackType attackType = AttackType.N_AIR;
			if (dot < -0.5) attackType = AttackType.DOWN_AIR;
			
			for (var entry : attackMap.entrySet()) {
				if (item.name().endsWith(entry.getKey())) {
					entry.getValue().accept(executor, attackType);
					return;
				}
			}
		}
	}
	
	// * = must be a method/function
	
	// Each attack needs:
	//
	//  ~ max cast duration (calculated somehow, usually by a stat) max cast duration also becomes duration for particle display
	//
	//  ~ at least one damage value, potentially more for 'sweet spots' in attacks
	//      ~ could just add other required damage values (passed as functions operated on a combatant)
	//
	//  ~ range multiplier (calculated by a stat)
	//
	//  ~ vectors that define the shape of the attack
	//      ~ for bezier, that means:
	//          ~ 4 (typically, could be 3+, higher cost higher up though) control vectors
	//          ~ 3 basis vectors, either oriented with pitch or not
	//
	//  | different attacks and abilities will have shapes that may not be in a nice list of ordered vectors, however,
	//  | so shapes will have to be translated around and displayed at different parts of the animations.
	//
	//  ~ period for how fast the attack will be executed
	//      ~ can be dynamic (through certain intervals, the speed of execution changes (a wind up for example)
	//
	//      Possible solution: to the dynamic specifications - attackNodes
	//
	//  ~ # of steps per iteration, heavily dependent on the choice of shapes
	//
	//  ~ Self inflicted velocity (can be different directions at different parts of the execution)
	//
	//  ~ offset vectors and aesthetic particle positions
	//
	//  ~ Velocity vectors applied to targets hit
	//
	//  ~ parameters for the hit function
	//
	//  ~ boolean: shouldStop on first # of hits
	//
	//  ~ other hit effects besides the entity.hit() function
	//
	//  ~ hit particles and visuals
	//
	//  ~ hit audio
	//
	//  ~ cast audio
	//
	//  ~ interpolation function for filling in the shape of the attack (optional)
	//
	//  ~ function for checking if the attack went into the ground or not (optional)
	//
	//  ~ boolean for destroying blocks or not
	//
	
	public static void basicSlash(Combatant executor, AttackType type) {
		long castDuration = (long) executor.calcValueReductive(AspectType.FINESSE, 1L, 3L, 0.2);
		if (executor instanceof SwordPlayer sp) sp.player().setCooldown(sp.getItemTypeInHand(true), (int) castDuration);
		cast(executor, castDuration,
			new BukkitRunnable() {
				@Override
				public void run() {
                    SoundUtil.playSound(executor.entity(), SoundType.ENTITY_ENDER_DRAGON_FLAP, 0.35f, 0.6f);

					executor.setTimeOfLastAttack(System.currentTimeMillis());
					executor.setDurationOfLastAttack((int) castDuration * 500);
					
					LivingEntity ex = executor.entity();
					double damage = getBasicNonRPGDamage(executor.getItemTypeInHand(true));

                    int numSteps = 50;

					double rangeMultiplier;
					List<Vector> controlVectors;
					List<Vector> bezierVectors;
					boolean withPitch = true;
					boolean aerial = false;
					switch (type) {
                        case BASIC_2 -> {
							rangeMultiplier = 1.4;
							controlVectors = new ArrayList<>(Cache.basicSword2);
						}
						case BASIC_3 -> {
							rangeMultiplier = 1.4;
							controlVectors = new ArrayList<>(Cache.basicSword3);
						}
						case N_AIR -> {
							rangeMultiplier = 1.3;
							controlVectors = new ArrayList<>(Cache.aerialNeutralSword);
							aerial = true;
						}
						case DOWN_AIR -> {
							rangeMultiplier = 1.2;
							controlVectors = new ArrayList<>(Cache.aerialSwordDown);
							withPitch = false;
							aerial = true;
						}
                        default -> {
                            rangeMultiplier = 1.4;
                            controlVectors = new ArrayList<>(Cache.basicSword1);
                        }
                    }
					
					Location o = ex.getEyeLocation();
					
					ArrayList<Vector> basis = withPitch ? VectorUtil.getBasis(o, o.getDirection()) : VectorUtil.getBasisWithoutPitch(o);
					Vector right = basis.getFirst();
					
					List<Vector> transformedControlVectors = controlVectors.stream()
							.map(v -> VectorUtil.transformWithNewBasis(basis, v).multiply(rangeMultiplier))
							.toList();

					bezierVectors = BezierUtil.cubicBezier3D(transformedControlVectors.getFirst(),transformedControlVectors.get(1), transformedControlVectors.get(2), transformedControlVectors.getLast(),
							numSteps);
					
					int duration = (int) castDuration;
					int period = 1;
					int[] step = {0};
					int size = bezierVectors.size();
					int perIteration = bezierVectors.size()/duration;
					
					HashSet<LivingEntity> hit = new HashSet<>();
					
					if (aerial) o.add(VectorUtil.UP.clone().multiply(ex.getVelocity().getY()));
					
					if (!aerial) {
						Vector curV = ex.getVelocity();
						ex.setVelocity(new Vector(
								curV.getX() * 0.3,
								curV.getY() * 0.4,
								curV.getZ() * 0.3));
					}
					
					double[] d = {damage};
					new BukkitRunnable() {
                        final int slice = numSteps/4;
						@Override
						public void run() {
							for (int i = 0; i < perIteration; i++) {
								int s = step[0];
								if (s >= size) {
									cancel();
									break;
								}
								
								// particle placement
								Vector v = bezierVectors.get(s);
								Vector n = v.clone().normalize();
								Location l = o.clone().add(v);

								Cache.testSwingParticle.display(l);
								
								if (s > size * (0.1)) {
									Location p = l.clone().subtract(v.clone().multiply(0.05 * ((double) s / (size))).add(n.multiply(0.2)));
									Cache.testSwingParticle.display(p);
								}
								if (s > size * (0.3)) {
									Location p = l.clone().subtract(v.clone().multiply(0.25 * ((double) s / (size))).add(n.multiply(2)));
									Cache.testSwingParticle.display(p);
								}
								if (s > size * (0.5)) {
									Location p = l.clone().subtract(v.clone().multiply(0.1 * ((double) s / (size))).add(n.multiply(0.5)));
									Cache.testSwingParticle.display(p);
									Location p2 = l.clone().subtract(v.clone().multiply(0.25 * ((double) s / (size))).add(n.multiply(1.75)));
									Cache.testSwingParticle.display(p2);
								}
								if (s > size * (0.625)) {
									Location p = l.clone().subtract(v.clone().multiply(0.05 * ((double) s / (size))).add(n.multiply(2.5)));
									Cache.testSwingParticle.display(p);
									Location p2 = l.clone().subtract(v.clone().multiply(0.05 * ((double) s / (size))).add(n.multiply(1.5)));
									Cache.testSwingParticle.display(p2);
								}
								if (s > size * (0.75)) {
									Location p = l.clone().subtract(v.clone().multiply(0.15 * ((double) s / (size))).add(n.multiply(0.6)));
									Cache.testSwingParticle.display(p);
									Location p2 = l.clone().subtract(v.clone().multiply(0.2 * ((double) s / (size))).add(n.multiply(0.55)));
									Cache.testSwingParticle.display(p2);
								}
								
								// retrieving targets and setting knockback
								Vector kb =  new Vector(0,0.25,0);
								Vector r = right.clone().multiply(0.1);
								
								// enum map to hitbox Consumer function accepting executor
								HashSet<LivingEntity> curHit = HitboxUtil.secant(ex, o, l, 0.4, true);
								
								for (LivingEntity target : curHit)
									if (!hit.contains(target)) {
										switch (type) {
											case BASIC_1 -> kb = kb.clone().add(r);
											case BASIC_2 -> kb = kb.clone().add(r.clone().multiply(-1));
											case BASIC_3 -> kb = target.getLocation().toVector()
													.subtract(o.toVector()).normalize()
													.subtract(new Vector(0,0.5,0));
											default -> kb = v.clone().normalize().multiply(0.7);
										}
										
										SwordEntity sTarget = SwordEntityArbiter.getOrAdd(target.getUniqueId());
										if (sTarget == null)
											continue;
										
										if (!sTarget.entity().isDead()) {
											// hit function call
											sTarget.hit(executor, 5, 1, (float) d[0], 6, kb);
											// hit particles
											Cache.testHitParticle.display(sTarget.getChestLocation());
										}
										else {
											executor.message("Target: " + target + " caused an NPE");
										}
									}
								hit.addAll(curHit);
								
								// check if attack entered the ground
								// enter ground and interpolation function
								if (s < size-1) {
									Vector direction = bezierVectors.get(s + 1).clone().subtract(v);
									RayTraceResult result = ex.getWorld().rayTraceBlocks(l, direction, 0.3);
									if (result != null) {
										// enter ground particles
										new ParticleWrapper(Particle.BLOCK, 10, 0.5, 0.5, 0.5, Objects.requireNonNull(result.getHitBlock()).getBlockData()).display(l);
										Cache.basicSwordEnterGround.display(l);
										// potential reduction of damage formula
										d[0] = Math.max(d[0] *(0.2), d[0]-1);
									}
									else if (direction.lengthSquared() > (double) 2 / (size*size)) {
										// interpolated particle, same as normal particle
										Cache.testSwingParticle.display(l.add(direction.multiply(0.5)));
									}
								}
								
								step[0]++;
							}
						}
					}.runTaskTimer(Sword.getInstance(), 1, period); // changed delay to 1 from 0
				}
			}
		);
	}
	
	public static double getBasicNonRPGDamage(Material item) {
		double damage;
		switch (item) {
			case NETHERITE_SWORD -> damage = 15;
			case DIAMOND_SWORD -> damage = 11;
			case IRON_SWORD -> damage = 8;
			case GOLDEN_SWORD -> damage = 6;
			default -> damage = 4;
		}
		return damage;
	}
	
	public static void cast(Combatant executor) {
		// step 0: calculate all preliminary stats and values, such as cast time, damage values, etc.
		
		// step 1: determine shape to be displayed
		//      step 1.1: get basis
		//      step 1.2: transform point vectors with basis
		
		// step 2: calculate runnable values from the shape generation (duration, period, # points per iteration)
		//      note: take what kind of points to be displayed into account:
		//          - image/3D shape of particles
		//          - armor stand projectile
		//          - function (bezier, circle, line, etc.)
		//      step 2.1: generate other objects or vectors with a function
		//      step 2.2: differentiate hitbox vectors from aesthetic vectors, or simply leave the new calculated
		//                vectors out of the hitbox calculation
		//      step 2.21: during these calculations, partition the locations into parts matching the number of iterations
		
		// | start the runnable |
		
		// step 3: give velocity and statuses to the executor
		
		// step 4: calculate hit targets and spawn particles
		//      step 4.1: run hit function and hit particles/sounds
		//      step 4.2: run interpolation function on current vectors and spawn extra aesthetic particles or do
		//                ground/entity checks if necessary
		
		// step 5: once done, run the next function or stop
	}

//    public static void basicSlash_V2(Combatant executor, AttackType type) {
//        long castDuration = (long) executor.calcValueReductive(AspectType.FINESSE, 1L, 3L, 0.2);
//        if (executor instanceof SwordPlayer sp) sp.player().setCooldown(sp.getItemTypeInHand(true), (int) castDuration);
//        cast(executor, castDuration,
//                new BukkitRunnable() {
//                    @Override
//                    public void run() {
//                        SoundUtil.playSound(executor.entity(), SoundType.ENTITY_ENDER_DRAGON_FLAP, 0.35f, 0.6f);
//
//                        executor.setTimeOfLastAttack(System.currentTimeMillis());
//                        executor.setDurationOfLastAttack((int) castDuration * 500);
//
//                        LivingEntity ex = executor.entity();
//                        double damage = getBasicNonRPGDamage(executor.getItemTypeInHand(true));
//
//                        double rangeMultiplier;
//                        List<Vector> controlVectors;
//                        boolean withPitch = true;
//                        boolean aerial = false;
//                        switch (type) {
//                            case BASIC_2 -> {
//                                rangeMultiplier = 1.4;
//                                controlVectors = new ArrayList<>(Cache.basicSword2);
//                            }
//                            case BASIC_3 -> {
//                                rangeMultiplier = 1.4;
//                                controlVectors = new ArrayList<>(Cache.basicSword3);
//                            }
//                            case N_AIR -> {
//                                rangeMultiplier = 1.4;
//                                controlVectors = new ArrayList<>(Cache.aerialNeutralSword);
//                                aerial = true;
//                            }
//                            case DOWN_AIR -> {
//                                rangeMultiplier = 1.2;
//                                controlVectors = new ArrayList<>(Cache.aerialSwordDown);
//                                withPitch = false;
//                                aerial = true;
//                            }
//                            default -> {
//                                rangeMultiplier = 1.4;
//                                controlVectors = new ArrayList<>(Cache.basicSword1);
//                            }
//                        }
//
//                        Location o = ex.getEyeLocation();
//                        Vector offset = new Vector(0, 1.1, 0);
//
//                        ArrayList<Vector> basis = withPitch ? VectorUtil.getBasis(o, o.getDirection()) : VectorUtil.getBasisWithoutPitch(o);
//                        Vector right = basis.getFirst();
//
//                        List<Vector> trCtrlVecs = BezierUtil.adjustCtrlToBasis(basis, controlVectors, rangeMultiplier);
//
//                        Function<Double, Vector> bezier = BezierUtil.cubicBezier3d(trCtrlVecs.getFirst(), trCtrlVecs.get(1), trCtrlVecs.get(2), trCtrlVecs.getLast());
//                        double start_t = -0.5;
//                        double end_t = 1.0;
//                        Vector start = bezier.apply(start_t);
//
//
//                        int stepsPerTick = 7;
//                        int numSteps = 3;
//                        double step = (end_t-start_t)/(numSteps*stepsPerTick);
//                        int period = 1;
//
//                        HashSet<LivingEntity> hit = new HashSet<>();
//
//                        if (aerial) o.add(VectorUtil.UP.clone().multiply(ex.getVelocity().getY()));
//
//                        if (!aerial) {
//                            Vector curV = ex.getVelocity();
//                            ex.setVelocity(new Vector(
//                                    curV.getX() * 0.3,
//                                    curV.getY() * 0.4,
//                                    curV.getZ() * 0.3));
//                        }
//
//                        // new item display stuff:
//                        ItemDisplay weapon = (ItemDisplay) ex.getWorld().spawnEntity(ex.getLocation().add(start).add(offset), EntityType.ITEM_DISPLAY);
//                        weapon.setItemStack(ItemStack.of(executor.getItemTypeInHand(true)));
//                        weapon.setGlowing(true);
//                        weapon.setGlowColorOverride(Color.WHITE);
//                        weapon.setTransformation(new Transformation(
//                                new Vector3f(),
//                                new Quaternionf().rotationX((float) Math.PI/2),
//                                new Vector3f(1,1,1),
//                                new Quaternionf()
//                        ));
//                        //
//
//                        new BukkitRunnable() {
//                            final double d = damage;
//                            int i = 0;
//                            Vector v;
//                            Location l;
//                            Vector prev = start.clone();
//                            @Override
//                            public void run() {
//                                if (i > (numSteps*stepsPerTick)) {
//                                    new BukkitRunnable() {
//                                        int x = 0;
//                                        @Override
//                                        public void run() {
//                                            if (x > 3) {
//                                                if (!weapon.isDead())
//                                                    weapon.remove();
//                                                cancel();
//                                            }
//                                            Cache.thrownItemMarkerParticle2.display(weapon.getLocation());
//                                            DisplayUtil.smoothTeleport(weapon);
//                                            weapon.teleport(ex.getLocation().add(offset).add(v.clone().multiply(0.75)).setDirection(v));
//                                            x++;
//                                        }
//                                    }.runTaskTimer(Sword.getInstance(),0,1);
//                                    cancel();
//                                }
//                                // for millisecond granularity
//                                int timeIncrement = 1000/(20 * stepsPerTick);
//                                for (int x = 0; x < stepsPerTick; x++) {
//                                    if (i > numSteps) break;
//                                    final int I = i;
//                                    SwordScheduler.runLater(() -> {
//                                        v = bezier.apply(step * I);
//
//                                        Vector to = v.clone().subtract(prev);
//
//                                        l = ex.getLocation().add(offset).add(v.clone().multiply(0.75));
//
////                                        DisplayUtil.secant(List.of(Cache.testSwingParticle), ex.getLocation().add(offset), l, 0.5);
//
//                                        DisplayUtil.smoothTeleport(weapon, 1);
//                                        weapon.teleport(l.setDirection(v));
//                                        Cache.testSwingParticle.display(l);
//
//                                        // retrieving targets and setting knockback
//                                        Vector kb =  new Vector(0,0.25,0);
//                                        Vector r = right.clone().multiply(0.1);
//
//                                        // enum map to hitbox Consumer function accepting executor
//                                        HashSet<LivingEntity> curHit = HitboxUtil.secant(ex, o, l, 0.4, true);
//
//                                        for (LivingEntity target : curHit)
//                                            if (!hit.contains(target)) {
//                                                switch (type) {
//                                                    case BASIC_1 -> kb = kb.clone().add(r);
//                                                    case BASIC_2 -> kb = kb.clone().add(r.clone().multiply(-1));
//                                                    case BASIC_3 -> kb = target.getLocation().toVector()
//                                                            .subtract(o.toVector()).normalize()
//                                                            .subtract(new Vector(0,0.5,0));
//                                                    default -> kb = v.clone().normalize().multiply(0.7);
//                                                }
//
//                                                SwordEntity sTarget = SwordEntityArbiter.getOrAdd(target.getUniqueId());
//                                                if (sTarget == null)
//                                                    continue;
//
//                                                if (!sTarget.entity().isDead()) {
//                                                    // hit function call
//                                                    sTarget.hit(executor, 5, 1, (float) d, 6, kb);
//                                                    // hit particles
//                                                    Cache.testHitParticle.display(sTarget.getChestLocation());
//                                                }
//                                            }
//                                        hit.addAll(curHit);
//
//                                        prev = v;
//
//                                    }, x * timeIncrement, TimeUnit.MILLISECONDS);
//
//                                    i++;
//                                }
//                            }
//                        }.runTaskTimer(Sword.getInstance(), 0, 1);
//                    }
//                }
//        );
//    }
}
