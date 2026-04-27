package btm.sword.system.action.attack;

import static btm.sword.system.action.attack.PunchAction.throwPunch;

import org.bukkit.inventory.ItemStack;

import btm.sword.config.Config;
import btm.sword.system.action.SwordAction;
import btm.sword.system.attack.SweepAttack;
import btm.sword.system.attack.def.AttackInstance;
import btm.sword.system.attack.dev.AttackDevSession;
import btm.sword.system.attack.dev.DevMode;
import btm.sword.system.attack.dev.VolumeEditorMode;
import btm.sword.system.attack.style.AttackProfile;
import btm.sword.system.attack.style.AttackType;
import btm.sword.system.attack.style.WeaponAttackStyle;
import btm.sword.system.entity.aspect.AspectType;
import btm.sword.system.entity.impl.Combatant;
import btm.sword.system.entity.impl.SwordPlayer;
import btm.sword.system.inventory.menu.dev.SweepGeneratorMenu;
import btm.sword.system.item.ItemUsageManager;
import btm.sword.system.item.KeyRegistry;
import btm.sword.utility.Debug;
import btm.sword.utility.misc.ConsumerToConsumePair;


/**
 * Provides attack-related actions for {@link Combatant} entities.
 * <p>
 * Supports basic melee attacks, including grounded and aerial variations.
 * Handles attack execution, hit detection, damage application, particle effects,
 * knockback, and associated cooldowns.
 */
public class AttackAction extends SwordAction {

    /** Creates an {@code AttackAction}. */
    public AttackAction() {}

    /**
     * Executes a basic attack for the given {@link Combatant} and {@link AttackType}.
     * <p>
     * Selects the correct attack variant based on the item in hand and whether the
     * executor is grounded or airborne. Aerial attacks reset the executor's combo tree.
     *
     * @param executor  the combatant performing the attack
     * @param comboStep the current step in the combo sequence (1-indexed), used to select
     *                  the appropriate attack profile and alternating punch side
     */
    public static void basicAttack(Combatant executor, int comboStep) {
        ItemStack itemUsedInAttack = executor.getItemStackInHand(true);

        if (KeyRegistry.hasKey(itemUsedInAttack, KeyRegistry.TEST_VOLUME_ATTACK_KEY)) {
            if (executor instanceof SwordPlayer sp) {
                AttackDevSession devSession = AttackDevSession.get(sp.player().getUniqueId());
                Debug.attackVolume("[AttackAction] wand fired by " + sp.player().getName()
                    + " — devSession=" + (devSession == null ? "null" : devSession.getMode())
                    + " loadedAttack=" + (devSession == null || devSession.getLoadedAttackInstance() == null
                    ? "null" : devSession.getLoadedAttackInstance().getId())
                    + " editingSessions=" + AttackDevSession.getEditingSessions().size());
                if (devSession != null && devSession.getMode() == DevMode.EDITING) {
                    Debug.attackVolume("[AttackAction]   keyframes=" + devSession.getEditKeyframes().size()
                        + " duration=" + devSession.getEditDurationMs() + "ms");
                }
                if (devSession != null && devSession.getMode() == DevMode.RECORDING) {
                    new SweepGeneratorMenu(sp).open();
                    return;
                }
                if (devSession != null && devSession.getLoadedAttackInstance() != null) {
                    fireWandDef(executor, devSession.getLoadedAttackInstance());
                    return;
                }
            }
            if (executor instanceof SwordPlayer sp2) {
                sp2.message(net.kyori.adventure.text.Component.text(
                    "[Dev] No attack loaded — open the browser and select one.",
                    net.kyori.adventure.text.format.NamedTextColor.YELLOW));
            }
            return;
        }

        WeaponAttackStyle weaponAttackStyle = WeaponAttackStyle.fromString(itemUsedInAttack);

        if (weaponAttackStyle.equals(WeaponAttackStyle.PUNCH)) { // catch any untagged items and perform a punch with it
            throwPunch(executor, comboStep == 1 || comboStep == 3, -1);
            return;
        }

        double dot = executor.dir().dot(Config.Direction.up());

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

    /** Executes a basic sweep slash attack using the provided profile and item. */
    public static void basicSlash(Combatant executor, ItemStack itemUsedInAttack, AttackProfile profile, Boolean orientWithPitch) {
        new SweepAttack(itemUsedInAttack, profile, orientWithPitch)
            .setAttackDuration(attacker -> (int) attacker.calcValueReductive(
                AspectType.CELERITY,
                Config.Combat.ATTACKS_CAST_TIMING_MIN_DURATION,
                Config.Combat.ATTACKS_CAST_TIMING_MAX_DURATION,
                Config.Combat.ATTACKS_CAST_TIMING_REDUCTION_RATE))
            .setAttackConnectInstructions(
                new ConsumerToConsumePair<>(
                    itemStack -> ItemUsageManager.damageItemStack(itemStack, 20, executor.self()),
                    itemUsedInAttack
                )
            )
            .execute(executor);
    }

    private static void fireWandDef(Combatant executor, AttackInstance def) {
        long startMs = System.currentTimeMillis();
        executor.launchAttack(def);
        if (executor instanceof SwordPlayer sp && Config.Debug.VISUALIZATION_SHOW_HITBOXES) {
            VolumeEditorMode.startPlaybackVisualization(
                sp.player(), def.getTrajectory(), startMs, def.getDurationMs(),
                def.isLockOriginOnFire(), def.isOrientWithPitch());
        }
    }

    // basic Thrust, and Bash coming later
}
