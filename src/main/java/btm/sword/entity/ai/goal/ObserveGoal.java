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

/**
 * Look goal that slowly pans the mob's gaze to random nearby points.
 *
 * <p>Simulates an aware but undisturbed mob casually observing its surroundings.
 * The look target changes every {@value #LOOK_CHANGE_CADENCE} ticks, creating a natural
 * idle-observation effect. Intended for states where the mob is stationary and has no
 * target to track.
 */
public class ObserveGoal implements Goal<@NotNull Mob> {

    /** Unique key used for lookup and removal. */
    public static final GoalKey<@NotNull Mob> KEY =
        GoalKey.of(Mob.class, new NamespacedKey(Sword.getInstance(), "observe"));

    /** Ticks between look-direction changes. */
    private static final int LOOK_CHANGE_CADENCE = 20;

    /** Distance in blocks the mob looks toward when picking a random direction. */
    private static final double LOOK_DISTANCE = 5.0;

    /** Pitch variation range (blocks) for slight up/down tilt. */
    private static final double LOOK_PITCH_RANGE = 0.8;

    /** Yaw (horizontal) turn speed in degrees per tick — slow for casual observation. */
    private static final float TURN_YAW = 10f;

    /** Pitch (vertical) turn speed in degrees per tick. */
    private static final float TURN_PITCH = 5f;

    private final Mob mob;
    private int lookChangeTick;
    @Nullable
    private Location lookTarget;

    /**
     * Constructs an {@code ObserveGoal} for the given mob.
     *
     * @param mob the Bukkit {@link Mob} to orient
     */
    public ObserveGoal(Mob mob) {
        this.mob = mob;
    }

    /** Activates immediately; the FSM has already decided to enter the owning state. */
    @Override
    public boolean shouldActivate() {
        return true;
    }

    /** Stays active until the owning state's {@code onExit} removes it. */
    @Override
    public boolean shouldStayActive() {
        return true;
    }

    /** Picks an initial look target when the goal activates. */
    @Override
    public void start() {
        lookChangeTick = 0;
        pickNewLookTarget();
    }

    /** Clears the look target when the goal deactivates. */
    @Override
    public void stop() {
        lookTarget = null;
    }

    /** Pans to the current look target; picks a new one every {@value #LOOK_CHANGE_CADENCE} ticks. */
    @Override
    public void tick() {
        lookChangeTick++;
        if (lookChangeTick >= LOOK_CHANGE_CADENCE) {
            lookChangeTick = 0;
            pickNewLookTarget();
        }
        if (lookTarget != null) {
            mob.lookAt(lookTarget, TURN_YAW, TURN_PITCH);
        }
    }

    @Override
    public @NotNull GoalKey<@NotNull Mob> getKey() {
        return KEY;
    }

    @Override
    public @NotNull EnumSet<GoalType> getTypes() {
        return EnumSet.of(GoalType.LOOK);
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
