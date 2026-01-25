package btm.sword.system.input;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

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
import btm.sword.system.control.SwordScheduler;
import btm.sword.system.entity.aspect.AspectType;
import btm.sword.system.entity.impl.Combatant;
import btm.sword.system.entity.impl.SwordPlayer;
import btm.sword.system.item.SwordItemType;
import btm.sword.utility.SwordTimeUnit;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;

/**
 * Represents a finite state tree that tracks sequences of player {@link InputType} inputs,
 * maps them to corresponding {@link InputAction}s, supports timeout resets,
 * and manages child nodes representing next steps in input sequences.
 * <p>
 * This class is central to handling input combos, timing, and action execution
 * in the Sword plugin input system.
 * </p>
 */
public class InputExecutionTree {
    private static final int MAX_ATTEMPT_ITERATIONS_BEFORE_FORCE_RESET = 3;
    private int currentAttempts = 0;

    private final InputNode root = new InputNode(null);
    private final SwordPlayer owner;

    private InputNode currentNode;
    private Component baseSequenceToDisplay;
    private Component potentialInputSelectionText;
    private ScheduledFuture<?> timeoutTimer;

    public InputExecutionTree(SwordPlayer owner) {
        this.owner = owner;
        currentNode = root;
        baseSequenceToDisplay = Component.text("");
        potentialInputSelectionText = Component.text("");
        timeoutTimer = null;
    }



    /**
     * Processes an input step in the execution tree, updating current node and sequence.
     * Returns the new node reached or null if the input is invalid or sequence is reset.
     * Automatically resets sequence if current node is a leaf.
     *
     * @return the resulting {@link InputNode} or null if invalid/reset
     */
    public InputNode step(InputKey inputKey) {
        stopTimeoutTimer();
        potentialInputSelectionText = Component.text(""); // reset potential input before new step
        // before taking input, if it is known that the current node is a leaf, reset and take input from the root
        if (!hasChildren()) reset();

        if (currentNode == null) {
            reset();
            return null;
        }

        // initialize a new node that points to the traversal of the input
        InputNode next = currentNode.retrieveAvailableNode(inputKey, owner);

        if (next == null) {
            if (isAtRoot()) return null;
            else {
                reset();
                if (currentNode.isCancellable())
                    return step(inputKey);

                else
                    return null;
            }
        }

        InputAction nextAction = next.resolveAction();

        if (nextAction != null && !InputActionExecutor.canCast(nextAction, owner)) {
            currentAttempts++; // unsuccessful attempt
            if (currentAttempts >= MAX_ATTEMPT_ITERATIONS_BEFORE_FORCE_RESET) {
                reset(); // reset if there is a deadlock state and the user is spamming
            }
            else {
                return null; // added this so that stepping forward cannot occur
            }
        }
        currentAttempts = 0; // successful attempt

        baseSequenceToDisplay = baseSequenceToDisplay.append(Component.text(inputToString(inputKey.input()),
            Config.SwordColor.TITLE_INPUT_STRING, TextDecoration.BOLD));

        currentNode = next;

        if (hasChildren()) {
            baseSequenceToDisplay = baseSequenceToDisplay.append(Component.text(" ➞ ",
                Config.SwordColor.TITLE_INPUT_STRING, TextDecoration.ITALIC));

            Iterator<Map.Entry<InputKey, InputNode>> it = currentNode.getChildren().entrySet().iterator();
            boolean inputAccepted = false;
            while (it.hasNext()) {
                Map.Entry<InputKey, InputNode> entry = it.next();

                // Skip paths that are not visible or not accessible to this player in the current state
                if (entry.getValue().hiddenFor(owner) || !entry.getKey().checkAccessibility(owner)) {
                    continue;
                }

                InputAction action = entry.getValue().resolveAction();

                boolean ready;
                if (action == null) {
                    ready = true;
                }
                else if (entry.getKey().input().equals(InputType.DROP)) {
                    ready = InputActionExecutor.canCast(action, owner) && !owner.getItemTypeInHand(true).isAir();
                }
                else {
                    ready = InputActionExecutor.canCast(action, owner);
                }

                potentialInputSelectionText = potentialInputSelectionText
                    .append(Component.text(inputToString(entry.getKey().input()),
                        ready ?
                            Config.SwordColor.TEXT_ITEM_BASE :
                            Config.SwordColor.TEXT_COOL_DARK,
                        ready ?
                            TextDecoration.BOLD :
                            TextDecoration.ITALIC))
                    .append(it.hasNext() ?
                        Component.text(", ", Config.SwordColor.TEXT_COOL_DARK) :
                        Component.text(""));
                inputAccepted = true;
            }
            if (inputAccepted) startTimeoutTimer();
        }

        return next;
    }

