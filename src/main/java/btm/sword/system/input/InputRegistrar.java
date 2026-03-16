package btm.sword.system.input;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import org.bukkit.Material;

import btm.sword.config.Config;
import btm.sword.system.action.BlockAction;
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
import btm.sword.utility.SwordTimeUnit;

/**
 * Registers all input sequences into an {@link InputExecutionTree}.
 * <p>
 * Item-type discrimination is done via context predicates on each
 * {@link InputExecutionTree.ActionContextPair} rather than on {@link InputType}.
 * Shared nodes (e.g. DROP + RIGHT for lunge and throw) are populated in priority order —
 * the first registered action whose context passes is executed.
 * </p>
 * <p>
 * Visibility predicates of sequential nodes are checked BEFORE the action
 * of the current node is executed.
 * Keep this in mind when developing sequential action systems.
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
        registerDash(root, InputType.SWAP, DashDirection.FORWARD, Combatant::canAirDash);
        registerDash(root, InputType.SHIFT, DashDirection.BACKWARD, Combatant::canAirDash);
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

        new InputExecutionTree.InputNodeBuilder(root, List.of(
            InputType.RIGHT,
            InputType.SHIFT
        )).action(new LinkedList<>(List.of(
                new InputExecutionTree.ActionContextPair(
                    () -> InputAction.builder()
                        .name("Channel")
                        .action(UmbralBladeAction::beginHealChannel)
                        .cooldown(executor -> 3000)
                        .canCast(Combatant::canPerformHealAction)
                        .displayCooldown(true)
                        .displayDisabled(true)
                        .resetIfCannotPerform(true)
                        .build(),
                    SwordPlayer::umbralBladeState)
            )))
            .sameItemRequired(true)
            .cancellable(true)
            .display(true)
            .build();

        // Block: RIGHT begins blocking (if shield in offhand). The node timeout is the parry window —
        // while it is alive, SWAP can follow to trigger a parry. The hold task does NOT restart the
        // timer while blocking, so the window closes naturally after PARRY_AVAILABLE_MS ticks.
        // Registered before any other RIGHT-prefixed paths so the node exists with the correct timeout.
        new InputExecutionTree.InputNodeBuilder(root, List.of(
            InputType.RIGHT
        )).action(new LinkedList<>(List.of(
            new InputExecutionTree.ActionContextPair(
                () -> InputAction.builder()
                    .name("Block")
                    .action(c -> BlockAction.startBlock((SwordPlayer) c))
                    .cooldown(executor -> 0)
                    .canCast(c -> true)
                    .displayDisabled(false)
                    .resetIfCannotPerform(false)
                    .build(),
                sp -> sp.normalActState() && sp.getItemStackInHand(false).getType() == Material.SHIELD)
        )))
            .timeoutTicks(SwordTimeUnit.millisToTicks(Config.Combat.PARRY_AVAILABLE_MS))
            .cancellable(true)
            .display(true)
            .build();

        // Parry: RIGHT → SWAP while the trie is still at the RIGHT node (within timeout).
        // Context predicate confirms the Bukkit player is actively blocking. If the window
        // has expired (trie reset) SWAP hits root instead → dash or skill.
        new InputExecutionTree.InputNodeBuilder(root, List.of(
            InputType.RIGHT,
            InputType.SWAP
        )).action(new LinkedList<>(List.of(
            new InputExecutionTree.ActionContextPair(
                () -> InputAction.builder()
                .name("Parry")
                .action(c -> BlockAction.parryAttempt((SwordPlayer) c))
                .cooldown(executor -> 0)
                .canCast(c -> true)
                .displayDisabled(false)
                .resetIfCannotPerform(false)
                .build(),
                sp -> true)
            )))
            .cancellable(true)
            .display(true)
            .build();

        // basic attack combo chain (L, LL, LLL) — umbral variants registered first for priority
        registerBasicAttackCombo(root);


        // lunge (umbral link DROP + RIGHT) — registered FIRST, takes priority over throw when holding soul link
        new InputExecutionTree.InputNodeBuilder(root, List.of(
            InputType.DROP,
            InputType.RIGHT
        )).action(new LinkedList<>(List.of(
            new InputExecutionTree.ActionContextPair(
                () -> InputAction.builder()
                .name("Lunge")
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
                .name("Throw Ready")
                .action(ThrowAction::throwReady)
                .cooldown(executor -> 0)
                .canCast(c -> c.canPerformAction() && (c instanceof SwordPlayer sp && sp.nonUmbralState()))
                .displayDisabled(true)
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
                .canCast(c -> true)
                .castDuration(() -> 0)
                .displayCooldown(true)
                .displayDisabled(true)
                .resetIfCannotPerform(true)
                .build(),
                sp -> true) // debug tool only
            )))
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
                .canCast(Combatant::canPerformWieldAction)
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

        // hover blade at world position (DROP + SWAP while holding soul link)
        new InputExecutionTree.InputNodeBuilder(root, List.of(
            InputType.DROP,
            InputType.SWAP
        )).action(new LinkedList<>(List.of(
            new InputExecutionTree.ActionContextPair(
                () -> InputAction.builder()
                .name("Hover Blade")
                .action(UmbralBladeAction::hoverBlade)
                .cooldown(executor -> 200)
                .canCast(Combatant::canPerformUmbralAction)
                .displayCooldown(true)
                .displayDisabled(true)
                .resetIfCannotPerform(false)
                .build(),
                SwordPlayer::soulLinkState)
            )))
            .timeoutTicks(100)
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
        registerActiveSkillSlot(root, owner, SkillSlot.ACTIVE_1, 1);
        registerActiveSkillSlot(root, owner, SkillSlot.ACTIVE_2, 2);
    }

    // ── Helper methods ──────────────────────────────────────────────────────

    /**
     * Registers the basic attack combo chain (L, LL, LLL...) with both umbral-link and
     * normal-state variants. The first combo step uses {@code getDurationOfLastAttack} as
     * its cooldown; subsequent steps have zero cooldown so the chain flows naturally.
     *
     * @param root the tree root node
     */
    private static void registerBasicAttackCombo(InputExecutionTree.InputNode root) {
        Function<Combatant, Integer> attackCastDuration = executor -> (int) executor.calcValueReductive(
            AspectType.CELERITY,
            Config.Combat.ATTACKS_CAST_TIMING_MIN_DURATION,
            Config.Combat.ATTACKS_CAST_TIMING_MAX_DURATION,
            Config.Combat.ATTACKS_CAST_TIMING_REDUCTION_RATE);

        for (int step = 1; step <= 3; step++) {// TODO: make '3' dynamic or config it.
            final int comboStep = step;
            boolean isFirst = step == 1;
            boolean isLast = step == 3;

            Function<Combatant, Integer> cooldown = isFirst
                ? Combatant::getDurationOfLastAttack
                : executor -> 0;

            new InputExecutionTree.InputNodeBuilder(root,
                Collections.nCopies(step, InputType.LEFT)
            ).action(new LinkedList<>(List.of(
                    new InputExecutionTree.ActionContextPair(
                        () -> InputAction.builder()
                            .action(executor -> UmbralBladeAction.wieldedUmbralBladeAttack(executor, comboStep))
                            .cooldown(cooldown)
                            .canCast(Combatant::canPerformAction)
                            .castDuration(attackCastDuration)
                            .displayCooldown(true)
                            .displayDisabled(true)
                            .resetIfCannotPerform(false)
                            .build(),
                        SwordPlayer::umbralBladeState),
                new InputExecutionTree.ActionContextPair(
                    () -> InputAction.builder()
                        .action(executor -> UmbralBladeAction.basicAttackWithLink(executor, comboStep))
                        .cooldown(cooldown)
                        .canCast(Combatant::canPerformUmbralLinkAttack)
                        .requiredSoulfire(() -> 2.5f)
                        .castDuration(attackCastDuration)
                        .displayCooldown(true)
                        .displayDisabled(true)
                        .resetIfCannotPerform(!isLast)
                        .build(),
                    SwordPlayer::soulLinkState),
                new InputExecutionTree.ActionContextPair(
                    () -> InputAction.builder()
                        .action(executor -> AttackAction.basicAttack(executor, comboStep))
                        .cooldown(cooldown)
                        .canCast(Combatant::canPerformAction)
                        .castDuration(attackCastDuration)
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
        }
    }

    /**
     * Registers a dash and its follow-up dash-attack for the given input key and direction.
     * Dash: {@code key, key}. Dash-attack: {@code key, key, LEFT}.
     *
     * @param root      the tree root node
     * @param key       the double-tap input (SWAP for forward, SHIFT for backward)
     * @param direction the dash direction
     * @param context   context predicate for the dash action
     */
    private static void registerDash(InputExecutionTree.InputNode root,
                                     InputType key, DashDirection direction,
                                     Predicate<SwordPlayer> context) {
        new InputExecutionTree.InputNodeBuilder(root, List.of(key, key))
            .action(new LinkedList<>(List.of(
                new InputExecutionTree.ActionContextPair(
                    () -> InputAction.builder()
                        .action(executor -> MovementAction.dash(executor, direction))
                        .cooldown(executor -> executor.calcCooldown(AspectType.CELERITY, 200, 1000, 10))
                        .canCast(Combatant::canAirDash)
                        .castDuration(() -> Config.Movement.DASH_CAST_DURATION)
                        .displayDisabled(true)
                        .resetIfCannotPerform(true)
                        .build(),
                    context)
            )))
            .timeoutTicks(7)
            .cancellable(true)
            .display(true)
            .build();

        new InputExecutionTree.InputNodeBuilder(root, List.of(key, key, InputType.LEFT))
            .action(new LinkedList<>(List.of(
                new InputExecutionTree.ActionContextPair(
                    () -> InputAction.builder()
                        .action(executor -> DashAttackAction.dashAttack(executor, direction))
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
     * Registers tap and hold variants for an active skill slot.
     *
     * @param root      the tree root node
     * @param owner     the player who owns the tree
     * @param slot      the skill slot to bind
     * @param slotIndex the hotbar slot index (1 or 2) for context predicate
     */
    private static void registerActiveSkillSlot(InputExecutionTree.InputNode root,
                                                SwordPlayer owner,
                                                SkillSlot slot, int slotIndex) {
        // Tap variant
        new InputExecutionTree.InputNodeBuilder(root, List.of(InputType.SWAP, InputType.RIGHT))
            .action(new LinkedList<>(List.of(
                new InputExecutionTree.ActionContextPair(
                    () -> SkillSlotActionFactory.create(owner, slot, false),
                    sp -> sp.activeItemState(slotIndex))
            )))
            .dynamic(true)
            .sameItemRequired(true)
            .cancellable(true)
            .display(true)
            .build();

        // Hold variant
        new InputExecutionTree.InputNodeBuilder(root, List.of(
            InputType.SWAP, InputType.RIGHT, InputType.RIGHT_HOLD
        )).action(new LinkedList<>(List.of(
                new InputExecutionTree.ActionContextPair(
                    () -> SkillSlotActionFactory.create(owner, slot, true),
                    sp -> sp.activeItemState(slotIndex))
            )))
            .dynamic(true)
            .sameItemRequired(true)
            .cancellable(true)
            .display(true)
            .build();
    }
}
