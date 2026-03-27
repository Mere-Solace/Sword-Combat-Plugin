package btm.sword.system.action.skill.container;

import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import btm.sword.system.action.skill.Skill;
import btm.sword.system.action.skill.SkillId;
import btm.sword.system.action.skill.SkillIds;
import btm.sword.system.action.skill.SkillRegistry;
import btm.sword.system.action.skill.SkillType;


public final class PlayerSkillContainer {

    private final EnumMap<SkillType, Set<SkillId>> availableSkillsMap = new EnumMap<>(SkillType.class);

    /**
     * Tracks the availability state of every skill the player has ever discovered.
     * A skill is only shown in menus if it appears in this map. Its state governs
     * whether it can be equipped ({@link SkillAvailability#AVAILABLE}) or only viewed
     * ({@link SkillAvailability#DEPLETED}, {@link SkillAvailability#RELINQUISHED}).
     */
    private final Map<SkillId, SkillAvailability> availabilityMap = new HashMap<>();

    private final EnumMap<SkillSlot, SkillId> equipped;
    private final Map<SkillSlot, SkillSlotState> slotStates = new EnumMap<>(SkillSlot.class);

    public PlayerSkillContainer(Collection<SkillId> availableSkills, EnumMap<SkillSlot, SkillId> equipped) {
        initializeSkillMap();
        addAvailableSkills(availableSkills);
        this.equipped = equipped;
    }

    /**
     * Constructs a container from persisted data that includes per-skill availability states.
     * Used exclusively by the persistence layer.
     *
     * @param skillAvailabilities the persisted map of skill IDs to their saved availability state
     * @param equipped            the persisted equipped-slot map
     */
    public PlayerSkillContainer(Map<SkillId, SkillAvailability> skillAvailabilities,
            EnumMap<SkillSlot, SkillId> equipped) {
        initializeSkillMap();
        for (Map.Entry<SkillId, SkillAvailability> entry : skillAvailabilities.entrySet()) {
            SkillId id = entry.getKey();
            if (id.equals(SkillIds.NONE) || id.equals(SkillIds.LOCKED)) continue;
            Skill skill = SkillRegistry.get(id);
            if (skill == null) continue;
            availabilityMap.put(id, entry.getValue());
            availableSkillsMap.get(skill.type()).add(id);
        }
        this.equipped = equipped;
    }

    public PlayerSkillContainer() { // for testing purposes
        initializeSkillMap();
        addAvailableSkills(SkillIds.getAll());
        this.equipped = new EnumMap<>(SkillSlot.class);
        equipped.putAll(Map.of(
            SkillSlot.UMBRAL_1, SkillIds.SHADOW_SLASH,
            SkillSlot.UMBRAL_2, SkillIds.VOID_LUNGE,
            SkillSlot.UMBRAL_3, SkillIds.LOCKED,
            SkillSlot.ACTIVE_1, SkillIds.KNIFE_THROW,
            SkillSlot.ACTIVE_2, SkillIds.LOCKED,
            SkillSlot.PASSIVE_1, SkillIds.NONE,
            SkillSlot.PASSIVE_2, SkillIds.NONE,
            SkillSlot.PASSIVE_3, SkillIds.LOCKED,
            SkillSlot.PASSIVE_CORE, SkillIds.NONE)
        );
    }

    public void initializeSkillMap() {
        availableSkillsMap.put(SkillType.UMBRAL, new HashSet<>());
        availableSkillsMap.put(SkillType.ACTIVE, new HashSet<>());
        availableSkillsMap.put(SkillType.PASSIVE, new HashSet<>());
    }

    public void addAvailableSkills(Collection<SkillId> skillIds) {
        for (SkillId id : skillIds) {
            discover(id);
        }
    }

    /**
     * Adds a skill to this player's discovered pool as {@link SkillAvailability#AVAILABLE}.
     * If the skill has already been discovered, its existing state is not changed.
     *
     * @param id the skill to discover; NONE and LOCKED are silently ignored
     */
    public void discover(SkillId id) {
        if (id.equals(SkillIds.NONE) || id.equals(SkillIds.LOCKED)) return;
        Skill skill = SkillRegistry.get(id);
        if (skill == null) return;
        if (availabilityMap.containsKey(id)) return;
        availabilityMap.put(id, SkillAvailability.AVAILABLE);
        availableSkillsMap.get(skill.type()).add(id);
    }

    /**
     * Updates the availability state of an already-discovered skill.
     * Has no effect if the skill has not been discovered yet.
     *
     * @param id           the skill whose state to update
     * @param availability the new state
     */
    public void setAvailability(SkillId id, SkillAvailability availability) {
        if (availabilityMap.containsKey(id)) {
            availabilityMap.put(id, availability);
        }
    }

    /**
     * Returns the current availability state of a skill, or {@code null} if it
     * has not been discovered by this player yet.
     *
     * @param id the skill to query
     * @return the availability state, or {@code null}
     */
    public SkillAvailability getAvailability(SkillId id) {
        return availabilityMap.get(id);
    }