    /**
     * Starts the timeout countdown to reset the input sequence after inactivity.
     */
    private void startTimeoutTimer() {
        timeoutTimer = SwordScheduler.runBukkitTaskLater(this::reset,
            SwordTimeUnit.ticksToMillis(
                currentNode.getTimeoutTicks()
            ),
            TimeUnit.MILLISECONDS);
    }

    /**
     * Stops the currently running timeout timer task.
     */
    public void stopTimeoutTimer() {
        if (timeoutTimer != null && !timeoutTimer.isCancelled()) timeoutTimer.cancel(true);
    }

    /**
     * Restarts the timeout timer by cancelling the existing and starting a new one.
     */
    public void restartTimeoutTimer() {
        stopTimeoutTimer();
        startTimeoutTimer();
    }

    /**
     * Resets the input execution tree to the root node and clears input sequence display.
     */
    public void reset() {
        currentNode = root;
        baseSequenceToDisplay = Component.text("");
    }

    /**
     * Checks if the current node is the root node.
     *
     * @return true if at root, false otherwise
     */
    public boolean isAtRoot() {
        return currentNode == root;
    }

    /**
     * Checks if the current node has a child corresponding to the specified input.
     *
     * @param inputKey the {@link InputType} to check
     * @return true if a child node exists, false otherwise
     */
    public boolean nextExists(InputKey inputKey) {
        return currentNode.retrieveAvailableNode(inputKey, owner) != null;
    }

    /**
     * Checks whether the current node has children nodes.
     *
     * @return true if children exist, false otherwise
     */
    public boolean hasChildren() {
        return !currentNode.children.isEmpty();
    }

    public Component getInputSequenceAsComponent() {
        return Component.textOfChildren(baseSequenceToDisplay, potentialInputSelectionText);
    }

    /**
     * Converts an {@link InputType} to its string representation for sequence display.
     *
     * @param type input type to convert
     * @return string representation such as "L", "R", "_", etc.
     */
    private String inputToString(InputType type) {
        return switch (type) {
            case LEFT -> "L";
            case RIGHT -> "R";
            case RIGHT_TAP, SHIFT_TAP -> "•";
            case RIGHT_HOLD, SHIFT_HOLD -> "▁▁";
            case DROP -> "D";
            case SWAP -> "F";
            case SHIFT -> "S";
        };
    }

    /**
     * Checks if the current input node requires all inputs in the sequence to use the same item.
     *
     * @return true if same item is required, false otherwise
     */
    public boolean requiresSameItem() {
        return currentNode.isSameItemRequired();
    }

    /**
     * Gets the minimum hold time in milliseconds required for the next input node of the specified hold type.
     *
     * @param holdType the hold input type
     * @return minimum hold time in milliseconds, or -1 if no such node exists
     */
    public long getMinHoldLengthOfNext(InputKey holdType) {
        InputNode next = currentNode.retrieveAvailableNode(holdType, owner);
        if (next == null) return -1;
        return next.getMinHoldTime();
    }

    public long timeoutTicks() {
        return currentNode.timeoutTicks;
    }

