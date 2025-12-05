package btm.sword.system.action.attack;

import org.bukkit.inventory.ItemStack;

import btm.sword.system.action.SwordAction;
import btm.sword.system.action.movement.DashDirection;
import btm.sword.system.attack.Attack;
import btm.sword.system.attack.style.AttackProfile;
import btm.sword.system.attack.style.WeaponAttackStyle;
import btm.sword.system.entity.types.Combatant;
import btm.sword.system.entity.types.SwordPlayer;
import btm.sword.system.entity.umbral.input.BladeRequest;
import btm.sword.system.entity.umbral.statemachine.state.StandbyState;

public class DashAttackAction extends SwordAction {
    public static void dashAttack(Combatant executor, DashDirection direction) {
        ItemStack itemUsedInAttack = executor.getItemStackInHand(true);
        WeaponAttackStyle weaponAttackStyle = WeaponAttackStyle.fromString(itemUsedInAttack);

        boolean forward = direction.equals(DashDirection.FORWARD);

        executor.setTimeOfLastAttack(System.currentTimeMillis());
        executor.setDurationOfLastAttack(700);

        if (weaponAttackStyle.equals(WeaponAttackStyle.PUNCH)) { // catch any untagged items and perform a punch with it
            PunchAction.throwPunch(executor, true, forward ? 3.5 : 1);
            return;
        }

        if (executor.holdingSoulLink()) {
            if (executor.getAspects().soulfireCur() >= 10f &&
                executor.getUmbralBlade().inState(StandbyState.class)) {

                // TODO: #137 fix and put in AttackingQuick state with some flags or smth
                executor.getUmbralBlade().setDashingDirection(direction);
                executor.getUmbralBlade().request(BladeRequest.ATTACK_QUICK);

                if (executor instanceof SwordPlayer swordPlayer) {
                    swordPlayer.resetTree(); // Reset the tree if they perform a quick attack with the blade.
                }

                return;
            }

            PunchAction.throwPunch(executor, true, forward ? 3.5 : 1);
            return;
        }

        AttackProfile attackDir = switch (direction) {
            case FORWARD -> weaponAttackStyle.f_dash();
            case BACKWARD -> weaponAttackStyle.b_dash();
            case RIGHT -> weaponAttackStyle.r_strafe();
            case LEFT -> weaponAttackStyle.l_strafe();
            default -> weaponAttackStyle.attacks().getFirst();
        };

        cast(executor, 500, () ->
            new Attack(itemUsedInAttack, attackDir, !direction.equals(DashDirection.BACKWARD),
                50,30,0,1)
                // use the direction of the dash so that players can't spin around and use the long attack omnidirectionally
                .setOrigin(forward ? executor.getChestLocation().setDirection(executor.getDashDirection()) : null)
                .execute(executor));
    }
}
