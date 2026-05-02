package btm.sword.item.special;

import java.util.List;
import java.util.Set;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import btm.sword.action.skill.AbilityUseType;
import btm.sword.action.skill.Skill;
import btm.sword.action.skill.SkillId;
import btm.sword.action.skill.SkillIds;
import btm.sword.action.skill.SkillRegistry;
import btm.sword.action.skill.container.PlayerSkillContainer;
import btm.sword.action.skill.container.SkillSlot;
import btm.sword.action.skill.container.SkillSlotState;
import btm.sword.action.skill.type.AbilitySkill;
import btm.sword.entity.player.SwordPlayer;
import btm.sword.item.core.SwordItemType;

/**
 * Per-player manager that owns two {@link AbilitySlotItem}s for hotbar slots 1 and 2.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>{@link #initialize()} — reads equipped state from {@link PlayerSkillContainer}
 *       and builds the correct items for each slot.</li>
 *   <li>{@link #refresh(int)} — rebuilds a single slot (called after equip/unequip
 *       from the CharacterMenu).</li>
 *   <li>{@link #consumeUse(int)} — applies all active {@link AbilityUseType}s for the
 *       equipped ability, potentially depleting the slot when any finite resource hits zero.</li>
 *   <li>{@link #deplete(int)} — replaces the slot item with the DEPLETED placeholder.</li>
 * </ul>
 *
 * <p>Resource tracking is non-exclusive: an ability may combine {@link AbilityUseType#STACK},
 * {@link AbilityUseType#DURABILITY}, and {@link AbilityUseType#COOLDOWN} simultaneously.
 * Each is tracked and applied independently; the slot depletes when <em>any</em> finite
 * resource reaches zero.</p>
 *
 * <p>Both {@link AbilitySlotItem} instances should be added to the player's
 * {@code managedItems} list for periodic upkeep.</p>
 */
public class AbilitySlotManager {

    /** Hotbar slot index for ACTIVE_1. */
    public static final int SLOT_1 = 1;

    /** Hotbar slot index for ACTIVE_2. */
    public static final int SLOT_2 = 2;

    private final SwordPlayer owner;
    private final AbilitySlotItem slot1;
    private final AbilitySlotItem slot2;

    /**
     * Whether each ability slot currently has an EQUIPPED (usable) ability.
     * Index 0 = SLOT_1, index 1 = SLOT_2. Kept in sync after every state change.
     */
    private final boolean[] slotEnabled = {false, false};

    /**
     * Constructs the manager with default LOCKED placeholder items.
     *
     * @param owner the player who owns these ability slots
     */
    public AbilitySlotManager(SwordPlayer owner) {
        this.owner = owner;
        this.slot1 = new AbilitySlotItem(SLOT_1, SwordItemType.ACTIVE_1, AbilitySlotItem.SlotState.LOCKED);
        this.slot2 = new AbilitySlotItem(SLOT_2, SwordItemType.ACTIVE_2, AbilitySlotItem.SlotState.LOCKED);
    }

    /**
     * Reads the player's equipped skills and builds the correct items for both slots.
     * Should be called after construction and after the player's combat profile is loaded.
     */
    public void initialize() {
        PlayerSkillContainer container = owner.getCombatProfile().getPlayerSkillContainer();
        SkillSlotState state1 = container.getSlotState(SkillSlot.ACTIVE_1);
        SkillSlotState state2 = container.getSlotState(SkillSlot.ACTIVE_2);
        refreshSlot(slot1, SkillSlot.ACTIVE_1, state1.remainingUses(), state1.remainingDurability());
        refreshSlot(slot2, SkillSlot.ACTIVE_2, state2.remainingUses(), state2.remainingDurability());
        syncEnabled();
        restore(owner.player());
    }

    /**
     * Rebuilds a single slot after equip/unequip. Resets both resource counters to max.
     *
     * @param slotIndex 1 or 2
     */
    @SuppressWarnings("deprecation") // updateInventory is deprecated but functionally necessary here
    public void refresh(int slotIndex) {
        SkillSlot skillSlot = toSkillSlot(slotIndex);
        // Reset slot state — new equip starts fresh
        owner.getCombatProfile().getPlayerSkillContainer()
            .setSlotState(skillSlot, SkillSlotState.EMPTY);
        if (slotIndex == SLOT_1) {
            refreshSlot(slot1, SkillSlot.ACTIVE_1, -1, -1);
            slot1.restore(owner.player());
        } else {
            refreshSlot(slot2, SkillSlot.ACTIVE_2, -1, -1);
            slot2.restore(owner.player());
        }
        syncEnabled();
        // Force client sync — without this, equipping while the slot is selected
        // or while a menu is open may not visually register until the next tick.
        owner.player().updateInventory();
    }

