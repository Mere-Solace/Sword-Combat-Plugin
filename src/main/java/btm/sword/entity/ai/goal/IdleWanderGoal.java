package btm.sword.entity.ai.goal;

import java.util.EnumSet;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Mob;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;

import btm.sword.Sword;
import btm.sword.entity.ai.WanderProfile;
import btm.sword.entity.mob.Hostile;

/**
 * Three-phase idle wander goal: {@code IDLE} → {@code LOOK} → {@code WALK} → repeat.
 *
 * <ol>
 *   <li><b>IDLE</b> — The mob stands still for a random number of ticks determined by
 *       its {@link WanderProfile}. Gives the appearance of a mob that is resting or
 *       simply waiting before deciding to move.</li>
 *   <li><b>LOOK</b> — The mob slowly turns to look in several random directions. The
 *       look target changes every {@value #LOOK_CHANGE_CADENCE} ticks. Duration is
 *       randomised within the profile's look-tick range.</li>
 *   <li><b>WALK</b> — The mob pathfinds to a random point within
 *       {@link WanderProfile#wanderRadius} blocks of its spawn origin. Once the path
 *       is complete (or fails immediately), the cycle resets to IDLE.</li>
 * </ol>
 *
 * <p>Registered by {@code IdleState.onEnter} and removed by {@code IdleState.onExit}.
 */
public class IdleWanderGoal implements Goal<@NotNull Mob> {

    /** Unique key for this goal; used for lookup and removal. */
    public static final GoalKey<@NotNull Mob> KEY =
        GoalKey.of(Mob.class, new NamespacedKey(Sword.getInstance(), "idle_wander"));

    /** Movement speed multiplier used during the walk phase (slower than combat). */
    private static final double WANDER_SPEED = 0.8;

    /** Ticks between look-direction changes during the LOOK phase. */
    private static final int LOOK_CHANGE_CADENCE = 15;

    /** Distance in blocks the mob looks toward when picking a random look direction. */
    private static final double LOOK_DISTANCE = 5.0;

    /** Pitch variation range (blocks) when generating a look target — slight up/down tilt. */
    private static final double LOOK_PITCH_RANGE = 0.8;

    private enum Phase {
        IDLE, LOOK, WALK
    }

    private final Mob mob;
    private final Hostile hostile;

    private Phase phase = Phase.IDLE;
    private int phaseTicksRemaining = 0;
    private int lookChangeTick = 0;
    @Nullable
    private Location lookTarget = null;

    /**
     * Constructs an {@code IdleWanderGoal} for the given mob and its Sword wrapper.
     *
     * @param mob     the Bukkit {@link Mob} to control
     * @param hostile the {@link Hostile} wrapper supplying the origin and wander profile
     */
    public IdleWanderGoal(Mob mob, Hostile hostile) {
        this.mob = mob;
        this.hostile = hostile;
    }

    /**
     * Activates immediately on registration; the FSM has already decided to enter
     * {@code IdleState}.
     *
     * @return {@code true} always
     */
    @Override
    public boolean shouldActivate() {
        return true;
    }

    /**
     * Stays active for the duration of {@code IdleState}; the FSM handles
     * deactivation by removing the goal in {@code IdleState.onExit}.
     *
     * @return {@code true} always
     */
    @Override
    public boolean shouldStayActive() {
        return true;
    }

    /** Enters the IDLE phase when the goal first activates. */
    @Override
    public void start() {
        enterIdle();
    }

    /** Stops any active pathfinding when the goal deactivates. */
    @Override
    public void stop() {
        mob.getPathfinder().stopPathfinding();
        lookTarget = null;
    }

    /** Advances the current phase each tick. */
    @Override
    public void tick() {
        switch (phase) {
            case IDLE -> {
                phaseTicksRemaining--;
                if (phaseTicksRemaining <= 0) enterLook();
            }
            case LOOK -> {
                phaseTicksRemaining--;
                handleLook();
                if (phaseTicksRemaining <= 0) enterWalk();
            }
            case WALK -> {
                if (!mob.getPathfinder().hasPath()) enterIdle();
            }
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

    // --- Phase transitions ---

    private void enterIdle() {
        phase = Phase.IDLE;
        mob.getPathfinder().stopPathfinding();
        WanderProfile profile = hostile.getWanderProfile();
        phaseTicksRemaining = ThreadLocalRandom.current()
            .nextInt(profile.minIdleTicks, profile.maxIdleTicks + 1);
    }

    private void enterLook() {
        phase = Phase.LOOK;
        WanderProfile profile = hostile.getWanderProfile();
        phaseTicksRemaining = ThreadLocalRandom.current()
            .nextInt(profile.minLookTicks, profile.maxLookTicks + 1);
        lookChangeTick = 0;
        pickNewLookTarget();
    }

    private void enterWalk() {
        phase = Phase.WALK;
        lookTarget = null;

        WanderProfile profile = hostile.getWanderProfile();
        Location origin = hostile.getOrigin();
        double radius = profile.wanderRadius;

        double angle = ThreadLocalRandom.current().nextDouble(Math.PI * 2);
        double dist = ThreadLocalRandom.current().nextDouble(radius * 0.4, radius);
        Location target = origin.clone().add(Math.cos(angle) * dist, 0, Math.sin(angle) * dist);

        if (!mob.getPathfinder().moveTo(target, WANDER_SPEED)) {
            enterIdle(); // pathfinding to target failed; rest and try again later
        }
    }

    // --- Look phase helpers ---

    private void handleLook() {
        lookChangeTick++;
        if (lookChangeTick >= LOOK_CHANGE_CADENCE) {
            lookChangeTick = 0;
            pickNewLookTarget();
        }
        if (lookTarget != null) {
            mob.lookAt(lookTarget, 20f, 10f);
        }
    }

    private void pickNewLookTarget() {
        double yaw = ThreadLocalRandom.current().nextDouble(Math.PI * 2);
        double dy = ThreadLocalRandom.current().nextDouble(-LOOK_PITCH_RANGE, LOOK_PITCH_RANGE);
        Location base = mob.getLocation();
        lookTarget = base.clone().add(
            Math.cos(yaw) * LOOK_DISTANCE,
            dy,
            Math.sin(yaw) * LOOK_DISTANCE
        );
    }
}
