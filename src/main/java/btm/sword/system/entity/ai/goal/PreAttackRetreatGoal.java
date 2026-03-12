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
 * Pathfinds the {@link Hostile} away from its target during a ranged pre-attack wind-up.
 *
 * <p>Recomputes the backoff vector each tick to continuously retreat even if the target moves.
 * Stays active while {@code preAttackTimer > 0}.
 * Registered by {@code PreAttackState.onEnter} for
 * {@link btm.sword.system.entity.ai.ability.AbilityCategory#RANGED} abilities and removed by
 * {@code PreAttackState.onExit}.
 */
public class PreAttackRetreatGoal implements Goal<@NotNull Mob> {

    /** Unique key for this goal; used for lookup and removal. */
    public static final GoalKey<@NotNull Mob> KEY =
        GoalKey.of(Mob.class, new NamespacedKey(Sword.getInstance(), "pre_attack_retreat"));

    private static final double RETREAT_DISTANCE = 5.0;
    private static final double RETREAT_SPEED = 1.1;

    private final Mob mob;
    private final Hostile hostile;

    /**
     * Constructs a {@code PreAttackRetreatGoal} for the given mob and its Sword wrapper.
     *
     * @param mob     the Bukkit {@link Mob} to pathfind
     * @param hostile the {@link Hostile} wrapper supplying target and timer references
     */
    public PreAttackRetreatGoal(Mob mob, Hostile hostile) {
        this.mob = mob;
        this.hostile = hostile;
    }

    /** Activates immediately on registration. */
    @Override
    public boolean shouldActivate() {
        return true;
    }

    /** Stays active while the pre-attack wind-up timer is still counting down. */
    @Override
    public boolean shouldStayActive() {
        return hostile.getPreAttackTimer() > 0;
    }

    @Override
    public void start() {
        retreatFromTarget();
    }

    @Override
    public void stop() {
        mob.getPathfinder().stopPathfinding();
    }

    /** Re-issues the retreat path each tick so the mob tracks the target's movement. */
    @Override
    public void tick() {
        retreatFromTarget();
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
            away = new Vector(1, 0, 0);
        }

        Location retreatPos = mob.getLocation().add(away.normalize().multiply(RETREAT_DISTANCE));
        mob.getPathfinder().moveTo(retreatPos, RETREAT_SPEED);
    }
}