    /**
     * Consumes one activation from the ability in the given slot.
     * All active {@link AbilityUseType}s are applied independently:
     * STACK decrements the use count, DURABILITY degrades the bar, COOLDOWN applies an
     * item-cooldown overlay. The slot is depleted if any finite resource reaches zero.
     *
     * @param slotIndex 1 or 2
     */
    public void consumeUse(int slotIndex) {
        AbilitySkill ability = resolveAbility(slotIndex == SLOT_1 ? SkillSlot.ACTIVE_1 : SkillSlot.ACTIVE_2);
        if (ability == null) return;

        Player player = owner.player();
        Set<AbilityUseType> types = ability.useTypes();

        if (types.contains(AbilityUseType.STACK)) {
            int remaining = getRemainingUses(slotIndex);
            if (remaining > 0) {
                remaining--;
                setRemainingUses(slotIndex, remaining);
                if (remaining <= 0) {
                    deplete(slotIndex);
                    return;
                }
            }
        }

        if (types.contains(AbilityUseType.DURABILITY)) {
            int remaining = getRemainingDurability(slotIndex);
            if (remaining > 0) {
                remaining--;
                setRemainingDurability(slotIndex, remaining);
                if (remaining <= 0) {
                    deplete(slotIndex);
                    return;
                }
            }
        }

        if (types.contains(AbilityUseType.COOLDOWN) && ability.cooldownTicks() > 0) {
            ItemStack current = player.getInventory().getItem(slotIndex);
            if (current != null) {
                player.setCooldown(current.getType(), ability.cooldownTicks());
            }
        }

        restoreSlot(slotIndex);
    }

    /**
     * Replaces the slot item with the DEPLETED placeholder.
     *
     * @param slotIndex 1 or 2
     */
    public void deplete(int slotIndex) {
        AbilitySlotItem item = slotIndex == SLOT_1 ? slot1 : slot2;
        item.setState(AbilitySlotItem.SlotState.DEPLETED);
        setRemainingUses(slotIndex, 0);
        setRemainingDurability(slotIndex, 0);
        item.restore(owner.player());
        syncEnabled();
    }

    /**
     * Returns {@code true} if the given slot has an actual ability equipped (not locked/empty/depleted).
     *
     * @param slotIndex 1 or 2
     * @return {@code true} if equipped with a usable ability
     */
    public boolean isEquipped(int slotIndex) {
        AbilitySlotItem item = slotIndex == SLOT_1 ? slot1 : slot2;
        return item.getState() == AbilitySlotItem.SlotState.EQUIPPED;
    }

    /**
     * Adds one use back to the given slot (e.g. on projectile pickup).
     * If the slot was depleted, transitions it back to EQUIPPED. No-ops if the slot has no
     * {@link AbilityUseType#STACK} ability or is already at max uses.
     *
     * @param slotIndex 1 or 2
     */
    @SuppressWarnings("deprecation") // updateInventory is deprecated but functionally necessary here
    public void addUse(int slotIndex) {
        SkillSlot skillSlot = toSkillSlot(slotIndex);
        AbilitySkill ability = resolveAbility(skillSlot);
        if (ability == null || !ability.useTypes().contains(AbilityUseType.STACK)) return;

        int current = getRemainingUses(slotIndex);
        int max = ability.maxUses();
        if (current >= max) return;

        int newUses = current < 0 ? 1 : Math.min(current + 1, max);
        setRemainingUses(slotIndex, newUses);

        AbilitySlotItem slotItem = slotIndex == SLOT_1 ? slot1 : slot2;
        refreshSlot(slotItem, skillSlot, newUses, getRemainingDurability(slotIndex));
        slotItem.restore(owner.player());
        syncEnabled();
        owner.player().updateInventory();
    }

    /**
     * Looks up which active slot (if any) has the given ability equipped, then
     * calls {@link #addUse(int)} to restore one stack use. No-ops if neither
     * slot matches the ability ID.
     *
     * @param abilityIdStr the {@link SkillId#asString()} stamped on the projectile
     */
    public void refundByAbilityId(String abilityIdStr) {
        PlayerSkillContainer container = owner.getCombatProfile().getPlayerSkillContainer();
        SkillId eq1 = container.getEquipped(SkillSlot.ACTIVE_1);
        if (eq1 != null && eq1.asString().equals(abilityIdStr)) {
            addUse(SLOT_1);
            return;
        }
        SkillId eq2 = container.getEquipped(SkillSlot.ACTIVE_2);
        if (eq2 != null && eq2.asString().equals(abilityIdStr)) {
            addUse(SLOT_2);
        }
    }

