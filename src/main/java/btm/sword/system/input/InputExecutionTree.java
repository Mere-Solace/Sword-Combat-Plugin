package btm.sword.system.input;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import btm.sword.Sword;
import btm.sword.config.Config;
import btm.sword.system.action.attack.AttackAction;
import btm.sword.system.action.attack.DashAttackAction;
import btm.sword.system.action.movement.DashDirection;
import btm.sword.system.action.movement.MovementAction;
import btm.sword.system.action.skill.UmbralBladeAction;
import btm.sword.system.action.throwing.ThrowAction;
import btm.sword.system.action.utility.GrabAction;
import btm.sword.system.action.utility.UtilityAction;
import btm.sword.system.entity.aspect.AspectType;
import btm.sword.system.entity.types.Combatant;
import btm.sword.system.entity.types.SwordPlayer;
import btm.sword.system.item.SwordItemType;
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
    private static final Plugin plugin = Sword.getInstance();

    private static final int MAX_ATTEMPT_ITERATIONS_BEFORE_FORCE_RESET = 3;
    private int currentAttempts = 0;

    private final InputNode root = new InputNode(null);
    private final SwordPlayer owner;

    private InputNode currentNode;
    private Component baseSequenceToDisplay;
    private Component potentialInputSelectionText;
    private BukkitTask timeoutTimer;

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
        InputNode next = currentNode.findMatchingChild(inputKey);

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

        if (next.action != null && !next.action.canCast(owner)) {
            currentAttempts++;
            if (currentAttempts >= MAX_ATTEMPT_ITERATIONS_BEFORE_FORCE_RESET) {
                reset(); // reset if there is a dead-lock state and the user is spamming
            }
            else {
                return null; // added this so that stepping forward cannot occur
            }
        }
        currentAttempts = 0;

        baseSequenceToDisplay = baseSequenceToDisplay.append(Component.text(inputToString(inputKey.input()),
            Config.SwordColor.TITLE_INPUT_STRING, TextDecoration.BOLD));

        currentNode = next;

        if (hasChildren()) {
            baseSequenceToDisplay = baseSequenceToDisplay.append(Component.text(" ➞ ",
                Config.SwordColor.TITLE_INPUT_STRING, TextDecoration.ITALIC));

            Iterator<Map.Entry<InputKey, InputNode>> it = currentNode.getChildren().entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<InputKey, InputNode> entry = it.next();
                InputAction action = entry.getValue().getAction();

                boolean ready;
                if (action == null) {
                    ready = true;
                }
                else if (entry.getKey().input().equals(InputType.DROP)) {
                    ready = action.canCast(owner) &&
                        owner.getAspects().soulfireCur() >= action.getRequiredSoulfire() &&
                        !owner.getItemTypeInHand(true).isAir();
                }
                else {
                    ready = action.canCast(owner) &&
                        owner.getAspects().soulfireCur() >= action.getRequiredSoulfire();
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
            }
            startTimeoutTimer();
        }

        return next;
    }

    /**
     * Starts the timeout countdown to reset the input sequence after inactivity.
     */
    private void startTimeoutTimer() {
        timeoutTimer = new BukkitRunnable() {
            @Override
            public void run() {
                reset();
            }
        }.runTaskLater(plugin, currentNode.getTimeoutTicks()); // use the current node's timeoutTicks (base = 20L); 1 second
    }

    /**
     * Stops the currently running timeout timer task.
     */
    public void stopTimeoutTimer() {
        if (timeoutTimer != null && !timeoutTimer.isCancelled()) timeoutTimer.cancel();
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
        return currentNode.findMatchingChild(inputKey) != null;
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
        String out;
        switch (type) {
            case LEFT -> out = "L";
            case RIGHT -> out = "R";
            case RIGHT_TAP, SHIFT_TAP -> out = "•";
            case RIGHT_HOLD, SHIFT_HOLD -> out = "▁▁";
            case DROP -> out = "D";
            case SWAP -> out = "F";
            case SHIFT -> out = "S";
            default -> out = "*";
        }
        return out;
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
        InputNode next = currentNode.findMatchingChild(holdType);
        if (next == null) return -1;
        return next.getMinHoldTime();
    }

    public long timeoutTicks() {
        return currentNode.timeoutTicks;
    }

    /**
     * Initializes the input tree with predefined combos and mapped {@link InputAction}s.
     * Sets up combos for movement, grabbing, attacks, throwing, and utility actions.
     * <p>
     * <b>System Overview:</b>
     * This method builds a finite state tree where each {@link InputKey} represents an input type + allowed item types.
     * The tree uses {@link LinkedHashMap} to preserve insertion order, so <b>specific item types are checked before generic ones</b>.
     * </p>
     *
     * <b>Entry Addition Rules:</b>
     * <ul>
     *   <li><b>Specific → Generic Order:</b> Add item-specific entries FIRST, generic/ANY entries LAST
     *     <pre>{@code
     *     // CORRECT - Specific first
     *     add(List.of(InputKey.of(InputType.LEFT, SwordItemType.SHORT_SWORD)), ...);
     *     add(List.of(InputKey.of(InputType.LEFT)), ...); // ANY implied
     *
     *     // WRONG - Generic blocks specific
     *     add(List.of(InputKey.of(InputType.LEFT)), ...);
     *     add(List.of(InputKey.of(InputType.LEFT, SwordItemType.SHORT_SWORD)), ...);
     *     }</pre>
     *   </li>
     *
     *   <li><b>No Overlapping Allowables:</b> Don't create entries where multiple nodes accept the same (input, itemType) combination
     *     <pre>{@code
     *     // BAD - Both accept SHORT_SWORD.LEFT
     *     add(List.of(InputKey.of(InputType.LEFT, SwordItemType.SHORT_SWORD, SwordItemType.BROAD_SWORD)), ...);
     *     add(List.of(InputKey.of(InputType.LEFT, SwordItemType.SHORT_SWORD)), ...);
     *     }</pre>
     *   </li>
     *
     *   <li><b>findMatchingChild Priority:</b> Iterates children in insertion order, returns first match where:
     *     {@code nodeKey.input() == inputKey.input() && nodeKey.allowed(playerItemType)}
     *   </li>
     * </ul>
     *
     * <b>Example Priority Chain:</b>
     * <pre>
     * children order: [SHORT_SWORD.LEFT, BROAD_SWORD.LEFT, ANY.LEFT]
     * player holds SHORT_SWORD → matches SHORT_SWORD.LEFT (first hit)
     * player holds BROAD_SWORD → matches BROAD_SWORD.LEFT (second hit)
     * player holds ZWEIHANDER → matches ANY.LEFT (last fallback)
     * </pre>
     *
     * <b>Current Structure:</b>
     * <ul>
     *   <li>Item-independent: Movement, grabs (ANY implied)</li>
     *   <li>UMBRAL_LINK specific attacks (FIRST)</li>
     *   <li>Generic sword attacks (SECOND - catch-all)</li>
     *   <li>Umbral blade actions (specific item requirements handled by canPerformUmbralAction)</li>
     * </ul>
     */
    public void initializeInputTree() {
        // Item independent actions:
        // dodge forward, dodge backward
        new InputNodeBuilder(root, List.of(
            InputKey.of(InputType.SWAP),
            InputKey.of(InputType.SWAP)
        )).action(new InputAction(
                executor -> MovementAction.dash(executor, DashDirection.FORWARD),
                executor -> executor.calcCooldown(AspectType.CELERITY, 200L, 1000L, 10),
                Combatant::canAirDash,
                5f,
                false,
                true,
                true))
            .timeoutTicks(7L)
            .sameItemRequired(false)
            .cancellable(true)
            .display(true)
            .build();

        new InputNodeBuilder(root, List.of(
            InputKey.of(InputType.SWAP),
            InputKey.of(InputType.SWAP),
            InputKey.of(InputType.LEFT, SwordItemType.UMBRAL_LINK)
        )).action(new InputAction(
                UmbralBladeAction::shadowBlink,
                executor -> 1000L,
                Combatant::canPerformShadowBlink,
                false,
                true,
                true))
            .sameItemRequired(false)
            .cancellable(true)
            .display(true)
            .build();

        new InputNodeBuilder(root, List.of(
            InputKey.of(InputType.SWAP),
            InputKey.of(InputType.SWAP),
            InputKey.of(InputType.LEFT)
        )).action(new InputAction(
                executor -> DashAttackAction.dashAttack(executor, DashDirection.FORWARD),
                executor -> 0L,
                Combatant::canPerformAction,
                false,
                true,
                true))
            .timeoutTicks(3L)
            .sameItemRequired(false)
            .cancellable(true)
            .display(true)
            .build();

        new InputNodeBuilder(root, List.of(
            InputKey.of(InputType.SHIFT),
            InputKey.of(InputType.SHIFT)
        )).action(new InputAction(
                executor -> MovementAction.dash(executor, DashDirection.BACKWARD),
                executor -> executor.calcCooldown(AspectType.CELERITY, 200L, 1000L, 10),
                Combatant::canAirDash,
                5f,
                false,
                true,
                true))
            .timeoutTicks(7L)
            .sameItemRequired(false)
            .cancellable(true)
            .display(true)
            .build();

        new InputNodeBuilder(root, List.of(
            InputKey.of(InputType.SHIFT),
            InputKey.of(InputType.SHIFT),
            InputKey.of(InputType.LEFT)
        )).action(new InputAction(
                executor -> DashAttackAction.dashAttack(executor, DashDirection.BACKWARD),
                executor -> 0L,
                Combatant::canPerformAction,
                false,
                true,
                true))
            .timeoutTicks(3L)
            .sameItemRequired(false)
            .cancellable(true)
            .display(true)
            .build();

        // strafe
        new InputNodeBuilder(root, List.of(
            InputKey.of(InputType.SWAP),
            InputKey.of(InputType.RIGHT)
        )).action(new InputAction(
                executor -> MovementAction.dash(executor, DashDirection.RIGHT),
                executor -> executor.calcCooldown(AspectType.CELERITY, 200L, 1000L, 10),
                Combatant::canStrafe,
                5f,
                false,
                true,
                true))
            .timeoutTicks(7L)
            .sameItemRequired(false)
            .cancellable(true)
            .display(true)
            .build();

        // strafe attack right
        new InputNodeBuilder(root, List.of(
            InputKey.of(InputType.SWAP),
            InputKey.of(InputType.RIGHT),
            InputKey.of(InputType.LEFT)
        )).action(new InputAction(
                executor -> DashAttackAction.dashAttack(executor, DashDirection.RIGHT),
                executor -> 0L,
                Combatant::canPerformAction,
                false,
                true,
                true))
            .sameItemRequired(false)
            .cancellable(true)
            .display(true)
            .build();

        new InputNodeBuilder(root, List.of(
            InputKey.of(InputType.SWAP),
            InputKey.of(InputType.LEFT)
        )).action(new InputAction(
                executor -> MovementAction.dash(executor, DashDirection.LEFT),
                executor -> executor.calcCooldown(AspectType.CELERITY, 200L, 1000L, 10),
                Combatant::canStrafe,
                5f,
                false,
                true,
                true))
            .timeoutTicks(7L)
            .sameItemRequired(false)
            .cancellable(true)
            .display(true)
            .build();

        // strafe attack left
        new InputNodeBuilder(root, List.of(
            InputKey.of(InputType.SWAP),
            InputKey.of(InputType.LEFT),
            InputKey.of(InputType.LEFT)
        )).action(new InputAction(
                executor -> DashAttackAction.dashAttack(executor, DashDirection.RIGHT),
                executor -> 0L,
                Combatant::canPerformAction,
                false,
                true,
                true))
            .sameItemRequired(false)
            .cancellable(true)
            .display(true)
            .build();

        // grab
        new InputNodeBuilder(root, List.of(
            InputKey.of(InputType.SHIFT),
            InputKey.of(InputType.LEFT)
        )).action(new InputAction(
                GrabAction::grab,
                executor -> executor.calcCooldown(AspectType.FORTITUDE, 200L, 1000L, 10),
                Combatant::canPerformAction,
                2f,
                false,
                true,
                true))
            .timeoutTicks(20L)
            .sameItemRequired(false)
            .cancellable(true)
            .display(true)
            .build();

        // Item dependent actions:

        // basic umbral attacks
        new InputNodeBuilder(root, List.of(
            InputKey.of(InputType.LEFT, SwordItemType.UMBRAL_LINK)
        )).action(new InputAction(
                executor -> UmbralBladeAction.basicAttackWithLink(executor, 1),
                executor -> Math.max(0, (executor.getTimeOfLastAttack() + executor.getDurationOfLastAttack()) - System.currentTimeMillis()),
                Combatant::canPerformUmbralLinkAttack,
                true,
                true,
                true))
            .timeoutTicks(60L)
            .sameItemRequired(true)
            .cancellable(true)
            .display(true)
            .build();

        new InputNodeBuilder(root, List.of(
            InputKey.of(InputType.LEFT, SwordItemType.UMBRAL_LINK),
            InputKey.of(InputType.LEFT, SwordItemType.UMBRAL_LINK)
        )).action(new InputAction(
                executor -> UmbralBladeAction.basicAttackWithLink(executor, 2),
                executor -> 0L,
                Combatant::canPerformUmbralLinkAttack,
                true,
                true,
                true))
            .timeoutTicks(60L)
            .sameItemRequired(true)
            .cancellable(true)
            .display(true)
            .build();

        new InputNodeBuilder(root, List.of(
            InputKey.of(InputType.LEFT, SwordItemType.UMBRAL_LINK),
            InputKey.of(InputType.LEFT, SwordItemType.UMBRAL_LINK),
            InputKey.of(InputType.LEFT, SwordItemType.UMBRAL_LINK)
        )).action(new InputAction(
                executor -> UmbralBladeAction.basicAttackWithLink(executor, 3),
                executor -> 0L,
                Combatant::canPerformUmbralLinkAttack,
                true,
                true,
                false))
            .timeoutTicks(60L)
            .sameItemRequired(true)
            .cancellable(true)
            .display(true)
            .build();

        // basic attacks
        new InputNodeBuilder(root, List.of(
            InputKey.of(InputType.LEFT)
        )).action(new InputAction(
                executor -> AttackAction.basicAttack(executor, 1),
                executor -> Math.max(0, (executor.getTimeOfLastAttack() + executor.getDurationOfLastAttack()) - System.currentTimeMillis()),
                Combatant::canPerformAction,
                true,
                true,
                false))
            .sameItemRequired(true)
            .cancellable(true)
            .display(true)
            .build();

        new InputNodeBuilder(root, List.of(
            InputKey.of(InputType.LEFT),
            InputKey.of(InputType.LEFT)
        )).action(new InputAction(
                executor -> AttackAction.basicAttack(executor, 2),
                executor -> 0L,
                Combatant::canPerformAction,
                true,
                true,
                false))
            .sameItemRequired(true)
            .cancellable(true)
            .display(true)
            .build();

        new InputNodeBuilder(root, List.of(
            InputKey.of(InputType.LEFT),
            InputKey.of(InputType.LEFT),
            InputKey.of(InputType.LEFT)
        )).action(new InputAction(
                executor -> AttackAction.basicAttack(executor, 3),
                executor -> 0L,
                Combatant::canPerformAction,
                true,
                true,
                false))
            .sameItemRequired(true)
            .cancellable(true)
            .display(true)
            .build();

        // throw hold action
        new InputNodeBuilder(root, List.of(
            InputKey.of(InputType.DROP),
            InputKey.of(InputType.RIGHT)
        )).action(new InputAction(
                ThrowAction::throwReady,
                executor -> 0L,
                Combatant::canPerformAction,
                false,
                false,
                true))
            .sameItemRequired(true)
            .cancellable(true)
            .display(true)
            .build();

        // throw
        new InputNodeBuilder(root, List.of(
            InputKey.of(InputType.DROP),
            InputKey.of(InputType.RIGHT),
            InputKey.of(InputType.RIGHT_HOLD)
        )).action(new InputAction(
                ThrowAction::throwItem,
                executor -> 0L,
                Combatant::canPerformAction,
                false,
                false,
                true))
            .minHoldTime(600L)
            .sameItemRequired(true)
            .cancellable(true)
            .display(true)
            .build();

        // just in case
        new InputNodeBuilder(root, List.of(
            InputKey.of(InputType.DROP),
            InputKey.of(InputType.DROP)
        )).action(new InputAction(
                UtilityAction::death,
                executor -> 0L,
                Combatant::canPerformAction,
                true,
                true,
                true))
            .sameItemRequired(true)
            .cancellable(true)
            .display(true)
            .build();

        // Slow em down
        new InputNodeBuilder(root, List.of(
            InputKey.of(InputType.RIGHT),
            InputKey.of(InputType.RIGHT_HOLD),
            InputKey.of(InputType.DROP)
        )).action(new InputAction(
                UtilityAction::bulletTime,
                executor -> 5000L,
                Combatant::canPerformAction,
                70f,
                true,
                true,
                true))
            .minHoldTime(500L)
            .sameItemRequired(true)
            .cancellable(true)
            .display(true)
            .build();

        // umbral blade toggling and wielding
        new InputNodeBuilder(root, List.of(
            InputKey.of(InputType.SHIFT),
            InputKey.of(InputType.DROP)
        )).action(new InputAction(
                UmbralBladeAction::toggle,
                executor -> 400L,
                Combatant::canPerformAction,
                true,
                true,
                true))
            .sameItemRequired(true)
            .cancellable(true)
            .display(true)
            .build();

        new InputNodeBuilder(root, List.of(
            InputKey.of(InputType.SHIFT),
            InputKey.of(InputType.SWAP)
        )).action(new InputAction(
                UmbralBladeAction::wield,
                executor -> 400L,
                Combatant::canPerformAction,
                true,
                true,
                true))
            .sameItemRequired(true)
            .cancellable(true)
            .display(true)
            .build();

        // heavy sweeps (umbral attacks)
        new InputNodeBuilder(root, List.of(
            InputKey.of(InputType.RIGHT),
            InputKey.of(InputType.RIGHT_TAP),
            InputKey.of(InputType.LEFT)
        )).action(new InputAction(
                UmbralBladeAction::sweep,
                executor -> 4000L,
                Combatant::canPerformUmbralAction,
                10f,
                true,
                true,
                false))
            .timeoutTicks(100L)
            .sameItemRequired(true)
            .cancellable(true)
            .display(true)
            .build();

        new InputNodeBuilder(root, List.of(
            InputKey.of(InputType.RIGHT),
            InputKey.of(InputType.RIGHT_TAP),
            InputKey.of(InputType.LEFT),
            InputKey.of(InputType.LEFT)
        )).action(new InputAction(
                UmbralBladeAction::sweep,
                executor -> 0L,
                Combatant::canPerformUmbralAction,
                15f,
                true,
                true,
                false))
            .timeoutTicks(100L)
            .sameItemRequired(true)
            .cancellable(true)
            .display(true)
            .build();

        new InputNodeBuilder(root, List.of(
            InputKey.of(InputType.RIGHT),
            InputKey.of(InputType.RIGHT_TAP),
            InputKey.of(InputType.LEFT),
            InputKey.of(InputType.LEFT),
            InputKey.of(InputType.LEFT)
        )).action(new InputAction(
                UmbralBladeAction::sweep,
                executor -> 0L,
                Combatant::canPerformUmbralAction,
                25f,
                true,
                true,
                false))
            .timeoutTicks(100L)
            .sameItemRequired(true)
            .cancellable(true)
            .display(true)
            .build();

        // lunges (umbral throw)
        new InputNodeBuilder(root, List.of(
            InputKey.of(InputType.RIGHT),
            InputKey.of(InputType.RIGHT_HOLD),
            InputKey.of(InputType.LEFT)
        )).action(new InputAction(
                UmbralBladeAction::lunge,
                executor -> 4000L,
                Combatant::canPerformUmbralAction,
                10f,
                true,
                true,
                false))
            .timeoutTicks(100L)
            .sameItemRequired(true)
            .cancellable(true)
            .display(true)
            .build();

        new InputNodeBuilder(root, List.of(
            InputKey.of(InputType.RIGHT),
            InputKey.of(InputType.RIGHT_HOLD),
            InputKey.of(InputType.LEFT),
            InputKey.of(InputType.LEFT)
        )).action(new InputAction(
                UmbralBladeAction::lunge,
                executor -> 0L,
                Combatant::canPerformUmbralAction,
                15f,
                true,
                true,
                false))
            .timeoutTicks(100L)
            .sameItemRequired(true)
            .cancellable(true)
            .display(true)
            .build();

        new InputNodeBuilder(root, List.of(
            InputKey.of(InputType.RIGHT),
            InputKey.of(InputType.RIGHT_HOLD),
            InputKey.of(InputType.LEFT),
            InputKey.of(InputType.LEFT),
            InputKey.of(InputType.LEFT)
        )).action(new InputAction(
                UmbralBladeAction::lunge,
                executor -> 0L,
                Combatant::canPerformUmbralAction,
                25f,
                true,
                true,
                false))
            .timeoutTicks(100L)
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
        private InputAction action;
        private Consumer<SwordPlayer> internalAction;
        private long timeoutTicks;
        private final LinkedHashMap<InputKey, InputNode> children = new LinkedHashMap<>();
        private boolean sameItemRequired;
        private boolean cancellable;
        private boolean display;
        private final long minHoldTime;

        /**
         * Constructs an InputNode with an associated action and hold time.
         *
         * @param action the associated {@link InputAction}, or null if none
         * @param minHoldTime minimum hold time in milliseconds for hold inputs, or -1 if none
         */
        public InputNode(InputAction action, long minHoldTime) {
            this.action = action;
            this.minHoldTime = minHoldTime;
            this.timeoutTicks = 20L;
        }

        /**
         * Constructs an InputNode with an associated action and no hold time limit.
         *
         * @param action the associated {@link InputAction}, or null if none
         */
        public InputNode(InputAction action) {
            this(action, -1);
        }

        /**
         * Adds a child node for the specified input with an associated action and no hold time.
         *
         * @param inputKey the {@link InputType} input triggering the child node
         * @param action the {@link InputAction} associated with the child node, or null
         */
        public void addChild(InputKey inputKey, InputAction action) {
            children.putIfAbsent(inputKey, new InputNode(action));
        }

        /**
         * Adds a child node for the specified input with an associated action and minimum hold time.
         *
         * @param inputKey the input triggering the child node
         * @param action the action associated with the child node
         * @param minHoldLength minimum hold time in ms required for this input
         */
        public void addChild(InputKey inputKey, InputAction action, long minHoldLength) {
            children.putIfAbsent(inputKey, new InputNode(action, minHoldLength));
        }

        /**
         * Finds child node where input matches AND player's item type is allowed by node's InputKey.
         */
        public InputNode findMatchingChild(InputKey inputKey) {
            // The input key being passed in will always only have 1 element
            SwordItemType playerItemType = inputKey.allowedItemTypes().getFirst();

            for (Map.Entry<InputKey, InputNode> entry : children.entrySet()) {
                if (entry.getKey().input().equals(inputKey.input()) &&
                    entry.getKey().allowed(playerItemType)) {
                    return entry.getValue();
                }
            }
            return null;
        }

        /**
         * Checks if any child accepts the player's item type.
         */
        public boolean noMatchingChild(InputKey inputKey) {
            return findMatchingChild(inputKey) == null;
        }
    }

    public static class InputNodeBuilder {
        private final List<InputKey> inputSequence;
        private final InputNode root;

        private InputAction action;
        private Consumer<SwordPlayer> internalAction;
        private Long timeoutTicks = null;
        private Long minHoldTime = null;
        private Boolean sameItemRequired = null;
        private Boolean cancellable = null;
        private Boolean display = null;

        public InputNodeBuilder(InputNode root, List<InputKey> inputSequence) {
            this.root = root;
            this.inputSequence = inputSequence;
        }

        public InputNodeBuilder action(InputAction action) {
            this.action = action;
            return this;
        }

        public InputNodeBuilder internalAction(Consumer<SwordPlayer> internalAction) {
            this.internalAction = internalAction;
            return this;
        }

        public InputNodeBuilder timeoutTicks(long timeoutTicks) {
            this.timeoutTicks = timeoutTicks;
            return this;
        }

        public InputNodeBuilder minHoldTime(long minHoldTime) {
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
                dummy = dummy.findMatchingChild(inputKey);
                if (dummy == null) break; // Defensive null check to avoid NPE
            }
            if (dummy != null) {
                if (action != null) {
                    dummy.setAction(action);
                }
                if (internalAction != null) {
                    dummy.setInternalAction(internalAction);
                }
                if (timeoutTicks != null) {
                    dummy.setTimeoutTicks(timeoutTicks);
                }
                if (display != null) {
                    dummy.setDisplay(display);
                }
            }
        }
    }

}
