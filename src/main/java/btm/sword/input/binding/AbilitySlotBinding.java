package btm.sword.input.binding;

import btm.sword.action.skill.Skill;
import btm.sword.action.skill.SkillId;
import btm.sword.action.skill.SkillRegistry;
import btm.sword.action.skill.container.PlayerSkillContainer;
import btm.sword.action.skill.container.SkillSlot;
import btm.sword.action.skill.container.SkillSlotActionFactory;
import btm.sword.action.skill.container.SkillSlotState;
import btm.sword.action.skill.type.ActiveSkill;
import btm.sword.entity.player.SwordPlayer;
import btm.sword.input.InputAction;
import btm.sword.input.InputActionExecutor;
import btm.sword.input.InputType;
import btm.sword.input.ItemInputBinding;
import btm.sword.item.core.SwordItemType;
import btm.sword.item.special.AbilitySlotManager;

/**
 * AT_ROOT-phase binding that intercepts {@link InputType#LEFT} on the {@code ACTIVE_1}
 * and {@code ACTIVE_2} hotbar slots and dispatches the equipped active skill.
 *
 * <p>Matching requires both that the input is LEFT and that the player's currently held
 * hotbar slot is registered as an ability slot. Any LEFT click on a non-ability slot or
 * a non-LEFT input on an ability slot falls through to the trie.</p>
 *
 * <p>Replaces {@code SwordPlayer.handleAbilityInput}.</p>
 */
public final class AbilitySlotBinding implements ItemInputBinding {

    @Override
    public String id() {
        return "ability_slot";
    }

    @Override
    public Phase phase() {
        return Phase.AT_ROOT;
    }

    @Override
    public boolean matches(MatchContext ctx) {
        if (ctx.input() != InputType.LEFT) return false;
        SwordPlayer player = ctx.player();
        int heldSlot = player.getCurrentInvIndex();
        return player.getAbilitySlotManager().getActiveTypeForHeldSlot(heldSlot) != null;
    }

    @Override
    public boolean dispatch(MatchContext ctx) {
        SwordPlayer player = ctx.player();
        AbilitySlotManager manager = player.getAbilitySlotManager();
        int heldSlot = player.getCurrentInvIndex();
        SwordItemType itemType = manager.getActiveTypeForHeldSlot(heldSlot);

        // matches() guarantees itemType != null and input == LEFT, but be defensive.
        if (itemType == null) return false;

        if (player.isHeldItemOnCooldown()) return true;

        SkillSlot slot = itemType == SwordItemType.ACTIVE_1 ? SkillSlot.ACTIVE_1 : SkillSlot.ACTIVE_2;

        InputAction action = SkillSlotActionFactory.create(player, slot, false);
        if (action == null) return true;

        PlayerSkillContainer container = player.getCombatProfile().getPlayerSkillContainer();
        SkillId equippedId = container.getEquipped(slot);
        Skill skill = SkillRegistry.get(equippedId);
        if (!(skill instanceof ActiveSkill active) || !active.canPerform(player)) return true;

        SkillSlotState state = container.getSlotState(slot);
        if (System.currentTimeMillis() < state.cooldownExpiresAt()) return true;

        InputActionExecutor.execute(action, player);

        manager.consumeUse(heldSlot);
        long expiry = System.currentTimeMillis() + active.calculateCooldown(player);
        SkillSlotState current = container.getSlotState(slot);
        container.setSlotState(slot,
            new SkillSlotState(current.remainingUses(), current.remainingDurability(), expiry));

        return true;
    }
}
