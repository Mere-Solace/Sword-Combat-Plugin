package btm.sword.system.input;

import java.util.LinkedList;
import java.util.List;

import btm.sword.config.Config;
import btm.sword.system.action.UmbralBladeAction;
import btm.sword.system.action.attack.AttackAction;
import btm.sword.system.action.attack.DashAttackAction;
import btm.sword.system.action.movement.DashDirection;
import btm.sword.system.action.movement.MovementAction;
import btm.sword.system.action.skill.container.SkillSlot;
import btm.sword.system.action.skill.container.SkillSlotActionFactory;
import btm.sword.system.action.throwing.ThrowAction;
import btm.sword.system.action.utility.GrabAction;
import btm.sword.system.action.utility.UtilityAction;
import btm.sword.system.entity.aspect.AspectType;
import btm.sword.system.entity.impl.Combatant;
import btm.sword.system.entity.impl.SwordPlayer;

/**
 * Registers all input sequences into an {@link InputExecutionTree}.
 * <p>
 * Item-type discrimination is done via context predicates on each
 * {@link InputExecutionTree.ActionContextPair} rather than on {@link InputType}.
 * Shared nodes (e.g. DROP + RIGHT for lunge and throw) are populated in priority order —
 * the first registered action whose context passes is executed.
 * </p>
 */
public class InputRegistrar {

    /**
     * Registers the four dash inputs (SWAP+SWAP, SWAP+SWAP+LEFT, SHIFT+SHIFT, SHIFT+SHIFT+LEFT)
     * into the execution tree. Called only when movement is enabled.
     *
     * @param root the root node of the execution tree
     */
    public static void initializeMovementInputs(InputExecutionTree.InputNode root) {
        new InputExecutionTree.InputNodeBuilder(root, List.of(
            InputType.SWAP,
            InputType.SWAP
        )).action(new LinkedList<>(List.of(
            new InputExecutionTree.ActionContextPair(
                () -> InputAction.builder()
                .action(executor -> MovementAction.dash(executor, DashDirection.FORWARD))
                .cooldown(executor -> executor.calcCooldown(AspectType.CELERITY, 200, 1000, 10))
                .canCast(Combatant::canAirDash)
                .castDuration(() -> Config.Movement.DASH_CAST_DURATION)
                .displayDisabled(true)
                .resetIfCannotPerform(true)
                .build(),
                Combatant::canAirDash)
            )))
            .timeoutTicks(7)
            .cancellable(true)
            .display(true)
            .build();

        new InputExecutionTree.InputNodeBuilder(root, List.of(
            InputType.SWAP,
            InputType.SWAP,
            InputType.LEFT
        )).action(new LinkedList<>(List.of(
            new InputExecutionTree.ActionContextPair(
                () -> InputAction.builder()
                .action(executor -> DashAttackAction.dashAttack(executor, DashDirection.FORWARD))
                .cooldown(executor -> 0)
                .canCast(Combatant::canPerformAction)
                .castDuration(() -> 500)
                .displayDisabled(true)
                .resetIfCannotPerform(true)
                .build(),
                SwordPlayer::normalActState)
            )))
            .timeoutTicks(3)
            .cancellable(true)
            .display(true)
            .build();

        new InputExecutionTree.InputNodeBuilder(root, List.of(
            InputType.SHIFT,
            InputType.SHIFT
        )).action(new LinkedList<>(List.of(
            new InputExecutionTree.ActionContextPair(
                () -> InputAction.builder()
                .action(executor -> MovementAction.dash(executor, DashDirection.BACKWARD))
                .cooldown(executor -> executor.calcCooldown(AspectType.CELERITY, 200, 1000, 10))
                .canCast(Combatant::canAirDash)
                .castDuration(() -> Config.Movement.DASH_CAST_DURATION)
                .displayDisabled(true)
                .resetIfCannotPerform(true)
                .build(),
                SwordPlayer::normalActState)
            )))
            .timeoutTicks(7)
            .cancellable(true)
            .display(true)
            .build();

        new InputExecutionTree.InputNodeBuilder(root, List.of(
            InputType.SHIFT,
            InputType.SHIFT,
            InputType.LEFT
        )).action(new LinkedList<>(List.of(
            new InputExecutionTree.ActionContextPair(
                () -> InputAction.builder()
                .action(executor -> DashAttackAction.dashAttack(executor, DashDirection.BACKWARD))
                .cooldown(executor -> 0)
                .canCast(Combatant::canPerformAction)
                .castDuration(() -> 500)
                .displayDisabled(true)
                .resetIfCannotPerform(true)
                .build(),
                SwordPlayer::normalActState)
            )))
            .timeoutTicks(3)
            .cancellable(true)
            .display(true)
            .build();
    }

