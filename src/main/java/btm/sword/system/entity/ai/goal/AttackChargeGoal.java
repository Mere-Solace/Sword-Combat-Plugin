package btm.sword.system.entity.ai.goal;

import java.util.EnumSet;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Mob;
import org.jetbrains.annotations.NotNull;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;

import btm.sword.Sword;
import btm.sword.system.entity.impl.Hostile;

/**
 * Charges the {@link Hostile} toward its current target at 160% movement speed.
 *
 * <p>Registered by {@code AttackState.onEnter} and removed by {@code AttackState.onExit}.
 * The FSM state handles the melee-reach check and attack execution; this goal only
 * drives the movement.
 */
public class AttackChargeGoal implements Goal<@NotNull Mob> {

    /** Unique key for this goal; used for lookup and removal. */
    public static final GoalKey<@NotNull Mob> KEY =
        GoalKey.of(Mob.class, new NamespacedKey(Sword.getInstance(), "attack_charge"));

    private static final double CHARGE_SPEED = 1.6;

    private final Mob mob;
    private final Hostile hostile;

    /**
     * Constructs an {@code AttackChargeGoal} for the given mob and its Sword wrapper.
     *
     * @param mob     the Bukkit {@link Mob} to pathfind
     * @param hostile the {@link Hostile} wrapper owning the current target reference
     */
    public AttackChargeGoal(Mob mob, Hostile hostile) {
        this.mob = mob;
        this.hostile = hostile;
    }

    /**
     * Activates immediately on registration; the FSM has already decided to enter
     * {@code AttackState}.
     *
     * @return {@code true} always
     */
    @Override
    public boolean shouldActivate() {
        return true;
    }

    /**
     * Deactivates once the attack has landed or the target becomes invalid.
     *
     * @return {@code false} when attack is done or target is gone
     */
    @Override
    public boolean shouldStayActive() {
        if (hostile.isAttackDone()) return false;
        if (hostile.getCurrentTarget() == null) return false;
        return hostile.getCurrentTarget().self().isValid();
    }

    /** Issues the initial charge command when the goal becomes active. */
    @Override
    public void start() {
        chargeToTarget();
    }

    /** Stops pathfinding when the goal deactivates. */
    @Override
    public void stop() {
        mob.getPathfinder().stopPathfinding();
    }

    /** Re-issues the charge path each tick so the mob tracks a moving target. */
    @Override
    public void tick() {
        chargeToTarget();
    }

    @Override
    public @NotNull GoalKey<@NotNull Mob> getKey() {
        return KEY;
    }

    @Override
    public @NotNull EnumSet<GoalType> getTypes() {
        return EnumSet.of(GoalType.MOVE);
    }

    private void chargeToTarget() {
        if (hostile.getCurrentTarget() == null || !hostile.getCurrentTarget().self().isValid()) return;
        mob.getPathfinder().moveTo(hostile.getCurrentTarget().self(), CHARGE_SPEED);
    }
}
