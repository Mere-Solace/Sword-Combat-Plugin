package btm.sword.entity.ai.goal;

import java.util.EnumSet;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Mob;
import org.jetbrains.annotations.NotNull;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;

import btm.sword.Sword;
import btm.sword.config.Config;
import btm.sword.entity.mob.Hostile;

/**
 * Move goal for {@code OnGuardState}.
 *
 * <p>Backs off from the target to the configured safe distance and then strafes
 * laterally by incrementally advancing an orbit angle around the target, recalculating
 * the path every {@value #STRAFE_RECALC_CADENCE} ticks. This creates a weaving, circling
 * motion that makes the mob appear defensive while still presenting a threat.
 */
public class OnGuardBackoffGoal implements Goal<@NotNull Mob> {

    /** Unique key used for lookup and removal. */
    public static final GoalKey<@NotNull Mob> KEY =
        GoalKey.of(Mob.class, new NamespacedKey(Sword.getInstance(), "on_guard_backoff"));

    private static final double ON_GUARD_SPEED = 1.0;

    /** Ticks between strafe destination recalculations. */
    private static final int STRAFE_RECALC_CADENCE = 10;

    /** Angle advanced per recalculation, in radians. Controls how fast the mob orbits. */
    private static final double ORBIT_STEP_MIN = 0.3;
    private static final double ORBIT_STEP_MAX = 0.8;

    private final Mob mob;
    private final Hostile hostile;

    private int strafeRecalcTick;
    private double orbitAngle;

    /**
     * Constructs an {@code OnGuardBackoffGoal} for the given mob and its Sword wrapper.
     *
     * @param mob     the Bukkit {@link Mob} to pathfind
     * @param hostile the {@link Hostile} wrapper supplying the current target reference
     */
    public OnGuardBackoffGoal(Mob mob, Hostile hostile) {
        this.mob = mob;
        this.hostile = hostile;
    }

    /** Activates immediately; the FSM has already decided to enter {@code OnGuardState}. */
    @Override
    public boolean shouldActivate() {
        return true;
    }

    /**
     * Stays active while the on-guard timer is running and the target is valid.
     *
     * @return {@code false} once the timer expires or the target is gone
     */
    @Override
    public boolean shouldStayActive() {
        if (hostile.getOnGuardTimer() <= 0) return false;
        if (hostile.getCurrentTarget() == null) return false;
        return hostile.getCurrentTarget().self().isValid();
    }

    /** Picks an initial orbit angle and triggers an immediate path recalculation. */
    @Override
    public void start() {
        mob.setAware(true);
        orbitAngle = ThreadLocalRandom.current().nextDouble(Math.PI * 2);
        strafeRecalcTick = STRAFE_RECALC_CADENCE; // force recalc on first tick
    }

    /** Stops pathfinding when the goal deactivates. */
    @Override
    public void stop() {
        mob.getPathfinder().stopPathfinding();
    }

    /**
     * Advances the orbit angle every {@value #STRAFE_RECALC_CADENCE} ticks and pathfinds
     * to the new position on the safe-distance circle around the target.
     */
    @Override
    public void tick() {
        if (hostile.getCurrentTarget() == null || !hostile.getCurrentTarget().self().isValid()) return;

        strafeRecalcTick++;
        if (strafeRecalcTick < STRAFE_RECALC_CADENCE) return;
        strafeRecalcTick = 0;

        orbitAngle += ThreadLocalRandom.current().nextDouble(ORBIT_STEP_MIN, ORBIT_STEP_MAX);

        double radius = Math.sqrt(Config.Hostile.ON_GUARD_SAFE_DISTANCE_SQUARED);
        Location targetLoc = hostile.getCurrentTarget().self().getLocation();
        Location strafeDest = targetLoc.clone().add(
            Math.cos(orbitAngle) * radius,
            0,
            Math.sin(orbitAngle) * radius
        );

        mob.getPathfinder().moveTo(strafeDest, ON_GUARD_SPEED);
    }

    @Override
    public @NotNull GoalKey<@NotNull Mob> getKey() {
        return KEY;
    }

    @Override
    public @NotNull EnumSet<GoalType> getTypes() {
        return EnumSet.of(GoalType.MOVE);
    }
}
