package btm.sword.system.action.skill.container;

import java.util.Collection;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import btm.sword.system.action.skill.SkillId;
import btm.sword.system.action.skill.SkillIds;

public final class PlayerSkillContainer {

    private final Set<SkillId> availableSkills = new HashSet<>();
    private final EnumMap<SkillSlot, SkillId> equipped;

    public PlayerSkillContainer(Collection<SkillId> availableSkills, EnumMap<SkillSlot, SkillId> equipped) {
        this.availableSkills.addAll(availableSkills);
        this.equipped = equipped;
    }

    public PlayerSkillContainer() { // for testing purposes
        this.availableSkills.addAll(SkillIds.getAll());
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

    public boolean isUnlocked(SkillId id) {
        return availableSkills.contains(id);
    }

    public @Nullable SkillId getEquipped(SkillSlot slot) {
        return equipped.get(slot);
    }

    public void equip(SkillSlot slot, SkillId id) {
        equipped.put(slot, id);

        for (EnumMap.Entry<SkillSlot, SkillId> pair : equipped.entrySet()) {
            if (pair.getValue().equals(id)) {
                equipped.remove(pair.getKey());
                equipped.put(pair.getKey(), SkillIds.NONE); // set to none
                return; // found it, duplicates are not allowed
            }
        }
    }

    public void unequip(SkillSlot slot) {
        equipped.remove(slot);
    }

    public Map<SkillSlot, SkillId> equippedView() {
        return Map.copyOf(equipped);
    }
}
