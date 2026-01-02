package btm.sword.system.action.skill.container;

import btm.sword.system.action.skill.Skill;
import btm.sword.system.action.skill.SkillId;
import btm.sword.system.action.skill.SkillRegistry;
import btm.sword.system.action.skill.type.ActiveSkill;
import btm.sword.system.entity.base.CombatProfile;
import btm.sword.system.entity.impl.SwordPlayer;
import btm.sword.system.input.InputAction;

public final class SkillSlotActionFactory {

    public static InputAction create(SwordPlayer player, SkillSlot slot) {

        ActiveSkill resolvedSkill = resolveActiveSkill(player, slot);
        if (resolvedSkill == null) return null;

        return new InputAction(
            resolvedSkill::execute,
            resolvedSkill::calculateCooldown,
            resolvedSkill::canPerform,
            true,
            true,
            true
        ); // Can make these dynamic later
    }

    private static ActiveSkill resolveActiveSkill(SwordPlayer player, SkillSlot slot) {
        CombatProfile profile = player.getCombatProfile();
        SkillId id = profile.getPlayerSkillContainer().getEquipped(slot);
        if (id == null) return null;

        Skill skill = SkillRegistry.get(id);

        return !(skill instanceof ActiveSkill active) ? null : active;
    }
}
