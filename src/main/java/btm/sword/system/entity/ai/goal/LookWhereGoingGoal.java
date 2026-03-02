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

/**
 * Look goal that orients the mob in the direction it is currently moving.
 *
 * <p>Registered by states where the mob is retreating or fleeing, so it naturally faces
 * its direction of travel — currently {@code RetreatState} and {@code FleeState}.
 * Removed by each state's {@code onExit} via {@code removeAllGoals(mob, GoalType.LOOK)}.
 */
public class LookWhereGoingGoal implements Goal<@NotNull Mob> {

    /** Unique key used for lookup and removal. */
    public static final GoalKey<@NotNull Mob> KEY =
        GoalKey.of(Mob.class, new NamespacedKey(Sword.getInstance(), "look_where_going"));

    /** Minimum horizontal velocity squared before a look update is issued (avoids jitter when nearly stopped). */
    private static final double MIN_VEL_SQ = 0.001;

    /** Distance ahead of the mob to project the look target along the velocity vector. */
    private static final double LOOK_AHEAD = 2.0;

    /** Yaw (horizontal) turn speed in degrees per tick. */
    private static final float TURN_YAW = 20f;

    /** Pitch (vertical) turn speed in degrees per tick. */
    private static final float TURN_PITCH = 10f;

    private final Mob mob;

    /**
     * Constructs a {@code LookWhereGoingGoal} for the given mob.
     *
     * @param mob the Bukkit {@link Mob} to orient
     */
    public LookWhereGoingGoal(Mob mob) {
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

    /** Orients the mob toward its current horizontal velocity direction each tick. */
    @Override
    public void tick() {
        Vector vel = mob.getVelocity().setY(0);
        if (vel.lengthSquared() < MIN_VEL_SQ) return;
        vel.normalize().multiply(LOOK_AHEAD);
        Location lookTarget = mob.getLocation().add(vel);
        mob.lookAt(lookTarget, TURN_YAW, TURN_PITCH);
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
