package btm.sword.system.entity.ai.goal;

import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Optional;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;

import btm.sword.Sword;
import btm.sword.config.Config;
import btm.sword.system.entity.impl.Hostile;

/**
 * Pathfinds the {@link Hostile} away from the nearest player when at low health.
 *
 * <p>Recalculates the flee direction every 20 ticks so the mob continues to track
 * moving players. Registered by {@code FleeState.onEnter} and removed by
 * {@code FleeState.onExit}.
 */
public class FleeGoal implements Goal<@NotNull Mob> {

    /** Unique key for this goal; used for lookup and removal. */
    public static final GoalKey<@NotNull Mob> KEY =
        GoalKey.of(Mob.class, new NamespacedKey(Sword.getInstance(), "flee"));

    private static final double FLEE_DISTANCE = 16.0;
    private static final double FLEE_SPEED = 1.5;
    private static final int RECALC_CADENCE = 20;

    private final Mob mob;
    private final Hostile hostile;
    private int recalcTick = 0;

    /**
     * Constructs a {@code FleeGoal} for the given mob and its Sword wrapper.
     *
     * @param mob     the Bukkit {@link Mob} to pathfind
     * @param hostile the {@link Hostile} wrapper providing location context
     */
    public FleeGoal(Mob mob, Hostile hostile) {
        this.mob = mob;
        this.hostile = hostile;
    }

    /**
     * Activates immediately on registration; the FSM has already decided to enter
     * {@code FleeState}.
     *
     * @return {@code true} always
     */
    @Override
    public boolean shouldActivate() {
        return true;
    }

    /**
     * Deactivates when no players remain within aggro range, signalling the
     * FSM to return to idle.
     *
     * @return {@code true} while at least one player is still within aggro range
     */
    @Override
    public boolean shouldStayActive() {
        double aggroRadius = Math.sqrt(Config.Hostile.AGGRO_RANGE_SQUARED);
        return !mob.getWorld().getNearbyPlayers(mob.getLocation(), aggroRadius).isEmpty();
    }

    /** Issues the initial flee path and resets the recalculation counter. */
    @Override
    public void start() {
        recalcTick = 0;
        recalculateFleeDirection();
    }

    /** Stops pathfinding when the goal deactivates. */
    @Override
    public void stop() {
        mob.getPathfinder().stopPathfinding();
    }

    /** Recalculates the flee direction every {@value #RECALC_CADENCE} ticks. */
    @Override
    public void tick() {
        if (hostile.shouldRunAllyScan()) {
            recalculateFleeDirection();
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

    private void recalculateFleeDirection() {
        double aggroRadius = Math.sqrt(Config.Hostile.AGGRO_RANGE_SQUARED);
        Collection<Player> nearbyPlayers = mob.getWorld().getNearbyPlayers(mob.getLocation(), aggroRadius);
        if (nearbyPlayers.isEmpty()) return;

        Optional<Player> nearest = nearbyPlayers.stream()
            .min(Comparator.comparingDouble(p -> p.getLocation().distanceSquared(mob.getLocation())));
        if (nearest.isEmpty()) return;

        Vector away = mob.getLocation()
            .subtract(nearest.get().getLocation())
            .toVector();

        if (away.lengthSquared() < 0.001) {
            away = new Vector(1, 0, 0); // fallback if positions are coincident
        }

        Location fleePos = mob.getLocation().add(away.normalize().multiply(FLEE_DISTANCE));
        mob.getPathfinder().moveTo(fleePos, FLEE_SPEED);
    }
}