    /**
     * Returns the two {@link AbilitySlotItem}s for inclusion in the managed items list.
     *
     * @return unmodifiable list of the two slot items
     */
    public List<AbilitySlotItem> getSlotItems() {
        return List.of(slot1, slot2);
    }

    /**
     * Returns the remaining stack-use count for the given slot.
     * Returns {@code -1} if STACK tracking is not active for the equipped ability.
     *
     * @param slotIndex 1 or 2
     * @return remaining use count, or {@code -1}
     */
    public int getRemainingUses(int slotIndex) {
        return owner.getCombatProfile().getPlayerSkillContainer()
            .getSlotState(toSkillSlot(slotIndex)).remainingUses();
    }

    /**
     * Sets the remaining stack-use count for the given slot, preserving other state dimensions.
     *
     * @param slotIndex 1 or 2
     * @param uses      remaining use count
     */
    public void setRemainingUses(int slotIndex, int uses) {
        SkillSlot slot = toSkillSlot(slotIndex);
        PlayerSkillContainer container = owner.getCombatProfile().getPlayerSkillContainer();
        SkillSlotState current = container.getSlotState(slot);
        container.setSlotState(slot, new SkillSlotState(uses, current.remainingDurability(), current.cooldownExpiresAt()));
    }

    /**
     * Returns the remaining durability for the given slot.
     * Returns {@code -1} if DURABILITY tracking is not active for the equipped ability.
     *
     * @param slotIndex 1 or 2
     * @return remaining durability, or {@code -1}
     */
    public int getRemainingDurability(int slotIndex) {
        return owner.getCombatProfile().getPlayerSkillContainer()
            .getSlotState(toSkillSlot(slotIndex)).remainingDurability();
    }

    /**
     * Sets the remaining durability for the given slot, preserving other state dimensions.
     *
     * @param slotIndex 1 or 2
     * @param durability remaining durability
     */
    public void setRemainingDurability(int slotIndex, int durability) {
        SkillSlot slot = toSkillSlot(slotIndex);
        PlayerSkillContainer container = owner.getCombatProfile().getPlayerSkillContainer();
        SkillSlotState current = container.getSlotState(slot);
        container.setSlotState(slot, new SkillSlotState(current.remainingUses(), durability, current.cooldownExpiresAt()));
    }

    /**
     * Restores both slot items into the player's inventory.
     *
     * @param player the player whose inventory to populate
     */
    public void restore(Player player) {
        slot1.restore(player);
        slot2.restore(player);
    }

    /**
     * Restores a single managed ability slot from persistent slot state.
     * Useful when the live inventory stack was temporarily replaced (for example,
     * by the right-click hold gunpowder placeholder).
     *
     * @param slotIndex 1 or 2
     */
    public void restoreSlot(int slotIndex) {
        syncSlotItemFromState(slotIndex);
        AbilitySlotItem slotItem = slotIndex == SLOT_1 ? slot1 : slot2;
        slotItem.restore(owner.player());
        syncEnabled();
    }

    /**
     * Returns the {@link SwordItemType} for the ability at the given hotbar slot index,
     * or {@code null} if that slot does not have an active (equipped) ability.
     * Used by {@code AbilitySlotBinding} as a reliable alternative to PDC tag lookups.
     *
     * @param heldSlot the player's currently held hotbar slot index
     * @return {@link SwordItemType#ACTIVE_1}, {@link SwordItemType#ACTIVE_2}, or {@code null}
     */
    public SwordItemType getActiveTypeForHeldSlot(int heldSlot) {
        if (heldSlot == SLOT_1 && slotEnabled[0]) return SwordItemType.ACTIVE_1;
        if (heldSlot == SLOT_2 && slotEnabled[1]) return SwordItemType.ACTIVE_2;
        return null;
    }

    /**
     * Returns {@code true} if the given hotbar slot holds an ability item in either
     * {@link AbilitySlotItem.SlotState#EQUIPPED} or {@link AbilitySlotItem.SlotState#DEPLETED} state.
     *
     * <p>Unlike {@link #getActiveTypeForHeldSlot(int)}, this includes depleted slots so that
     * dash/grab/catch pickup interactions still work when the ability's uses are exhausted.</p>
     *
     * @param heldSlot the player's currently held hotbar slot index
     * @return {@code true} if the slot is EQUIPPED or DEPLETED
     */
    public boolean isAbilityHeldSlot(int heldSlot) {
        if (heldSlot == SLOT_1) {
            AbilitySlotItem.SlotState s = slot1.getState();
            return s == AbilitySlotItem.SlotState.EQUIPPED || s == AbilitySlotItem.SlotState.DEPLETED;
        }
        if (heldSlot == SLOT_2) {
            AbilitySlotItem.SlotState s = slot2.getState();
            return s == AbilitySlotItem.SlotState.EQUIPPED || s == AbilitySlotItem.SlotState.DEPLETED;
        }
        return false;
    }