    /**
     * Returns {@code true} if the skill has been discovered and is currently
     * {@link SkillAvailability#AVAILABLE} (i.e. can be equipped).
     *
     * @param id the skill to check
     * @return {@code true} if equippable
     */
    public boolean isEquippable(SkillId id) {
        return availabilityMap.get(id) == SkillAvailability.AVAILABLE;
    }

    public void unlock(SkillSlot slot) {
        if (!equipped.get(slot).equals(SkillIds.LOCKED)) {
            return;
        }
        equipped.put(slot, SkillIds.NONE);
    }

    public boolean isAvailable(SkillId id) {
        return availabilityMap.get(id) == SkillAvailability.AVAILABLE;
    }

    public SkillId getEquipped(SkillSlot slot) {
        SkillId id = equipped.get(slot);
        return id == null ? SkillIds.NONE : id;
    }

    /**
     * Avoid calling with None as that will generate redundant work. Instead call {@link #unequip(SkillSlot)}
     *
     * @param slot the slot to equip the id to
     * @param id the id to be equipped; must be {@link SkillAvailability#AVAILABLE}
     */
    public void equip(SkillSlot slot, SkillId id) {
        if (equipped.get(slot).equals(SkillIds.LOCKED)) return;
        if (!isEquippable(id)) return;

        equipped.put(slot, id);

        for (EnumMap.Entry<SkillSlot, SkillId> pair : equipped.entrySet()) {
            // if it was already equipped in a different slot, remove it and set that slot to none
            if (!pair.getKey().equals(slot) && pair.getValue().equals(id)) {
                equipped.remove(pair.getKey());
                equipped.put(pair.getKey(), SkillIds.NONE); // set to none
                return; // found it, duplicates are not allowed
            }
        }
    }

    public void unequip(SkillSlot slot) {
        if (equipped.get(slot).equals(SkillIds.LOCKED)) {
            return;
        }

        equipped.put(slot, SkillIds.NONE);
    }

    /**
     * Returns discovered skills of the given type that are {@link SkillAvailability#AVAILABLE}
     * and not currently equipped in any slot. These are the skills the player can select from
     * in the equip menu.
     *
     * @param type the skill type to filter by
     * @return equippable, unequipped skills of that type
     */
    public List<SkillId> freeSkillIds(SkillType type) {
        Set<SkillId> allSkillsOfType = new HashSet<>(availableSkillsMap.get(type));
        allSkillsOfType.removeAll(equipped.values());
        return allSkillsOfType.stream()
            .filter(id -> availabilityMap.get(id) == SkillAvailability.AVAILABLE)
            .toList();
    }

    /**
     * Returns discovered skills of the given type that are {@link SkillAvailability#DEPLETED}
     * or {@link SkillAvailability#RELINQUISHED} and not currently equipped. These are shown in
     * the menu as locked — visible but not selectable.
     *
     * @param type the skill type to filter by
     * @return discovered but locked skills of that type
     */
    public List<SkillId> lockedSkillIds(SkillType type) {
        Set<SkillId> allSkillsOfType = new HashSet<>(availableSkillsMap.get(type));
        allSkillsOfType.removeAll(equipped.values());
        return allSkillsOfType.stream()
            .filter(id -> {
                SkillAvailability a = availabilityMap.get(id);
                return a == SkillAvailability.DEPLETED || a == SkillAvailability.RELINQUISHED;
            })
            .toList();
    }

    /**
     * Returns the persisted resource state for the given slot, or {@link SkillSlotState#EMPTY}
     * if no state has been recorded yet.
     *
     * @param slot the ability slot to query
     * @return the current slot state; never {@code null}
     */
    public SkillSlotState getSlotState(SkillSlot slot) {
        return slotStates.getOrDefault(slot, SkillSlotState.EMPTY);
    }

    /**
     * Stores the resource state for the given slot.
     *
     * @param slot  the ability slot to update
     * @param state the new state; must not be {@code null}
     */
    public void setSlotState(SkillSlot slot, SkillSlotState state) {
        slotStates.put(slot, state);
    }

    /**
     * Returns all slot states for persistence.
     * Only slots with explicitly recorded state are included (EMPTY slots are omitted).
     *
     * @return an unmodifiable copy of the slot-state map
     */
    public Map<SkillSlot, SkillSlotState> allSlotStates() {
        return Map.copyOf(slotStates);
    }

    public Map<SkillSlot, SkillId> equippedView() {
        return Map.copyOf(equipped);
    }

    /**
     * Returns all discovered skills and their current availability states.
     * Used by the persistence layer to save the full discovered pool with states.
     *
     * @return an unmodifiable view of the availability map
     */
    public Map<SkillId, SkillAvailability> allDiscoveredSkillAvailabilities() {
        return Map.copyOf(availabilityMap);
    }
}