    public void initializeInputTree() {
        // Item independent actions:
        // dodge forward, dodge backward
        new InputNodeBuilder(root, List.of(
            InputKey.of(InputType.SWAP),
            InputKey.of(InputType.SWAP)
        )).action(() -> InputAction.builder()
                .action(executor -> MovementAction.dash(executor, DashDirection.FORWARD))
                .cooldown(executor -> executor.calcCooldown(AspectType.CELERITY, 200, 1000, 10))
                .canCast(Combatant::canAirDash)
                .castDuration(() -> Config.Movement.DASH_CAST_DURATION)
                .displayDisabled(true)
                .resetIfCannotPerform(true)
                .build())
            .timeoutTicks(7)
            .cancellable(true)
            .display(true)
            .build();

        new InputNodeBuilder(root, List.of(
            InputKey.of(InputType.SWAP),
            InputKey.of(InputType.SWAP),
            InputKey.of(InputType.LEFT, SwordItemType.UMBRAL_LINK)
        )).action(() -> InputAction.builder()
                .action(UmbralBladeAction::shadowBlink)
                .cooldown(executor -> 1000)
                .canCast(Combatant::canPerformShadowBlink)
                .displayDisabled(true)
                .resetIfCannotPerform(true)
                .build())
            .cancellable(true)
            .display(true)
            .build();

        new InputNodeBuilder(root, List.of(
            InputKey.of(InputType.SWAP),
            InputKey.of(InputType.SWAP),
            InputKey.of(InputType.LEFT)
        )).action(() -> InputAction.builder()
                .action(executor -> DashAttackAction.dashAttack(executor, DashDirection.FORWARD))
                .cooldown(executor -> 0)
                .canCast(Combatant::canPerformAction)
                .castDuration(() -> 500)
                .displayDisabled(true)
                .resetIfCannotPerform(true)
                .build())
            .timeoutTicks(3)
            .cancellable(true)
            .display(true)
            .build();

        new InputNodeBuilder(root, List.of(
            InputKey.of(InputType.SHIFT),
            InputKey.of(InputType.SHIFT)
        )).action(() -> InputAction.builder()
                .action(executor -> MovementAction.dash(executor, DashDirection.BACKWARD))
                .cooldown(executor -> executor.calcCooldown(AspectType.CELERITY, 200, 1000, 10))
                .canCast(Combatant::canAirDash)
                .castDuration(() -> Config.Movement.DASH_CAST_DURATION)
                .displayDisabled(true)
                .resetIfCannotPerform(true)
                .build())
            .timeoutTicks(7)
            .cancellable(true)
            .display(true)
            .build();

        new InputNodeBuilder(root, List.of(
            InputKey.of(InputType.SHIFT),
            InputKey.of(InputType.SHIFT),
            InputKey.of(InputType.LEFT)
        )).action(() -> InputAction.builder()
                .action(executor -> DashAttackAction.dashAttack(executor, DashDirection.BACKWARD))
                .cooldown(executor -> 0)
                .canCast(Combatant::canPerformAction)
                .castDuration(() -> 500)
                .displayDisabled(true)
                .resetIfCannotPerform(true)
                .build())
            .timeoutTicks(3)
            .cancellable(true)
            .display(true)
            .build();

        // strafe
        new InputNodeBuilder(root, List.of(
            InputKey.of(InputType.SHIFT),
            InputKey.of(InputType.RIGHT)
        )).action(() -> InputAction.builder()
                .action(executor -> MovementAction.dash(executor, DashDirection.RIGHT))
                .cooldown(executor -> executor.calcCooldown(AspectType.CELERITY, 200, 1000, 10))
                .canCast(Combatant::canStrafe)
                .requiredSoulfire(() -> 5f)
                .castDuration(() -> Config.Movement.DASH_CAST_DURATION)
                .displayDisabled(true)
                .resetIfCannotPerform(true)
                .build())
            .timeoutTicks(7)
            .cancellable(true)
            .display(true)
            .build();

        // strafe attack right
        new InputNodeBuilder(root, List.of(
            InputKey.of(InputType.SHIFT),
            InputKey.of(InputType.RIGHT),
            InputKey.of(InputType.LEFT)
        )).action(() -> InputAction.builder()
                .action(executor -> DashAttackAction.dashAttack(executor, DashDirection.RIGHT))
                .cooldown(executor -> 0)
                .canCast(Combatant::canPerformAction)
                .castDuration(() -> 500)
                .displayDisabled(true)
                .resetIfCannotPerform(true)
                .build())
            .cancellable(true)
            .display(true)
            .build();

        new InputNodeBuilder(root, List.of(
            InputKey.of(InputType.SHIFT),
            InputKey.of(InputType.LEFT)
        )).action(() -> InputAction.builder()
                .action(executor -> MovementAction.dash(executor, DashDirection.LEFT))
                .cooldown(executor -> executor.calcCooldown(AspectType.CELERITY, 200, 1000, 10))
                .canCast(Combatant::canStrafe)
                .requiredSoulfire(() -> 5f)
                .castDuration(() -> Config.Movement.DASH_CAST_DURATION)
                .displayDisabled(true)
                .resetIfCannotPerform(true)
                .build())
            .timeoutTicks(7)
            .cancellable(true)
            .display(true)
            .build();

        // strafe attack left
        new InputNodeBuilder(root, List.of(
            InputKey.of(InputType.SHIFT),
            InputKey.of(InputType.LEFT),
            InputKey.of(InputType.LEFT)
        )).action(() -> InputAction.builder()
                .action(executor -> DashAttackAction.dashAttack(executor, DashDirection.RIGHT))
                .cooldown(executor -> 0)
                .canCast(Combatant::canPerformAction)
                .castDuration(() -> 500)
                .displayDisabled(true)
                .resetIfCannotPerform(true)
                .build())
            .cancellable(true)
            .display(true)
            .build();

        // grab
        new InputNodeBuilder(root, List.of(
            InputKey.of(InputType.SWAP),
            InputKey.of(InputType.RIGHT)
        )).action(() -> InputAction.builder()
                .action(GrabAction::grab)
                .cooldown(executor -> executor.calcCooldown(AspectType.FORTITUDE, 200, 1000, 10))
                .canCast(Combatant::canPerformAction)
                .requiredSoulfire(() -> 2f)
                .castDuration(() -> Config.Grab.CAST_DURATION)
                .displayDisabled(true)
                .resetIfCannotPerform(true)
                .build())
            .timeoutTicks(20)
            .cancellable(true)
            .display(true)
            .build();

        // Item dependent actions:

        // basic umbral attacks
        new InputNodeBuilder(root, List.of(
            InputKey.of(InputType.LEFT, SwordItemType.UMBRAL_LINK)
        )).action(() -> InputAction.builder()
                .action(executor -> UmbralBladeAction.basicAttackWithLink(executor, 1))
                .cooldown(executor -> Math.toIntExact(Math.max(0, (executor.getTimeOfLastAttack() + executor.getDurationOfLastAttack()) - System.currentTimeMillis())))
                .canCast(Combatant::canPerformUmbralLinkAttack)
                .displayCooldown(true)
                .displayDisabled(true)
                .resetIfCannotPerform(true)
                .build())
            .timeoutTicks(60)
            .sameItemRequired(true)
            .cancellable(true)
            .display(true)
            .build();

        new InputNodeBuilder(root, List.of(
            InputKey.of(InputType.LEFT, SwordItemType.UMBRAL_LINK),
            InputKey.of(InputType.LEFT, SwordItemType.UMBRAL_LINK)
        )).action(() -> InputAction.builder()
                .action(executor -> UmbralBladeAction.basicAttackWithLink(executor, 2))
                .cooldown(executor -> 0)
                .canCast(Combatant::canPerformUmbralLinkAttack)
                .displayCooldown(true)
                .displayDisabled(true)
                .resetIfCannotPerform(true)
                .build())
            .timeoutTicks(60)
            .sameItemRequired(true)
            .cancellable(true)
            .display(true)
            .build();

        new InputNodeBuilder(root, List.of(
            InputKey.of(InputType.LEFT, SwordItemType.UMBRAL_LINK),
            InputKey.of(InputType.LEFT, SwordItemType.UMBRAL_LINK),
            InputKey.of(InputType.LEFT, SwordItemType.UMBRAL_LINK)
        )).action(() -> InputAction.builder()
                .action(executor -> UmbralBladeAction.basicAttackWithLink(executor, 3))
                .cooldown(executor -> 0)
                .canCast(Combatant::canPerformUmbralLinkAttack)
                .displayCooldown(true)
                .displayDisabled(true)
                .resetIfCannotPerform(false)
                .build())
            .timeoutTicks(60)
            .sameItemRequired(true)
            .cancellable(true)
            .display(true)
            .build();

        // basic attacks
        new InputNodeBuilder(root, List.of(
            InputKey.of(InputType.LEFT)
        )).action(() -> InputAction.builder()
                .action(executor -> AttackAction.basicAttack(executor, 1))
                .cooldown(executor -> Math.toIntExact(Math.max(0, (executor.getTimeOfLastAttack() + executor.getDurationOfLastAttack()) - System.currentTimeMillis())))
                .canCast(Combatant::canPerformAction)
                .castDuration(() -> 0)
                .displayCooldown(true)
                .displayDisabled(true)
                .resetIfCannotPerform(false)
                .build())
            .sameItemRequired(true)
            .cancellable(true)
            .display(true)
            .build();

        new InputNodeBuilder(root, List.of(
            InputKey.of(InputType.LEFT),
            InputKey.of(InputType.LEFT)
        )).action(() -> InputAction.builder()
                .action(executor -> AttackAction.basicAttack(executor, 2))
                .cooldown(executor -> 0)
                .canCast(Combatant::canPerformAction)
                .castDuration(() -> 0)
                .displayCooldown(true)
                .displayDisabled(true)
                .resetIfCannotPerform(false)
                .build())
            .sameItemRequired(true)
            .cancellable(true)
            .display(true)
            .build();

        new InputNodeBuilder(root, List.of(
            InputKey.of(InputType.LEFT),
            InputKey.of(InputType.LEFT),
            InputKey.of(InputType.LEFT)
        )).action(() -> InputAction.builder()
                .action(executor -> AttackAction.basicAttack(executor, 3))
                .cooldown(executor -> 0)
                .canCast(Combatant::canPerformAction)
                .castDuration(() -> 1) // changed
                .displayCooldown(true)
                .displayDisabled(true)
                .resetIfCannotPerform(false)
                .build())
            .sameItemRequired(true)
            .cancellable(true)
            .display(true)
            .build();

        // throw hold action
        new InputNodeBuilder(root, List.of(
            InputKey.of(InputType.DROP),
            InputKey.of(InputType.RIGHT)
        )).action(() -> InputAction.builder()
                .action(ThrowAction::throwReady)
                .cooldown(executor -> 0)
                .canCast(Combatant::canPerformAction)
                .displayDisabled(false)
                .resetIfCannotPerform(true)
                .build())
            .sameItemRequired(true)
            .cancellable(true)
            .display(true)
            .build();

        // throw
        new InputNodeBuilder(root, List.of(
            InputKey.of(InputType.DROP),
            InputKey.of(InputType.RIGHT),
            InputKey.of(InputType.RIGHT_HOLD)
        )).action(() -> InputAction.builder()
                .action(ThrowAction::throwItem)
                .cooldown(executor -> 0)
                .canCast(Combatant::canPerformAction)
                .castDuration(() -> 10)
                .displayDisabled(false)
                .resetIfCannotPerform(true)
                .build())
            .minHoldTime(600)
            .sameItemRequired(true)
            .cancellable(true)
            .display(true)
            .build();

        // just in case
        new InputNodeBuilder(root, List.of(
            InputKey.of(InputType.DROP),
            InputKey.of(InputType.DROP)
        )).action(() -> InputAction.builder()
                .action(UtilityAction::death)
                .cooldown(executor -> 0)
                .canCast(Combatant::canPerformAction)
                .castDuration(() -> 0)
                .displayCooldown(true)
                .displayDisabled(true)
                .resetIfCannotPerform(true)
                .build())
            .sameItemRequired(true)
            .cancellable(true)
            .display(true)
            .build();


        // Skill mappings:

        // Umbral Skills (1, 2, and 3)
        new InputNodeBuilder(root, List.of(
            InputKey.of(InputType.SWAP),
            InputKey.of(InputType.LEFT),
            InputKey.of(InputType.LEFT)
        )).action(() -> SkillSlotActionFactory.create(owner, SkillSlot.UMBRAL_1))
            .sameItemRequired(true)
            .cancellable(true)
            .display(true)
            .build();

        new InputNodeBuilder(root, List.of(
            InputKey.of(InputType.SWAP),
            InputKey.of(InputType.LEFT),
            InputKey.of(InputType.RIGHT)
        )).action(() -> SkillSlotActionFactory.create(owner, SkillSlot.UMBRAL_2))
            .sameItemRequired(true)
            .cancellable(true)
            .display(true)
            .build();

        new InputNodeBuilder(root, List.of(
            InputKey.of(InputType.SWAP),
            InputKey.of(InputType.LEFT),
            InputKey.of(InputType.SWAP)
        )).action(() -> SkillSlotActionFactory.create(owner, SkillSlot.UMBRAL_3))
            .sameItemRequired(true)
            .cancellable(true)
            .display(true)
            .build();

//
//        // Slow em down
//        new InputNodeBuilder(root, List.of(
//            InputKey.of(InputType.RIGHT),
//            InputKey.of(InputType.RIGHT_HOLD),
//            InputKey.of(InputType.DROP)
//        )).action(() -> new InputAction(
//                UtilityAction::bulletTime,
//                executor -> 5000,
//                Combatant::canPerformAction,
//                70f,
//                true,
//                true,
//                true))
//            .minHoldTime(500)
//            .sameItemRequired(true)
//            .cancellable(true)
//            .display(true)
//            .build();

        // umbral blade toggling and wielding
        new InputNodeBuilder(root, List.of(
            InputKey.of(InputType.SHIFT),
            InputKey.of(InputType.SWAP)
        )).action(() -> InputAction.builder()
                .action(UmbralBladeAction::toggle)
                .cooldown(executor -> 400)
                .canCast(Combatant::canPerformAction)
                .displayCooldown(true)
                .displayDisabled(true)
                .resetIfCannotPerform(true)
                .build())
            .sameItemRequired(true)
            .cancellable(true)
            .display(true)
            .build();

        new InputNodeBuilder(root, List.of(
            InputKey.of(InputType.SHIFT),
            InputKey.of(InputType.DROP)
        )).action(() -> InputAction.builder()
                .action(UmbralBladeAction::wield)
                .cooldown(executor -> 400)
                .canCast(Combatant::canPerformAction)
                .displayCooldown(true)
                .displayDisabled(true)
                .resetIfCannotPerform(true)
                .build())
            .sameItemRequired(true)
            .cancellable(true)
            .display(true)
            .build();

        // heavy sweeps (umbral attacks)
        new InputNodeBuilder(root, List.of(
            InputKey.of(InputType.DROP),
            InputKey.of(InputType.LEFT)
        )).action(() -> InputAction.builder()
                .action(UmbralBladeAction::sweep)
                .cooldown(executor -> 4000)
                .canCast(Combatant::canPerformUmbralAction)
                .requiredSoulfire(() -> 10f)
                .displayCooldown(true)
                .displayDisabled(true)
                .resetIfCannotPerform(false)
                .build())
            .timeoutTicks(100)
            .sameItemRequired(true)
            .cancellable(true)
            .display(true)
            .build();

        new InputNodeBuilder(root, List.of(
            InputKey.of(InputType.DROP),
            InputKey.of(InputType.LEFT),
            InputKey.of(InputType.RIGHT)
        )).action(() -> InputAction.builder()
                .action(UmbralBladeAction::sweep)
                .cooldown(executor -> 0)
                .canCast(Combatant::canPerformUmbralAction)
                .requiredSoulfire(() -> 15f)
                .displayCooldown(true)
                .displayDisabled(true)
                .resetIfCannotPerform(false)
                .build())
            .timeoutTicks(100)
            .sameItemRequired(true)
            .cancellable(true)
            .display(true)
            .build();

        new InputNodeBuilder(root, List.of(
            InputKey.of(InputType.DROP),
            InputKey.of(InputType.LEFT),
            InputKey.of(InputType.RIGHT),
            InputKey.of(InputType.LEFT)
        )).action(() -> InputAction.builder()
                .action(UmbralBladeAction::sweep)
                .cooldown(executor -> 0)
                .canCast(Combatant::canPerformUmbralAction)
                .requiredSoulfire(() -> 25f)
                .displayCooldown(true)
                .displayDisabled(true)
                .resetIfCannotPerform(false)
                .build())
            .timeoutTicks(100)
            .sameItemRequired(true)
            .cancellable(true)
            .display(true)
            .build();

        // lunges (umbral throw)
        new InputNodeBuilder(root, List.of(
            InputKey.of(InputType.DROP),
            InputKey.of(InputType.SWAP),
            InputKey.of(InputType.RIGHT)
        )).action(
            () -> InputAction.builder()
                .action(UmbralBladeAction::lunge)
                .cooldown(executor -> 200)
                .canCast(Combatant::canPerformUmbralAction)
                .requiredSoulfire(() -> 10f)
                .displayCooldown(true)
                .displayDisabled(true)
                .resetIfCannotPerform(false)
                .build())
            .timeoutTicks(100)
            .sameItemRequired(true)
            .cancellable(true)
            .display(true)
            .build();
    }