    /**
     * Registers all standard combat and skill input sequences into the execution tree.
     *
     * @param root  the root node of the execution tree
     * @param owner the player who owns this tree (used for dynamic skill resolution)
     */
    public static void initializeInputTree(InputExecutionTree.InputNode root, SwordPlayer owner) {
        // grab
        new InputExecutionTree.InputNodeBuilder(root, List.of(
            InputType.SHIFT,
            InputType.LEFT
        )).action(new LinkedList<>(List.of(
            new InputExecutionTree.ActionContextPair(
                () -> InputAction.builder()
                .action(GrabAction::grab)
                .cooldown(executor -> executor.calcCooldown(AspectType.FORTITUDE, 200, 1000, 10))
                .canCast(Combatant::canPerformAction)
                .requiredSoulfire(() -> 2f)
                .castDuration(() -> Config.Grab.CAST_DURATION)
                .displayDisabled(true)
                .resetIfCannotPerform(true)
                .build(),
                SwordPlayer::normalActState)
            )))
            .timeoutTicks(20)
            .cancellable(true)
            .display(true)
            .build();

        // basic umbral attacks — registered BEFORE basic attacks so they're first in the action list
        new InputExecutionTree.InputNodeBuilder(root, List.of(
            InputType.LEFT
        )).action(new LinkedList<>(List.of(
            new InputExecutionTree.ActionContextPair(
                () -> InputAction.builder()
                .action(executor -> UmbralBladeAction.basicAttackWithLink(executor, 1))
                .cooldown(Combatant::getDurationOfLastAttack)
                .canCast(Combatant::canPerformUmbralLinkAttack)
                .displayCooldown(true)
                .displayDisabled(true)
                .resetIfCannotPerform(true)
                .build(),
                SwordPlayer::soulLinkState),
            new InputExecutionTree.ActionContextPair(
                () -> InputAction.builder()
                .action(executor -> AttackAction.basicAttack(executor, 1))
                .cooldown(Combatant::getDurationOfLastAttack)
                .canCast(Combatant::canPerformAction)
                .castDuration(() -> 0)
                .displayCooldown(true)
                .displayDisabled(true)
                .resetIfCannotPerform(false)
                .build(),
                SwordPlayer::normalActState)
            )))
            .timeoutTicks(60)
            .sameItemRequired(true)
            .cancellable(true)
            .display(true)
            .build();

        new InputExecutionTree.InputNodeBuilder(root, List.of(
            InputType.LEFT,
            InputType.LEFT
        )).action(new LinkedList<>(List.of(
            new InputExecutionTree.ActionContextPair(
                () -> InputAction.builder()
                .action(executor -> UmbralBladeAction.basicAttackWithLink(executor, 2))
                .cooldown(executor -> 0)
                .canCast(Combatant::canPerformUmbralLinkAttack)
                .displayCooldown(true)
                .displayDisabled(true)
                .resetIfCannotPerform(true)
                .build(),
                SwordPlayer::soulLinkState),
            new InputExecutionTree.ActionContextPair(
                () -> InputAction.builder()
                .action(executor -> AttackAction.basicAttack(executor, 2))
                .cooldown(executor -> 0)
                .canCast(Combatant::canPerformAction)
                .castDuration(() -> 0)
                .displayCooldown(true)
                .displayDisabled(true)
                .resetIfCannotPerform(false)
                .build(),
                SwordPlayer::normalActState)
            )))
            .timeoutTicks(60)
            .sameItemRequired(true)
            .cancellable(true)
            .display(true)
            .build();

        new InputExecutionTree.InputNodeBuilder(root, List.of(
            InputType.LEFT,
            InputType.LEFT,
            InputType.LEFT
        )).action(new LinkedList<>(List.of(
            new InputExecutionTree.ActionContextPair(
                () -> InputAction.builder().action(executor -> UmbralBladeAction.basicAttackWithLink(executor, 3))
                    .cooldown(executor -> 0)
                    .canCast(Combatant::canPerformUmbralLinkAttack)
                    .displayCooldown(true)
                    .displayDisabled(true)
                    .resetIfCannotPerform(false)
                    .build(),
                SwordPlayer::soulLinkState),
            new InputExecutionTree.ActionContextPair(
                () -> InputAction.builder()
                    .action(executor -> AttackAction.basicAttack(executor, 3))
                    .cooldown(executor -> 0)
                    .canCast(Combatant::canPerformAction)
                    .castDuration(() -> 0)
                    .displayCooldown(true)
                    .displayDisabled(true)
                    .resetIfCannotPerform(false)
                    .build(),
                    SwordPlayer::normalActState)
            )))
            .timeoutTicks(60)
            .sameItemRequired(true)
            .cancellable(true)
            .display(true)
            .build();


        // lunge (umbral link DROP + RIGHT) — registered FIRST, takes priority over throw when holding soul link
        new InputExecutionTree.InputNodeBuilder(root, List.of(
            InputType.DROP,
            InputType.RIGHT
        )).action(new LinkedList<>(List.of(
            new InputExecutionTree.ActionContextPair(
                () -> InputAction.builder()
                .action(UmbralBladeAction::lunge)
                .cooldown(executor -> 200)
                .canCast(Combatant::canPerformUmbralAction)
                .requiredSoulfire(() -> 10f)
                .displayCooldown(true)
                .displayDisabled(true)
                .resetIfCannotPerform(false)
                .build(),
                SwordPlayer::soulLinkState),
            new InputExecutionTree.ActionContextPair(
                () -> InputAction.builder()
                .action(ThrowAction::throwReady)
                .cooldown(executor -> 0)
                .canCast(Combatant::canPerformAction)
                .displayDisabled(false)
                .resetIfCannotPerform(true)
                .build(),
                SwordPlayer::nonUmbralState)
            )))
            .timeoutTicks(100)
            .sameItemRequired(true)
            .cancellable(true)
            .display(true)
            .build();

        // throw
        new InputExecutionTree.InputNodeBuilder(root, List.of(
            InputType.DROP,
            InputType.RIGHT,
            InputType.RIGHT_HOLD
        )).action(new LinkedList<>(List.of(
            new InputExecutionTree.ActionContextPair(
                () -> InputAction.builder()
                .action(ThrowAction::throwItem)
                .cooldown(executor -> 0)
                .canCast(Combatant::canPerformAction)
                .castDuration(() -> 10)
                .displayDisabled(false)
                .resetIfCannotPerform(true)
                .build(),
                SwordPlayer::throwingNonUmbralState)
            )))
            .minHoldTime(600)
            .sameItemRequired(true)
            .cancellable(true)
            .display(true)
            .build();

        // debug kill
        new InputExecutionTree.InputNodeBuilder(root, List.of(
            InputType.DROP,
            InputType.DROP
        )).action(new LinkedList<>(List.of(
            new InputExecutionTree.ActionContextPair(
                () -> InputAction.builder()
                .action(UtilityAction::death)
                .cooldown(executor -> 0)
                .canCast(Combatant::canPerformAction)
                .castDuration(() -> 0)
                .displayCooldown(true)
                .displayDisabled(true)
                .resetIfCannotPerform(true)
                .build(),
                sp -> true) // debug tool only
            )))
            .sameItemRequired(true)
            .cancellable(true)
            .display(true)
            .build();

        // Umbral Skills (1, 2, 3) — only accessible while holding soul link
        new InputExecutionTree.InputNodeBuilder(root, List.of(
            InputType.SWAP,
            InputType.LEFT,
            InputType.LEFT
        )).action(new LinkedList<>(List.of(
            new InputExecutionTree.ActionContextPair(
                () -> SkillSlotActionFactory.create(owner, SkillSlot.UMBRAL_1),
                SwordPlayer::soulLinkState)
            )))
            .dynamic(true)
            .sameItemRequired(true)
            .cancellable(true)
            .display(true)
            .build();

        new InputExecutionTree.InputNodeBuilder(root, List.of(
            InputType.SWAP,
            InputType.LEFT,
            InputType.RIGHT
        )).action(new LinkedList<>(List.of(
            new InputExecutionTree.ActionContextPair(
                () -> SkillSlotActionFactory.create(owner, SkillSlot.UMBRAL_2),
                SwordPlayer::soulLinkState)
            )))
            .dynamic(true)
            .sameItemRequired(true)
            .cancellable(true)
            .display(true)
            .build();

        new InputExecutionTree.InputNodeBuilder(root, List.of(
            InputType.SWAP,
            InputType.LEFT,
            InputType.SWAP
        )).action(new LinkedList<>(List.of(
            new InputExecutionTree.ActionContextPair(
                () -> SkillSlotActionFactory.create(owner, SkillSlot.UMBRAL_3),
                SwordPlayer::soulLinkState)
            )))
            .dynamic(true)
            .sameItemRequired(true)
            .cancellable(true)
            .display(true)
            .build();

        // umbral blade toggling and wielding
        new InputExecutionTree.InputNodeBuilder(root, List.of(
            InputType.SHIFT,
            InputType.SWAP
        )).action(new LinkedList<>(List.of(
            new InputExecutionTree.ActionContextPair(
                () -> InputAction.builder()
                .action(UmbralBladeAction::toggle)
                .cooldown(executor -> 400)
                .canCast(Combatant::canPerformAction)
                .displayCooldown(true)
                .displayDisabled(true)
                .resetIfCannotPerform(true)
                .build(),
                SwordPlayer::normalActState)
            )))
            .sameItemRequired(true)
            .cancellable(true)
            .display(true)
            .build();

        new InputExecutionTree.InputNodeBuilder(root, List.of(
            InputType.SHIFT,
            InputType.DROP
        )).action(new LinkedList<>(List.of(
            new InputExecutionTree.ActionContextPair(
                () -> InputAction.builder()
                .action(UmbralBladeAction::wield)
                .cooldown(executor -> 400)
                .canCast(Combatant::canPerformAction)
                .displayCooldown(true)
                .displayDisabled(true)
                .resetIfCannotPerform(true)
                .build(),
                SwordPlayer::normalActState)
            )))
            .sameItemRequired(true)
            .cancellable(true)
            .display(true)
            .build();

        // heavy sweeps (umbral attacks)
        new InputExecutionTree.InputNodeBuilder(root, List.of(
            InputType.DROP,
            InputType.LEFT
        )).action(new LinkedList<>(List.of(
            new InputExecutionTree.ActionContextPair(
                () -> InputAction.builder()
                .action(UmbralBladeAction::sweep)
                .cooldown(executor -> 4000)
                .canCast(Combatant::canPerformUmbralAction)
                .requiredSoulfire(() -> 10f)
                .displayCooldown(true)
                .displayDisabled(true)
                .resetIfCannotPerform(false)
                .build(),
                SwordPlayer::normalActState)
            )))
            .timeoutTicks(100)
            .sameItemRequired(true)
            .cancellable(true)
            .display(true)
            .build();

        new InputExecutionTree.InputNodeBuilder(root, List.of(
            InputType.DROP,
            InputType.LEFT,
            InputType.RIGHT
        )).action(new LinkedList<>(List.of(
            new InputExecutionTree.ActionContextPair(
                () -> InputAction.builder()
                .action(UmbralBladeAction::sweep)
                .cooldown(executor -> 0)
                .canCast(Combatant::canPerformUmbralAction)
                .requiredSoulfire(() -> 15f)
                .displayCooldown(true)
                .displayDisabled(true)
                .resetIfCannotPerform(false)
                .build(),
                SwordPlayer::normalActState)
            )))
            .timeoutTicks(100)
            .sameItemRequired(true)
            .cancellable(true)
            .display(true)
            .build();

        new InputExecutionTree.InputNodeBuilder(root, List.of(
            InputType.DROP,
            InputType.LEFT,
            InputType.RIGHT,
            InputType.LEFT
        )).action(new LinkedList<>(List.of(
            new InputExecutionTree.ActionContextPair(
                () -> InputAction.builder()
                .action(UmbralBladeAction::sweep)
                .cooldown(executor -> 0)
                .canCast(Combatant::canPerformUmbralAction)
                .requiredSoulfire(() -> 25f)
                .displayCooldown(true)
                .displayDisabled(true)
                .resetIfCannotPerform(false)
                .build(),
                SwordPlayer::normalActState)
            )))
            .timeoutTicks(100)
            .sameItemRequired(true)
            .cancellable(true)
            .display(true)
            .build();

        // Active Skill slots (ACTIVE_1 and ACTIVE_2)
        // Tap variant
        new InputExecutionTree.InputNodeBuilder(root, List.of(
            InputType.SWAP,
            InputType.RIGHT
        )).action(new LinkedList<>(List.of(
            new InputExecutionTree.ActionContextPair(
                () -> SkillSlotActionFactory.create(owner, SkillSlot.ACTIVE_1, false),
                sp -> sp.activeItemState(1))
            )))
            .dynamic(true)
            .sameItemRequired(true)
            .cancellable(true)
            .display(true)
            .build();

        // Hold variant
        new InputExecutionTree.InputNodeBuilder(root, List.of(
            InputType.SWAP,
            InputType.RIGHT,
            InputType.RIGHT_HOLD
        )).action(new LinkedList<>(List.of(
                new InputExecutionTree.ActionContextPair(
                    () -> SkillSlotActionFactory.create(owner, SkillSlot.ACTIVE_1, true),
                    sp -> sp.activeItemState(1))
            )))
            .dynamic(true)
            .sameItemRequired(true)
            .cancellable(true)
            .display(true)
            .build();

        // ACTIVE_2 tap variant
        new InputExecutionTree.InputNodeBuilder(root, List.of(
            InputType.SWAP,
            InputType.RIGHT
        )).action(new LinkedList<>(List.of(
            new InputExecutionTree.ActionContextPair(
                () -> SkillSlotActionFactory.create(owner, SkillSlot.ACTIVE_2, false),
                sp -> sp.activeItemState(2))
            )))
            .dynamic(true)
            .sameItemRequired(true)
            .cancellable(true)
            .display(true)
            .build();

        // ACTIVE_2 hold variant
        new InputExecutionTree.InputNodeBuilder(root, List.of(
            InputType.SWAP,
            InputType.RIGHT,
            InputType.RIGHT_HOLD
        )).action(new LinkedList<>(List.of(
            new InputExecutionTree.ActionContextPair(
                () -> SkillSlotActionFactory.create(owner, SkillSlot.ACTIVE_2, true),
                sp -> sp.activeItemState(2))
            )))
            .dynamic(true)
            .sameItemRequired(true)
            .cancellable(true)
            .display(true)
            .build();
    }
}
