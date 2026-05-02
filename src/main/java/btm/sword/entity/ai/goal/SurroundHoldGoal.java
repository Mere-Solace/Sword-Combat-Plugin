package btm.sword.entity.ai.goal;

import java.util.EnumSet;

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
 * Pathfinds the {@link Hostile} to its assigned arc-slot position around the target.
 *
 * <p>The arc slot index and ally count are maintained by {@code SurroundState.onTick};
 * this goal only issues the movement commands based on those current values.
 * Registered by {@code SurroundState.onEnter} and removed by {@code SurroundState.onExit}.
 */
public class SurroundHoldGoal implements Goal<@NotNull Mob> {

    /** Unique key for this goal; used for lookup and removal. */
    public static final GoalKey<@NotNull Mob> KEY =
        GoalKey.of(Mob.class, new NamespacedKey(Sword.getInstance(), "surround_hold"));

    private static final double SURROUND_SPEED = 1.0;

    private final Mob mob;
    private final Hostile hostile;

    /**
     * Constructs a {@code SurroundHoldGoal} for the given mob and its Sword wrapper.
     *
     * @param mob     the Bukkit {@link Mob} to pathfind
     * @param hostile the {@link Hostile} wrapper owning arc-slot state
     */
    public SurroundHoldGoal(Mob mob, Hostile hostile) {
        this.mob = mob;
        this.hostile = hostile;
    }

    /**
     * Activates immediately on registration; the FSM has already decided to enter
     * {@code SurroundState}.
     *
     * @return {@code true} always
     */
    @Override
    public boolean shouldActivate() {
        return true;
    }

    /**
     * Deactivates if the target becomes invalid.
     *
     * @return {@code true} while the target is valid
     */
    @Override
    public boolean shouldStayActive() {
        if (hostile.getCurrentTarget() == null) return false;
        return hostile.getCurrentTarget().self().isValid();
    }

    /** Pathfinds to the initial arc position when the goal becomes active. */
    @Override
    public void start() {
        pathfindToArcPosition();
    }

    /** Stops pathfinding when the goal deactivates. */
    @Override
    public void stop() {
        mob.getPathfinder().stopPathfinding();
    }

    /**
     * Re-issues the arc position path each tick so movement updates when the
     * FSM recalculates the slot assignment.
     */
    @Override
    public void tick() {
        pathfindToArcPosition();
    }

    @Override
    public @NotNull GoalKey<@NotNull Mob> getKey() {
        return KEY;
    }

    @Override
    public @NotNull EnumSet<GoalType> getTypes() {
        return EnumSet.of(GoalType.MOVE);
    }

    private void pathfindToArcPosition() {
        if (hostile.getCurrentTarget() == null || !hostile.getCurrentTarget().self().isValid()) return;

        double radius = Math.sqrt(Config.Hostile.APPROACH_DISTANCE_SQUARED);
        int totalMobs = Math.max(1, hostile.getNearbyAlliesCount() + 1);
        double angle = 2 * Math.PI * hostile.getArcSlotIndex() / totalMobs;

        Location targetLoc = hostile.getCurrentTarget().self().getLocation();
        double x = targetLoc.getX() + radius * Math.cos(angle);
        double z = targetLoc.getZ() + radius * Math.sin(angle);
        Location arcPos = new Location(targetLoc.getWorld(), x, targetLoc.getY(), z);

        mob.getPathfinder().moveTo(arcPos, SURROUND_SPEED);
    }
}