    /**
     * Represents a node in the {@link InputExecutionTree}, used to map input sequences to actions.
     * Each node maintains children inputs leading to subsequent nodes,
     * and stores metadata like whether the sequence requires same item, is cancellable, or displayable.
     */
    @Getter
    @Setter
    public static class InputNode {
        private Supplier<InputAction> action;
        private Consumer<SwordPlayer> internalAction;
        private int timeoutTicks;
        private final LinkedHashMap<InputKey, InputNode> children = new LinkedHashMap<>();
        private boolean sameItemRequired;
        private boolean cancellable;
        private boolean display;
        private final int minHoldTime;
        private Predicate<SwordPlayer> visibleIf = p -> true; // dynamic visibility predicate

        /**
         * Constructs an InputNode with an associated action and hold time.
         *
         * @param action the associated {@link InputAction}, or null if none
         * @param minHoldTime minimum hold time in milliseconds for hold inputs, or -1 if none
         */
        public InputNode(Supplier<InputAction> action, int minHoldTime) {
            this.action = action;
            this.minHoldTime = minHoldTime;
            this.timeoutTicks = 20;
        }

        /**
         * Constructs an InputNode with an associated action and no hold time limit.
         *
         * @param action the associated {@link InputAction}, or null if none
         */
        public InputNode(Supplier<InputAction> action) {
            this(action, -1);
        }

