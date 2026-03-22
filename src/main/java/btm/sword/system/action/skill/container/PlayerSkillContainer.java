package btm.sword.system.action.skill.container;

import java.util.Collection;
import java.util.EnumMap;
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
    private final EnumMap<SkillSlot, SkillId> equipped;

    public PlayerSkillContainer(Collection<SkillId> availableSkills, EnumMap<SkillSlot, SkillId> equipped) {
        initializeSkillMap();
        addAvailableSkills(availableSkills);
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
            if (id.equals(SkillIds.NONE) || id.equals(SkillIds.LOCKED)) {
                continue;
            }
            Skill skill = SkillRegistry.get(id);
            if (skill == null) {
                continue;
            }
            availableSkillsMap.get(skill.type()).add(id);
        }
    }

    public void unlock(SkillSlot slot) {
        if (!equipped.get(slot).equals(SkillIds.LOCKED)) {
            return;
        }
        equipped.put(slot, SkillIds.NONE);
    }

    public boolean isAvailable(SkillId id) {
        for (Map.Entry<SkillType, Set<SkillId>> entry : availableSkillsMap.entrySet()) {
            if (entry.getValue().contains(id)) {
                return true;
            }
        }
        return false;
    }

    public SkillId getEquipped(SkillSlot slot) {
        SkillId id = equipped.get(slot);
        return id == null ? SkillIds.NONE : id;
    }

    /**
     * Avoid calling with None as that will generate redundant work. Instead call {@link #unequip(SkillSlot)}
     *
     * @param slot the slot to equip the id to
     * @param id the id to be equipped
     */
    public void equip(SkillSlot slot, SkillId id) {
        if (equipped.get(slot).equals(SkillIds.LOCKED)) {
            return;
        }

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

    public List<SkillId> freeSkillIds(SkillType type) {
        Set<SkillId> allSkillsOfType = new HashSet<>(availableSkillsMap.get(type));
        allSkillsOfType.removeAll(equipped.values());
        return allSkillsOfType.stream().toList();
    }

    public Map<SkillSlot, SkillId> equippedView() {
        return Map.copyOf(equipped);
    }

    /**
     * Returns all unlocked skill IDs across every skill type as a flat list.
     * Used by the persistence layer to save the player's available skill pool.
     *
     * @return an unmodifiable flat list of all available (unlocked) skill IDs
     */
    public List<SkillId> allAvailableSkillIds() {
        return availableSkillsMap.values().stream()
            .flatMap(Set::stream)
            .toList();
    }
}
