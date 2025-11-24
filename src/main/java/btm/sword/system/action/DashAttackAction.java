package btm.sword.system.action;

import btm.sword.system.attack.Attack;
import btm.sword.system.attack.AttackType;
import btm.sword.system.entity.types.Combatant;
import btm.sword.system.entity.types.SwordPlayer;
import btm.sword.system.entity.umbral.input.BladeRequest;
import btm.sword.system.entity.umbral.statemachine.state.StandbyState;

public class DashAttackAction extends SwordAction {
    public static void dashAttack(Combatant executor, boolean forward) {
        executor.setTimeOfLastAttack(System.currentTimeMillis());
        executor.setDurationOfLastAttack(700);

        if (executor.getItemStackInHand(true).isEmpty()) {
            AttackAction.throwPunch(executor, forward ? 3.5 : 1);
            return;
        }

        if (executor.holdingSoulLink()) {
            if (executor.getAspects().soulfireCur() >= 10f &&
                executor.getUmbralBlade().inState(StandbyState.class)) {

                // TODO: fix and put in AttackingQuick state with some flags or smth
                executor.getUmbralBlade().setDashingDirection(forward);
                executor.getUmbralBlade().request(BladeRequest.ATTACK_QUICK);

                if (executor instanceof SwordPlayer swordPlayer) {
                    swordPlayer.resetTree(); // Reset the tree if they perform a quick attack with the blade.\
                }
                return;
            }

            AttackAction.throwPunch(executor, forward ? 3.5 : 1);
            return;
        }

        cast(executor, 500, () ->
            new Attack(forward ? AttackType.F_DASH_ATTACK : AttackType.B_DASH_ATTACK, forward,
                50,30,0,1)
                // use the direction of the dash so that players can't spin around and use the long attack omnidirectionally
                .setOrigin(forward ? executor.getChestLocation().setDirection(executor.getDashDirection()) : null)
                .execute(executor));
    }
}
