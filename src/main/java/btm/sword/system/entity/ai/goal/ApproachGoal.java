package btm.sword.system.entity.ai.goal;

import java.util.EnumSet;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Mob;
import org.jetbrains.annotations.NotNull;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;

import btm.sword.Sword;
import btm.sword.config.Config;
import btm.sword.system.entity.impl.Hostile;

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

    private final Mob mob;
    private final Hostile hostile;

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

    /** Issues the first pathfind command when the goal becomes active. */
    @Override
    public void start() {
        pathfindToTarget();
    }

    /** Stops pathfinding when the goal deactivates. */
    @Override
    public void stop() {
        mob.getPathfinder().stopPathfinding();
    }

    /**
     * Re-issues the pathfind command each tick so the mob tracks a moving target.
     * Pathfinding stops once within the approach threshold (plus hysteresis band).
     */
    @Override
    public void tick() {
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
}
