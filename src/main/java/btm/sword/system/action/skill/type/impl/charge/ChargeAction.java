package btm.sword.system.action.skill.type.impl.charge;

import btm.sword.system.action.skill.Skill;
import btm.sword.system.action.skill.SkillId;
import btm.sword.system.action.skill.SkillRegistry;
import btm.sword.system.control.PredicateRunnablePair;
import btm.sword.system.control.TimeArbiter;

import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import btm.sword.Sword;
import btm.sword.system.action.skill.container.SkillSlot;
import btm.sword.system.action.throwing.ThrowAction;
import btm.sword.system.entity.impl.Combatant;
import btm.sword.system.entity.impl.SwordPlayer;
import btm.sword.system.item.AbilityItemBuilder;
import btm.sword.system.item.SwordItemType;
import btm.sword.utility.Debug;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Static utility that manages the charge lifecycle for {@link ChargeableAbility} skills.
 *
 * <p>Flow:
 * <ol>
 *   <li>{@link #startCharge(Combatant)} — spawns an {@link ItemDisplay}, starts tick loop.</li>
 *   <li>{@link #releaseCharge(Combatant)} — fires the projectile via
 *       {@link ThrowAction#throwDirect}.</li>
 *   <li>{@link #cancelCharge(Combatant)} — disposes the display without firing.</li>
 * </ol>
 *
 * <p>All visual behavior (scaling, rotation, particles) is delegated to
 * {@link ChargeableAbility#onChargeTick(ChargeSession, long)}.</p>
 */
public final class ChargeAction {

    /** Distance in front of the player's eye to hold the charging display. */
    private static final double HOLD_DISTANCE = 4.5;

    private ChargeAction() { }

    /**
     * Begins charging the equipped chargeable ability. Resolves the ability from the held slot,
     * spawns a display entity, and starts the per-tick loop.
     *
     * @param executor the combatant starting the charge (must be a {@link SwordPlayer})
     */
    public static void startCharge(Combatant executor) {
        if (!(executor instanceof SwordPlayer sp)) return;

        ChargeableAbility chargeable = resolveChargeable(sp);
        if (chargeable == null) return;

        int heldSlot = sp.player().getInventory().getHeldItemSlot();

        // Cancel any existing charge
        cancelCharge(executor);

        // Spawn display in front of the player
        Location spawnLoc = sp.self().getEyeLocation()
            .add(sp.self().getEyeLocation().getDirection().multiply(HOLD_DISTANCE));
        ItemDisplay display = (ItemDisplay) spawnLoc.getWorld()
            .spawnEntity(spawnLoc, EntityType.ITEM_DISPLAY);
        display.setItemStack(chargeable.buildWorldItem());

        ChargeSession session = new ChargeSession(chargeable, display, heldSlot);
        sp.setActiveCharge(session);

        Debug.debug("Charge started: " + chargeable.id().asString());

        AtomicInteger it = new AtomicInteger(0);
        // Per-tick loop: follow player + delegate visuals to ability
        session.setTickTask(
            TimeArbiter.runTimeBoundBukkitTaskOnTimer(
                null,
                () -> {
                    // Follow the player's eye direction
                    Location eyeLoc = sp.self().getEyeLocation();
                    Vector dir = eyeLoc.getDirection().multiply(HOLD_DISTANCE);
                    TimeArbiter.teleportDisplay(display, eyeLoc, dir, 3, ChargeAction.class, 83);

                    // Delegate all visual behavior to the ability
                    long elapsed = System.currentTimeMillis() - session.getStartTimeMs();
                    chargeable.onChargeTick(session, elapsed);
                },
                null,
                0,50,
                ChargeAction.class, "startCharge",
                new PredicateRunnablePair(
                    () -> sp.isDead() || display.isDead() ||
                        sp.getActiveCharge() != session ||
                        (it.incrementAndGet() > 3 && !sp.player().isBlocking()),
                    () -> {
                        if (it.get() > 6) releaseCharge(sp);
                        else cancelCharge(sp);
                    }
                )
            )

        );
    }

    /**
     * Releases the charge, firing the projectile at the ability's release scale.
     * If the ability says the charge can't be released yet, cancels instead.
     *
     * @param executor the combatant releasing the charge
     */
    public static void releaseCharge(Combatant executor) {
        if (!(executor instanceof SwordPlayer sp)) return;

        ChargeSession session = sp.getActiveCharge();
        if (session == null) return;

        ChargeableAbility ability = session.getAbility();

        if (!ability.canRelease(session)) {
            Debug.debug("Charge cancelled — ability says not ready");
            cancelCharge(executor);
            return;
        }

        Debug.debug("Charge released");

        // Build the projectile item and tag it
        ItemStack projectile = ability.buildWorldItem();
        AbilityItemBuilder.tag(projectile, ability.id());

        float releaseScale = ability.releaseDisplayScale(session);
        int slotIndex = session.getSlotIndex();

        // Clean up the charge display
        session.dispose();
        sp.setActiveCharge(null);

        // Fire via throwDirect with the ability's release scale
        ThrowAction.throwDirect(executor, projectile, releaseScale, ability.projectileVelocity());

        // Consume a use from the ability slot
        sp.getAbilitySlotManager().consumeUse(slotIndex);
    }

    /**
     * Cancels an active charge without firing.
     *
     * @param executor the combatant whose charge to cancel
     */
    public static void cancelCharge(Combatant executor) {
        if (!(executor instanceof SwordPlayer sp)) return;

        ChargeSession session = sp.getActiveCharge();
        if (session == null) return;

        session.dispose();
        sp.setActiveCharge(null);
    }

    /**
     * Returns {@code true} if the given player is holding a chargeable ability item
     * in the currently selected hotbar slot.
     *
     * @param sp the player to check
     * @return {@code true} if the held ability is chargeable
     */
    public static boolean isHoldingChargeable(SwordPlayer sp) {
        return resolveChargeable(sp) != null;
    }

    /**
     * Resolves the {@link ChargeableAbility} from the player's currently held slot,
     * or {@code null} if not holding one.
     */
    private static ChargeableAbility resolveChargeable(SwordPlayer sp) {
        int heldSlot = sp.player().getInventory().getHeldItemSlot();
        SwordItemType itemType = sp.getAbilitySlotManager().getActiveTypeForHeldSlot(heldSlot);
        if (itemType == null) return null;

        SkillSlot slot = itemType == SwordItemType.ACTIVE_1 ? SkillSlot.ACTIVE_1 : SkillSlot.ACTIVE_2;
        SkillId equippedId = sp.getCombatProfile().getPlayerSkillContainer().getEquipped(slot);
        if (equippedId == null) return null;

        Skill skill = SkillRegistry.get(equippedId);
        return skill instanceof ChargeableAbility c ? c : null;
    }
}