        public InputAction resolveAction() {
            return action == null ? null : action.get();
        }

        /**
         * Adds a child node for the specified input with an associated action and no hold time.
         *
         * @param inputKey the {@link InputType} input triggering the child node
         * @param action the {@link InputAction} associated with the child node, or null
         */
        public void addChild(InputKey inputKey, Supplier<InputAction> action) {
            children.putIfAbsent(inputKey, new InputNode(action));
        }

        /**
         * Adds a child node for the specified input with an associated action and minimum hold time.
         *
         * @param inputKey the input triggering the child node
         * @param action the action associated with the child node
         * @param minHoldLength minimum hold time in ms required for this input
         */
        public void addChild(InputKey inputKey, Supplier<InputAction> action, int minHoldLength) {
            children.putIfAbsent(inputKey, new InputNode(action, minHoldLength));
        }

        /**
         * Finds child node where input matches AND the condition is matched
         */
        public InputNode retrieveAvailableNode(InputType type, SwordPlayer owner) {
            for (Map.Entry<InputKey, InputNode> entry : children.entrySet()) {
                if (entry.getKey().input().equals(type) &&
                    entry.getKey().checkAccessibility(owner)) {
                    return entry.getValue();
                }
            }
            return null;
        }

        /**
         * Finds a child node by a concrete InputKey and owner. This is the runtime path resolver
         * that takes into account predicates attached to keys or nodes.
         */
        public InputNode retrieveAvailableNode(InputKey inputKey, SwordPlayer owner) {
            for (Map.Entry<InputKey, InputNode> entry : children.entrySet()) {
                if (!entry.getKey().input().equals(inputKey.input())) continue;
                if (!entry.getKey().checkAccessibility(owner)) continue;
                // node-level visibility
                if (entry.getValue().hiddenFor(owner)) continue;
                return entry.getValue();
            }
            return null;
        }

