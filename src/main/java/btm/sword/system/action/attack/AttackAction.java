package btm.sword.system.action.attack;

import static btm.sword.system.action.attack.PunchAction.throwPunch;

import btm.sword.system.item.ItemUsageManager;
import btm.sword.utility.misc.ConsumerToConsumePair;

import org.bukkit.inventory.ItemStack;

import btm.sword.config.Config;
import btm.sword.system.action.SwordAction;
import btm.sword.system.attack.Attack;
import btm.sword.system.attack.style.AttackProfile;
import btm.sword.system.attack.style.AttackType;
import btm.sword.system.attack.style.WeaponAttackStyle;
import btm.sword.system.entity.types.Combatant;
import btm.sword.system.entity.types.SwordPlayer;


/**
 * Provides attack-related actions for {@link Combatant} entities.
 * <p>
 * Supports basic melee attacks, including grounded and aerial variations.
 * Handles attack execution, hit detection, damage application, particle effects,
 * knockback, and associated cooldowns.
 */
public class AttackAction extends SwordAction {
    /**
     * Executes a basic attack for the given {@link Combatant} and {@link AttackType}.
     * <p>
     * Selects the correct attack variant based on the item in hand and whether the
     * executor is grounded or airborne. Aerial attacks reset the executor's combo tree.
     *
     * @param executor The combatant performing the attack.
     */
    public static void basicAttack(Combatant executor, int comboStep) {
        ItemStack itemUsedInAttack = executor.getItemStackInHand(true);
        WeaponAttackStyle weaponAttackStyle = WeaponAttackStyle.fromString(itemUsedInAttack);

        if (weaponAttackStyle.equals(WeaponAttackStyle.PUNCH)) { // catch any untagged items and perform a punch with it
            throwPunch(executor, comboStep == 1 || comboStep == 3 ,-1);
            return;
        }

        double dot = executor.dir().dot(Config.Direction.UP());

        if (executor.isGrounded()) {
            switch (weaponAttackStyle) {
                case SLASH -> basicSlash(executor, itemUsedInAttack, weaponAttackStyle.attacks().get(comboStep - 1), true);
            }
        }
        else {
            ((SwordPlayer) executor).resetTree(); // can't combo aerials

            AttackProfile attacktype;
            double downAirThreshold = Config.Combat.ATTACKS_DOWN_AIR_THRESHOLD;
            if (dot < downAirThreshold) {
                attacktype = weaponAttackStyle.downAir();
            }
            else {
                attacktype = weaponAttackStyle.neutralAir();
            }

            if (attacktype == null) {
                throwPunch(executor, true, -1);
                return;
            }

            switch (weaponAttackStyle) {
                case SLASH -> basicSlash(executor, itemUsedInAttack, attacktype, false);
            }
        }
    }

    public static void basicSlash(Combatant executor, ItemStack itemUsedInAttack, AttackProfile profile, Boolean orientWithPitch) {
        new Attack(itemUsedInAttack, profile,
            orientWithPitch, 40,
            60, 0.1, 0.9)
            .setAttackConnectInstructions(
                new ConsumerToConsumePair<>(
                    itemStack -> ItemUsageManager.damageItemStack(itemStack, 20, executor.self()),
                    itemUsedInAttack
                )
            )
            .execute(executor);
    }

    // basic Thrust, and Bash coming later
}
