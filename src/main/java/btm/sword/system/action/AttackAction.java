package btm.sword.system.action;

import btm.sword.Sword;
import btm.sword.system.action.type.AttackType;
import btm.sword.system.combat.Affliction;
import btm.sword.system.entity.SwordEntityArbiter;
import btm.sword.system.entity.aspect.AspectType;
import btm.sword.system.entity.base.SwordEntity;
import btm.sword.system.entity.types.Combatant;
import btm.sword.system.entity.types.SwordPlayer;
import btm.sword.util.display.ParticleWrapper;
import btm.sword.util.display.Prefab;
import btm.sword.util.entity.HitboxUtil;
import btm.sword.util.math.BezierUtil;
import btm.sword.util.math.VectorUtil;
import btm.sword.util.sound.SoundType;
import btm.sword.util.sound.SoundUtil;
import java.util.*;
import org.apache.logging.log4j.util.BiConsumer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

/**
 * Provides attack-related actions for {@link Combatant} entities.
 * <p>
 * Supports basic melee attacks, including grounded and aerial variations.
 * Handles attack execution, hit detection, damage application, particle effects,
 * knockback, and associated cooldowns.
 */
public class AttackAction extends SwordAction {
    /** Mapping from item suffixes to corresponding attack handlers. */
    private static final Map<String, BiConsumer<Combatant, AttackType>> attackMap = Map.of(
            "_SWORD", AttackAction::basicSlash,
            "_SHOVEL", AttackAction::basicSlash,
            "_AXE", AttackAction::basicSlash,
            "SHIELD", AttackAction::basicSlash
    );

    /**
     * Executes a basic attack for the given {@link Combatant} and {@link AttackType}.
     * <p>
     * Selects the correct attack variant based on the item in hand and whether the
     * executor is grounded or airborne. Aerial attacks reset the executor's combo tree.
     *
     * @param executor The combatant performing the attack.
     * @param type The type of attack being performed.
     */
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

    /**
     * Executes a basic slash attack.
     * <p>
     * Generates a cubic Bezier path for the attack based on attack type, applies
     * knockback, spawns particles, and calls {@link SwordEntity#hit(Combatant source, long hitInvulnerableTickDuration, int baseNumShards, float baseToughnessDamage, float baseSoulfireReduction, Vector knockbackVelocity, Affliction... afflictions)} for each
     * target intersected by the attack path.
     *
     * @param executor The combatant performing the slash.
     * @param type The attack variant (e.g., grounded, aerial, downward).
     */
    public static void basicSlash(Combatant executor, AttackType type) {
        long castDuration = (long) executor.calcValueReductive(AspectType.FINESSE, 1L, 3L, 0.2);
        if (executor instanceof SwordPlayer sp) sp.player().setCooldown(sp.getItemTypeInHand(true), (int) castDuration);
        cast(executor, castDuration,
            new BukkitRunnable() {
                @Override
                public void run() {
                    // Testing:
                    executor.hit(executor, 5, 1, 60, 6, Prefab.Direction.UP);

                    executor.message("Soulfire 'Hunger': " + 20 * (executor.getAspects().soulfireCur()/executor.getAspects().soulfireVal()));
                    //

                    SoundUtil.playSound(executor.entity(), SoundType.ENTITY_ENDER_DRAGON_FLAP, 0.35f, 0.6f);

                    executor.setTimeOfLastAttack(System.currentTimeMillis());
                    executor.setDurationOfLastAttack((int) castDuration * 500);

                    LivingEntity ex = executor.entity();
                    double damage = 20;

                    int numSteps = 50;

                    double rangeMultiplier;
                    List<Vector> controlVectors;
                    List<Vector> bezierVectors;
                    boolean withPitch = true;
                    boolean aerial = false;
                    switch (type) {
                        case BASIC_2 -> {
                            rangeMultiplier = 1.4;
                            controlVectors = new ArrayList<>(Prefab.ControlVectors.SLASH2);
                        }
                        case BASIC_3 -> {
                            rangeMultiplier = 1.4;
                            controlVectors = new ArrayList<>(Prefab.ControlVectors.SLASH3);
                        }
                        case N_AIR -> {
                            rangeMultiplier = 1.3;
                            controlVectors = new ArrayList<>(Prefab.ControlVectors.N_AIR_SLASH);
                            aerial = true;
                        }
                        case DOWN_AIR -> {
                            rangeMultiplier = 1.2;
                            controlVectors = new ArrayList<>(Prefab.ControlVectors.D_AIR_SLASH);
                            withPitch = false;
                            aerial = true;
                        }
                        default -> {
                            rangeMultiplier = 1.4;
                            controlVectors = new ArrayList<>(Prefab.ControlVectors.SLASH1);
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

                                Prefab.Particles.TEST_SWING.display(l);

                                if (s > size * (0.1)) {
                                    Location p = l.clone().subtract(v.clone().multiply(0.05 * ((double) s / (size))).add(n.multiply(0.2)));
                                    Prefab.Particles.TEST_SWING.display(p);
                                }
                                if (s > size * (0.3)) {
                                    Location p = l.clone().subtract(v.clone().multiply(0.25 * ((double) s / (size))).add(n.multiply(2)));
                                    Prefab.Particles.TEST_SWING.display(p);
                                }
                                if (s > size * (0.5)) {
                                    Location p = l.clone().subtract(v.clone().multiply(0.1 * ((double) s / (size))).add(n.multiply(0.5)));
                                    Prefab.Particles.TEST_SWING.display(p);
                                    Location p2 = l.clone().subtract(v.clone().multiply(0.25 * ((double) s / (size))).add(n.multiply(1.75)));
                                    Prefab.Particles.TEST_SWING.display(p2);
                                }
                                if (s > size * (0.625)) {
                                    Location p = l.clone().subtract(v.clone().multiply(0.05 * ((double) s / (size))).add(n.multiply(2.5)));
                                    Prefab.Particles.TEST_SWING.display(p);
                                    Location p2 = l.clone().subtract(v.clone().multiply(0.05 * ((double) s / (size))).add(n.multiply(1.5)));
                                    Prefab.Particles.TEST_SWING.display(p2);
                                }
                                if (s > size * (0.75)) {
                                    Location p = l.clone().subtract(v.clone().multiply(0.15 * ((double) s / (size))).add(n.multiply(0.6)));
                                    Prefab.Particles.TEST_SWING.display(p);
                                    Location p2 = l.clone().subtract(v.clone().multiply(0.2 * ((double) s / (size))).add(n.multiply(0.55)));
                                    Prefab.Particles.TEST_SWING.display(p2);
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
                                            Prefab.Particles.TEST_HIT.display(sTarget.getChestLocation());
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
                                        Prefab.Particles.COLLIDE.display(l);
                                        // potential reduction of damage formula
                                        d[0] = Math.max(d[0] *(0.2), d[0]-1);
                                    }
                                    else if (direction.lengthSquared() > (double) 2 / (size*size)) {
                                        // interpolated particle, same as normal particle
                                        Prefab.Particles.TEST_SWING.display(l.add(direction.multiply(0.5)));
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
}