        /**
         * Gets a child node by exact key reference (used by the builder during construction).
         */
        public InputNode getChildByKey(InputKey key) {
            return children.get(key);
        }

        /**
         * Checks whether adding inputKey would create an overlapping acceptor (i.e., any existing
         * child already accepts one of the same allowed item types for the same input). This prevents
         * ambiguous overlapping sequences when using specific allowed types.
         */
        public boolean noMatchingChild(InputKey inputKey) {
            for (InputKey k : children.keySet()) {
                if (!k.input().equals(inputKey.input())) continue;
                for (btm.sword.system.item.SwordItemType t : inputKey.allowedItemTypes()) {
                    if (k.allows(t)) return false;
                }
            }
            return true;
        }

        public void setVisibleIf(Predicate<SwordPlayer> visibleIf) {
            this.visibleIf = visibleIf != null ? visibleIf : (p -> true);
        }

        public boolean hiddenFor(SwordPlayer player) {
            return !visibleIf.test(player);
        }
    }

    public static class InputNodeBuilder {
        private final List<InputKey> inputSequence;
        private final InputNode root;

        private Supplier<InputAction> action;
        private Integer timeoutTicks = null;
        private Integer minHoldTime = null;
        private Boolean sameItemRequired = null;
        private Boolean cancellable = null;
        private Boolean display = null;
        private Predicate<SwordPlayer> visibleIf;

