package btm.sword.entity.ai.goal;

import java.util.EnumSet;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Mob;
import org.jetbrains.annotations.NotNull;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;

import btm.sword.Sword;
import btm.sword.entity.mob.Hostile;

/**
 * Look goal that continuously orients the mob toward its current combat target.
 *
 * <p>Registered by states where the mob should face the player it is engaging —
 * currently {@code ApproachState}, {@code SurroundState}, and {@code AttackState}.
 * Removed by each state's {@code onExit} via {@code removeAllGoals(mob, GoalType.LOOK)}.
 */
public class LookAtTargetGoal implements Goal<@NotNull Mob> {

    /** Unique key used for lookup and removal. */
    public static final GoalKey<@NotNull Mob> KEY =
        GoalKey.of(Mob.class, new NamespacedKey(Sword.getInstance(), "look_at_target"));

    /** Yaw (horizontal) turn speed in degrees per tick. */
    private static final float TURN_YAW = 30f;

    /** Pitch (vertical) turn speed in degrees per tick. */
    private static final float TURN_PITCH = 10f;

    private final Mob mob;
    private final Hostile hostile;

    /**
     * Constructs a {@code LookAtTargetGoal} for the given mob and its Sword wrapper.
     *
     * @param mob     the Bukkit {@link Mob} to orient
     * @param hostile the {@link Hostile} wrapper supplying the current target reference
     */
    public LookAtTargetGoal(Mob mob, Hostile hostile) {
        this.mob = mob;
        this.hostile = hostile;
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

    /** Faces the mob toward {@code currentTarget} each tick. */
    @Override
    public void tick() {
        if (hostile.getCurrentTarget() == null || !hostile.getCurrentTarget().self().isValid()) return;
        mob.lookAt(hostile.getCurrentTarget().self(), TURN_YAW, TURN_PITCH);
    }

    @Override
    public @NotNull GoalKey<@NotNull Mob> getKey() {
        return KEY;
    }

    @Override
    public @NotNull EnumSet<GoalType> getTypes() {
        return EnumSet.of(GoalType.LOOK);
    }
}
