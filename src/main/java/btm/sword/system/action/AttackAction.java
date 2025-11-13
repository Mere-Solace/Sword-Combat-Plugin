package btm.sword.system.action;

import java.util.Map;

import org.apache.logging.log4j.util.TriConsumer;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import btm.sword.config.ConfigManager;
import btm.sword.system.attack.Attack;
import btm.sword.system.attack.AttackType;
import btm.sword.system.entity.types.Combatant;
import btm.sword.system.entity.types.SwordPlayer;
import btm.sword.system.entity.umbral.statemachine.UmbralState;
import btm.sword.system.item.KeyRegistry;
import btm.sword.util.display.Prefab;

/**
 * Provides attack-related actions for {@link Combatant} entities.
 * <p>
 * Supports basic melee attacks, including grounded and aerial variations.
 * Handles attack execution, hit detection, damage application, particle effects,
 * knockback, and associated cooldowns.
 */
public class AttackAction extends SwordAction {
    /** Mapping from item suffixes to corresponding attack handlers. */
    private static final Map<String, TriConsumer<Combatant, AttackType, Boolean>> attackMap = Map.of(
            "_SWORD", AttackAction::basicSlash,
            "_SHOVEL", AttackAction::basicSlash,
            "_AXE", AttackAction::basicSlash,
            "SHIELD", AttackAction::basicSlash
    );

    /**
     * Executes a basic attack for the given {@link Combatant} and {@link AttackType}.
     * <p>
     * Selects the correct attack variant based on the item in hand and whether the
     * executor is grounded or airborne. Aerial attacks reset the executor's combo tree.
     *
     * @param executor The combatant performing the attack.
     * @param type The type of attack being performed.
     */
    public static void basicAttack(Combatant executor, AttackType type, boolean orientWithPitch) {
        ItemStack itemStack = executor.getItemStackInHand(true);
        Material itemType = itemStack.getType();

        // TODO: todo link from the umbral Blade todo in input execution tree.
        // handle potential umbral blade usage
        if (KeyRegistry.hasKey(itemStack, KeyRegistry.SOUL_LINK_KEY) &&
                executor.getUmbralBlade() != null) {
            if (executor.getUmbralBlade().inState(UmbralState.STANDBY)) {
                UmbralBladeAction.performAttack(executor);
                return;
            }

            if (!executor.getUmbralBlade().inState(UmbralState.WIELD)) {
                return;
            }
        }

        double dot = executor.entity().getEyeLocation().getDirection().dot(Prefab.Direction.UP);

        if (executor.isGrounded()) {
            for (var entry : attackMap.entrySet()) {
                if (itemType.name().endsWith(entry.getKey())) {
                    entry.getValue().accept(executor, type, orientWithPitch);
                    return;
                }
            }
        }
        else {
            ((SwordPlayer) executor).resetTree(); // can't combo aerials

            AttackType attackType = AttackType.N_AIR;
            double downAirThreshold = ConfigManager.getInstance().getCombat().getAttacks().getDownAirThreshold();
            if (dot < downAirThreshold) attackType = AttackType.D_AIR;

            for (var entry : attackMap.entrySet()) {
                if (itemType.name().endsWith(entry.getKey())) {
                    entry.getValue().accept(executor, attackType, true);
                    return;
                }
            }
        }
    }

    public static void basicSlash(Combatant executor, AttackType type, Boolean orientWithPitch) {
        new Attack(type, orientWithPitch, null).execute(executor);
    }
}