        public InputNodeBuilder(InputNode root, List<InputKey> inputSequence) {
            this.root = root;
            this.inputSequence = inputSequence;
        }

        public InputNodeBuilder action(Supplier<InputAction> action) {
            this.action = action;
            return this;
        }

        public InputNodeBuilder timeoutTicks(int timeoutTicks) {
            this.timeoutTicks = timeoutTicks;
            return this;
        }

        public InputNodeBuilder minHoldTime(int minHoldTime) {
            this.minHoldTime = minHoldTime;
            return this;
        }

        public InputNodeBuilder sameItemRequired(boolean sameItemRequired) {
            this.sameItemRequired = sameItemRequired;
            return this;
        }

        public InputNodeBuilder cancellable(boolean cancellable) {
            this.cancellable = cancellable;
            return this;
        }

        public InputNodeBuilder display(boolean display) {
            this.display = display;
            return this;
        }

        public InputNodeBuilder visibleIf(Predicate<SwordPlayer> predicate) {
            this.visibleIf = predicate;
            return this;
        }

        public void build() {
            InputNode dummy = root;
            for (InputKey inputKey : inputSequence) {
                if (dummy.noMatchingChild(inputKey)) {
                    if (minHoldTime != null &&
                        (inputKey.input() == InputType.RIGHT_HOLD || inputKey.input() == InputType.SHIFT_HOLD)) {
                        dummy.addChild(inputKey, null, minHoldTime);
                    } else {
                        dummy.addChild(inputKey, null);
                    }
                    if (sameItemRequired != null) {
                        dummy.setSameItemRequired(sameItemRequired);
                    }
                    if (cancellable != null) {
                        dummy.setCancellable(cancellable);
                    }
                    if (display != null) {
                        dummy.setDisplay(display);
                    }
                }
                dummy = dummy.getChildByKey(inputKey);
                if (dummy == null) break; // Defensive null check to avoid NPE
            }
            if (dummy != null) {
                if (action != null) {
                    dummy.setAction(action);
                }
                if (timeoutTicks != null) {
                    dummy.setTimeoutTicks(timeoutTicks);
                }
                if (display != null) {
                    dummy.setDisplay(display);
                }
                if (visibleIf != null) {
                    dummy.setVisibleIf(visibleIf);
                }
            }
        }
    }

    /**
     * Convenience method to add a new input sequence to the tree at runtime using the builder.
     */
    public void addSequence(List<InputKey> sequence, Consumer<InputNodeBuilder> setup) {
        InputNodeBuilder builder = new InputNodeBuilder(root, sequence);
        setup.accept(builder);
        builder.build();
    }

}