    // ── Internal ─────────────────────────────────────────────────────────────

    /** Syncs {@link #slotEnabled} from the current {@link AbilitySlotItem} states. */
    private void syncEnabled() {
        slotEnabled[0] = slot1.getState() == AbilitySlotItem.SlotState.EQUIPPED;
        slotEnabled[1] = slot2.getState() == AbilitySlotItem.SlotState.EQUIPPED;
    }

    private void syncSlotItemFromState(int slotIndex) {
        SkillSlot skillSlot = toSkillSlot(slotIndex);
        SkillSlotState state = owner.getCombatProfile().getPlayerSkillContainer().getSlotState(skillSlot);
        AbilitySlotItem slotItem = slotIndex == SLOT_1 ? slot1 : slot2;
        refreshSlot(slotItem, skillSlot, state.remainingUses(), state.remainingDurability());
    }

    private SkillSlot toSkillSlot(int slotIndex) {
        return slotIndex == SLOT_1 ? SkillSlot.ACTIVE_1 : SkillSlot.ACTIVE_2;
    }

    private void refreshSlot(AbilitySlotItem slotItem, SkillSlot skillSlot,
                              int persistedUses, int persistedDurability) {
        PlayerSkillContainer container = owner.getCombatProfile().getPlayerSkillContainer();
        SkillId equipped = container.getEquipped(skillSlot);

        if (equipped == null || equipped.equals(SkillIds.LOCKED)) {
            slotItem.setState(AbilitySlotItem.SlotState.LOCKED);
            return;
        }

        if (equipped.equals(SkillIds.NONE)) {
            slotItem.setState(AbilitySlotItem.SlotState.EMPTY);
            return;
        }

        Skill skill = SkillRegistry.get(equipped);
        if (!(skill instanceof AbilitySkill ability)) {
            slotItem.setState(AbilitySlotItem.SlotState.EMPTY);
            return;
        }

        Set<AbilityUseType> types = ability.useTypes();
        int slotIndex = slotItem == slot1 ? SLOT_1 : SLOT_2;

        // Resolve and store remaining uses
        int uses = resolveUses(ability, persistedUses);
        setRemainingUses(slotIndex, uses);

        // Resolve and store remaining durability
        int durability = resolveDurability(ability, persistedDurability);
        setRemainingDurability(slotIndex, durability);

        // Depleted if any finite resource is exhausted
        if (types.contains(AbilityUseType.STACK) && uses == 0) {
            slotItem.setState(AbilitySlotItem.SlotState.DEPLETED);
            return;
        }
        if (types.contains(AbilityUseType.DURABILITY) && durability == 0) {
            slotItem.setState(AbilitySlotItem.SlotState.DEPLETED);
            return;
        }

        // Build the equipped world item, applying visual resource states
        ItemStack worldItem = ability.buildWorldItem();
        if (types.contains(AbilityUseType.STACK)) {
            worldItem.setAmount(Math.max(1, uses));
        }
        if (types.contains(AbilityUseType.DURABILITY)) {
            updateDurabilityDisplay(worldItem, durability, ability.maxDurability());
        }

        slotItem.setEquipped(worldItem);
    }

    private int resolveUses(AbilitySkill ability, int persisted) {
        if (!ability.useTypes().contains(AbilityUseType.STACK)) return -1;
        return persisted >= 0 ? persisted : ability.maxUses();
    }

    private int resolveDurability(AbilitySkill ability, int persisted) {
        if (!ability.useTypes().contains(AbilityUseType.DURABILITY)) return -1;
        return persisted >= 0 ? persisted : ability.maxDurability();
    }

    private AbilitySkill resolveAbility(SkillSlot skillSlot) {
        PlayerSkillContainer container = owner.getCombatProfile().getPlayerSkillContainer();
        SkillId equipped = container.getEquipped(skillSlot);
        if (equipped == null || equipped.equals(SkillIds.LOCKED) || equipped.equals(SkillIds.NONE)) {
            return null;
        }
        Skill skill = SkillRegistry.get(equipped);
        return skill instanceof AbilitySkill ability ? ability : null;
    }

    private void updateDurabilityDisplay(ItemStack item, int remaining, int max) {
        if (item.getItemMeta() instanceof org.bukkit.inventory.meta.Damageable damageable) {
            int maxDurability = item.getType().getMaxDurability();
            if (maxDurability > 0 && max > 0) {
                int damage = (int) ((1.0 - (double) remaining / max) * maxDurability);
                damageable.setDamage(Math.min(damage, maxDurability - 1));
                item.setItemMeta(damageable);
            }
        }
    }
}
