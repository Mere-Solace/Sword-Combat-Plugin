package btm.sword.system.entity.ai.goal;

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
import btm.sword.system.entity.impl.Hostile;

/**
 * Pathfinds the {@link Hostile} away from its target after an attack lands.
 *
 * <p>Computes a retreat position once on {@link #start()} and pathfinds there at 110%
 * speed. The FSM's {@code retreatTimer} controls re-engagement timing; this goal runs
 * for the duration of that timer.
 * Registered by {@code RetreatState.onEnter} and removed by {@code RetreatState.onExit}.
 */
public class RetreatBackoffGoal implements Goal<@NotNull Mob> {

    /** Unique key for this goal; used for lookup and removal. */
    public static final GoalKey<@NotNull Mob> KEY =
        GoalKey.of(Mob.class, new NamespacedKey(Sword.getInstance(), "retreat_backoff"));

    private static final double RETREAT_DISTANCE = 8.0;
    private static final double RETREAT_SPEED = 1.1;

    private final Mob mob;
    private final Hostile hostile;

    /**
     * Constructs a {@code RetreatBackoffGoal} for the given mob and its Sword wrapper.
     *
     * @param mob     the Bukkit {@link Mob} to pathfind
     * @param hostile the {@link Hostile} wrapper owning the current target reference
     */
    public RetreatBackoffGoal(Mob mob, Hostile hostile) {
        this.mob = mob;
        this.hostile = hostile;
    }

    /**
     * Activates immediately on registration; the FSM has already decided to enter
     * {@code RetreatState}.
     *
     * @return {@code true} always
     */
    @Override
    public boolean shouldActivate() {
        return true;
    }

    /**
     * Deactivates when the retreat timer runs out.
     *
     * @return {@code true} while the retreat timer is still counting down
     */
    @Override
    public boolean shouldStayActive() {
        return hostile.getRetreatTimer() > 0;
    }

    /** Computes the retreat direction and issues the one-shot pathfind on activation. */
    @Override
    public void start() {
        retreatFromTarget();
    }

    /** Stops pathfinding when the goal deactivates. */
    @Override
    public void stop() {
        mob.getPathfinder().stopPathfinding();
    }

    /**
     * No per-tick pathfind re-issue needed; the retreat position is a static location
     * computed once on entry.
     */
    @Override
    public void tick() {
        // Retreat position is computed once in start(); no re-issue needed.
    }

    @Override
    public @NotNull GoalKey<@NotNull Mob> getKey() {
        return KEY;
    }

    @Override
    public @NotNull EnumSet<GoalType> getTypes() {
        return EnumSet.of(GoalType.MOVE);
    }

    private void retreatFromTarget() {
        if (hostile.getCurrentTarget() == null || !hostile.getCurrentTarget().self().isValid()) return;

        Vector away = mob.getLocation()
            .subtract(hostile.getCurrentTarget().self().getLocation())
            .toVector();

        if (away.lengthSquared() < 0.001) {
            away = new Vector(1, 0, 0); // fallback if positions are coincident
        }

        Location retreatPos = mob.getLocation().add(away.normalize().multiply(RETREAT_DISTANCE));
        mob.getPathfinder().moveTo(retreatPos, RETREAT_SPEED);
    }
}
