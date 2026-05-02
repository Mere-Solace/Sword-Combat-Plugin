package btm.sword.system.action.skill.container;

import btm.sword.input.InputAction;
import btm.sword.system.action.skill.Skill;
import btm.sword.system.action.skill.SkillId;
import btm.sword.system.action.skill.SkillRegistry;
import btm.sword.system.action.skill.type.ActiveSkill;
import btm.sword.system.action.skill.type.impl.umbral.VoidLungeSkill;
import btm.sword.system.entity.base.CombatProfile;
import btm.sword.system.entity.impl.SwordPlayer;

/**
 * Factory that builds {@link InputAction} instances for skill slots at runtime.
 *
 * <p>Resolves the skill equipped in a given slot from the player's {@link PlayerSkillContainer},
 * constructs an {@link InputAction} with the skill's cooldown, cast guard, and soulfire cost
 * wired in, and caches the result on the skill instance to avoid re-building on every input
 * tree rebuild.</p>
 */
public final class SkillSlotActionFactory {

    private SkillSlotActionFactory() {}

    /**
     * Resolves an {@link InputAction} for the skill equipped in {@code slot}.
     *
     * @param player the player whose skill container is queried
     * @param slot the slot to resolve
     * @param holdVariant {@code true} if the caller registered a hold-style input sequence;
     *                    returns {@code null} when the equipped skill's {@code requiresHold()} does
     *                    not match, making that input branch silently inert
     * @return the cached or newly built {@link InputAction}, or {@code null} if no skill is
     *         equipped or the hold variant does not match
     */
    public static InputAction create(SwordPlayer player, SkillSlot slot, boolean holdVariant) {

        ActiveSkill resolvedSkill = resolveActiveSkill(player, slot);
        if (resolvedSkill == null) return null;
        if (resolvedSkill.requiresHold() != holdVariant) return null;

        if (resolvedSkill.getCachedInputAction() != null) return resolvedSkill.getCachedInputAction();

        InputAction.Builder b = InputAction.builder()
            .action(resolvedSkill::execute)
            .cooldown(resolvedSkill::calculateCooldown)
            .canCast(resolvedSkill::canPerform)
            .displayCooldown(true)
            .displayDisabled(true)
            .resetIfCannotPerform(true);

        // Some skills perform an internal cast; where known, surface the cast duration on the action
        if (resolvedSkill instanceof VoidLungeSkill) {
            b.castDuration(() -> 250);
            b.requiredSoulfire(c -> 40f);
        }

        InputAction action = b.build();
        resolvedSkill.setCachedInputAction(action);
        return action;
    }

    /**
     * Convenience overload for umbral skill slots that do not use the hold/tap distinction.
     *
     * @param player the player whose skill container is queried
     * @param slot the slot to resolve
     * @return the cached or newly built {@link InputAction}, or {@code null} if no skill is equipped
     */
    public static InputAction create(SwordPlayer player, SkillSlot slot) {
        return create(player, slot, false);
    }

    private static ActiveSkill resolveActiveSkill(SwordPlayer player, SkillSlot slot) {
        CombatProfile profile = player.getCombatProfile();
        SkillId id = profile.getPlayerSkillContainer().getEquipped(slot);
        if (id == null) return null;

        Skill skill = SkillRegistry.get(id);

        return !(skill instanceof ActiveSkill active) ? null : active;
    }
}
