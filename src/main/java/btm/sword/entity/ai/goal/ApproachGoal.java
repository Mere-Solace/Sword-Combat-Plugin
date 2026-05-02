package btm.sword.entity.ai.goal;

import java.util.EnumSet;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Mob;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;

import btm.sword.Sword;
import btm.sword.config.Config;
import btm.sword.entity.mob.Hostile;

/**
 * Pathfinds toward the {@link Hostile}'s current target at 110% movement speed.
 *
 * <p>Registered by {@code ApproachState.onEnter} and removed by {@code ApproachState.onExit}.
 * {@link #shouldStayActive()} mirrors the FSM's own approach exit condition as a secondary
 * safety net.
 */
public class ApproachGoal implements Goal<@NotNull Mob> {

    /** Unique key for this goal; used for lookup and removal. */
    public static final GoalKey<@NotNull Mob> KEY =
        GoalKey.of(Mob.class, new NamespacedKey(Sword.getInstance(), "approach"));

    private static final double APPROACH_SPEED = 1.1;

    /** Squared hysteresis margin (2 blocks) to prevent start/stop oscillation at the boundary. */
    private static final double HYSTERESIS_SQ = 4.0;

    /**
     * Degrees of angle change per swivel tick.
     * A 90-degree turn takes 6 ticks; a 180-degree reversal takes 12 ticks (~0.6 s).
     */
    private static final double SWIVEL_DEGS_PER_TICK = 15.0;

    private final Mob mob;
    private final Hostile hostile;

    /** Ticks remaining in the initial swivel phase before pathfinding begins. */
    private int swivelTicksRemaining;

    /**
     * Constructs an {@code ApproachGoal} for the given mob and its Sword wrapper.
     *
     * @param mob     the Bukkit {@link Mob} to pathfind
     * @param hostile the {@link Hostile} wrapper owning the current target reference
     */
    public ApproachGoal(Mob mob, Hostile hostile) {
        this.mob = mob;
        this.hostile = hostile;
    }

    /**
     * Activates immediately on registration; the FSM has already decided to enter
     * {@code ApproachState}.
     *
     * @return {@code true} always
     */
    @Override
    public boolean shouldActivate() {
        return true;
    }

    /**
     * Returns {@code false} when the target is gone or has left aggro range,
     * acting as a secondary exit signal alongside the FSM transition.
     *
     * @return {@code true} while the target is valid and within aggro range
     */
    @Override
    public boolean shouldStayActive() {
        if (hostile.getCurrentTarget() == null) return false;
        if (!hostile.getCurrentTarget().self().isValid()) return false;
        return mob.getLocation().distanceSquared(hostile.getCurrentTarget().self().getLocation())
            <= Config.Hostile.AGGRO_RANGE_SQUARED;
    }

    /**
     * Computes swivel ticks then either starts pathfinding immediately (small angle)
     * or enters the swivel phase first ({@link LookAtTargetGoal} handles the actual rotation).
     */
    @Override
    public void start() {
        swivelTicksRemaining = computeSwivelTicks();
        if (swivelTicksRemaining == 0) {
            pathfindToTarget();
        }
    }

    /** Stops pathfinding when the goal deactivates. */
    @Override
    public void stop() {
        mob.getPathfinder().stopPathfinding();
        swivelTicksRemaining = 0;
    }

    /**
     * During the swivel phase, waits for the mob to turn before pathfinding.
     * After the swivel completes, re-issues the pathfind command each tick to track the target.
     * Pathfinding stops once within the approach threshold (plus hysteresis band).
     */
    @Override
    public void tick() {
        if (swivelTicksRemaining > 0) {
            swivelTicksRemaining--;
            if (swivelTicksRemaining == 0) {
                pathfindToTarget();
            }
            return;
        }
        if (hostile.getCurrentTarget() == null || !hostile.getCurrentTarget().self().isValid()) return;
        double distSq = mob.getLocation().distanceSquared(hostile.getCurrentTarget().self().getLocation());
        if (distSq > Config.Hostile.APPROACH_DISTANCE_SQUARED + HYSTERESIS_SQ) {
            pathfindToTarget();
        } else {
            mob.getPathfinder().stopPathfinding();
        }
    }

    @Override
    public @NotNull GoalKey<@NotNull Mob> getKey() {
        return KEY;
    }

    @Override
    public @NotNull EnumSet<GoalType> getTypes() {
        return EnumSet.of(GoalType.MOVE);
    }

    private void pathfindToTarget() {
        if (hostile.getCurrentTarget() == null) return;
        mob.getPathfinder().moveTo(hostile.getCurrentTarget().self(), APPROACH_SPEED);
    }

    /**
     * Calculates how many swivel ticks are needed before pathfinding starts.
     * Based on the angle between the mob's current facing direction and the direction to the target.
     * Returns 0 for angles smaller than {@value #SWIVEL_DEGS_PER_TICK} degrees (no swivel needed).
     */
    private int computeSwivelTicks() {
        if (hostile.getCurrentTarget() == null || !hostile.getCurrentTarget().self().isValid()) return 0;
        Location mobLoc = mob.getLocation();
        Location targetLoc = hostile.getCurrentTarget().self().getLocation();
        Vector toTarget = targetLoc.subtract(mobLoc).toVector().setY(0);
        if (toTarget.lengthSquared() < 0.001) return 0;
        Vector facing = mobLoc.getDirection().setY(0);
        if (facing.lengthSquared() < 0.001) return 0;
        double dot = Math.max(-1.0, Math.min(1.0, facing.normalize().dot(toTarget.normalize())));
        double angleDeg = Math.toDegrees(Math.acos(dot));
        return (int) (angleDeg / SWIVEL_DEGS_PER_TICK);
    }
}
