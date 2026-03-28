package btm.sword.system.action.attack;

import org.bukkit.inventory.ItemStack;

import btm.sword.system.action.SwordAction;
import btm.sword.system.action.movement.DashDirection;
import btm.sword.system.attack.Attack;
import btm.sword.system.attack.style.AttackProfile;
import btm.sword.system.attack.style.WeaponAttackStyle;
import btm.sword.system.entity.impl.Combatant;
import btm.sword.system.entity.impl.SwordPlayer;
import btm.sword.system.entity.umbral.input.BladeRequest;
import btm.sword.system.entity.umbral.statemachine.state.StandbyState;

/**
 * Resolves and fires the appropriate attack for a dash-momentum hit.
 *
 * <p>Selects between a punch, a directional blade attack (if the umbral blade is in standby
 * with sufficient soulfire), or a full weapon-style forward/backward dash attack based on
 * the executor's current equipment and state.</p>
 */
public class DashAttackAction extends SwordAction {

    /**
     * Fires a dash-momentum attack for {@code executor} in the given {@code direction}.
     *
     * <p>Resolution order:
     * <ol>
     *   <li>Untagged / PUNCH-style items → {@link PunchAction#throwPunch}</li>
     *   <li>Soul-link held + blade in Standby + enough soulfire → blade quick-attack</li>
     *   <li>Otherwise → direction-keyed {@link btm.sword.system.attack.style.AttackProfile} from
     *       the weapon's {@link btm.sword.system.attack.style.WeaponAttackStyle}</li>
     * </ol>
     * </p>
     *
     * @param executor  the combatant performing the attack
     * @param direction the dash direction ({@link DashDirection#FORWARD} or
     *                  {@link DashDirection#BACKWARD})
     */
    public static void dashAttack(Combatant executor, DashDirection direction) {
        ItemStack itemUsedInAttack = executor.getItemStackInHand(true);
        WeaponAttackStyle weaponAttackStyle = WeaponAttackStyle.fromString(itemUsedInAttack);

        boolean forward = direction.equals(DashDirection.FORWARD);

        executor.applyAttackCooldown();

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
            default -> weaponAttackStyle.attacks().getFirst();
        };

        new Attack(itemUsedInAttack, attackDir, !direction.equals(DashDirection.BACKWARD),
            50,30,0,1)
            // use the direction of the dash so that players can't spin around and use the long attack omnidirectionally
            .setOrigin(forward ? executor.getChestLocation().setDirection(executor.getDashDirection()) : null)
            .execute(executor);
    }
}
