package btm.sword.system.input;

import btm.sword.Sword;
import btm.sword.system.action.AttackAction;
import btm.sword.system.action.MovementAction;
import btm.sword.system.action.type.AttackType;
import btm.sword.system.action.utility.GrabAction;
import btm.sword.system.action.utility.UtilityAction;
import btm.sword.system.action.utility.thrown.ThrowAction;
import btm.sword.system.entity.aspect.AspectType;
import btm.sword.system.entity.types.Combatant;
import java.util.HashMap;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

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

    private final InputNode root = new InputNode(null);

    private InputNode currentNode;
    private StringBuilder sequenceToDisplay;
    private BukkitTask timeoutTimer;
    private final long timeoutTicks;

    /**
     * Creates an InputExecutionTree with a specified timeout for input sequences.
     *
     * @param timeoutMillis timeout duration in milliseconds before sequence resets
     */
    public InputExecutionTree(long timeoutMillis) {
        currentNode = root;
        sequenceToDisplay = new StringBuilder();
        timeoutTimer = null;
        this.timeoutTicks = (long) (timeoutMillis * (0.02)); // 1/50 (or 0.02) is the conversion from milliseconds to ticks
    }

    /**
     * Processes an input step in the execution tree, updating current node and sequence.
     * Returns the new node reached or null if the input is invalid or sequence is reset.
     * Automatically resets sequence if current node is a leaf.
     *
     * @param input the {@link InputType} input to process
     * @return the resulting {@link InputNode} or null if invalid/reset
     */
    public InputNode step(InputType input) {
        stopTimeoutTimer();
        // before taking input, if it is known that the current node is a leaf, reset and take input from the root
        if (!hasChildren()) reset();

        // shouldn't happen often
        if (currentNode == null) {
            reset();
            return null;
        }

        // initialize a new node that points to the traversal of the input
        InputNode next = currentNode.getChild(input);

        if (next == null) {
            if (isAtRoot()) return null;
            else {
                reset();
                if (currentNode.isCancellable())
                    return step(input);

                else
                    return null;
            }
        }

        sequenceToDisplay.append(inputToString(input));

        // set the
        currentNode = next;

        if (hasChildren()) {
            sequenceToDisplay.append(" + ");
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
        }.runTaskLater(plugin, timeoutTicks);
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
        sequenceToDisplay = new StringBuilder();
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
     * @param input the {@link InputType} to check
     * @return true if a child node exists, false otherwise
     */
    public boolean nextExists(InputType input) {
        return currentNode.getChild(input) != null;
    }

    /**
     * Adds an input sequence mapping to an {@link InputAction} to the tree.
     * Overwrites existing paths or creates new nodes as needed.
     *
     * @param inputSequence list of {@link InputType}s representing the input combo
     * @param action the {@link InputAction} to associate with the final input
     * @param sameItemRequired whether all inputs require the same held item
     * @param cancellable whether this input sequence can be cancelled mid-way
     * @param display whether to display this input sequence progress to the player
     */
    public void add(List<InputType> inputSequence, InputAction action,
                    boolean sameItemRequired,
                    boolean cancellable,
                    boolean display) {
        InputNode dummy = root;
        for (InputType input : inputSequence) {
            if (dummy.noChild(input)) {
                dummy.addChild(input, null);
                dummy.setSameItemRequired(sameItemRequired);
                dummy.setCancellable(cancellable);
                dummy.setDisplay(display);
            }
            dummy = dummy.getChild(input);
        }
        dummy.setAction(action);
        dummy.setDisplay(display);
    }

    /**
     * Adds an input sequence mapping with a minimum hold time requirement for hold inputs.
     *
     * @param inputSequence list of {@link InputType}s representing the input combo
     * @param action associated {@link InputAction}
     * @param sameItemRequired whether the same item must be held for all inputs
     * @param cancellable whether this sequence is cancellable
     * @param display whether to display input combo progress
     * @param minHoldTime minimum hold time in milliseconds required for hold inputs to register
     */
    public void add(List<InputType> inputSequence, InputAction action,
                    boolean sameItemRequired,
                    boolean cancellable,
                    boolean display,
                    long minHoldTime) {
        InputNode dummy = root;
        for (InputType input : inputSequence) {
            if (dummy.noChild(input)) {
                if (input == InputType.RIGHT_HOLD || input == InputType.SHIFT_HOLD) {
                    dummy.addChild(input, null, minHoldTime);
                }
                else {
                    dummy.addChild(input, null);
                }
                dummy.setSameItemRequired(sameItemRequired);
                dummy.setCancellable(cancellable);
                dummy.setDisplay(display);
            }
            dummy = dummy.getChild(input);
        }
        dummy.setAction(action);
        dummy.setDisplay(display);
    }

    /**
     * Checks whether the current node has children nodes.
     *
     * @return true if children exist, false otherwise
     */
    public boolean hasChildren() {
        return !currentNode.children.isEmpty();
    }

    /**
     * Returns the string representation of the current input sequence for display.
     *
     * @return string representation of input sequence
     */
    @Override
    public String toString() {
        return sequenceToDisplay.toString();
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
            case RIGHT_TAP, SHIFT_TAP -> out = "_";
            case RIGHT_HOLD, SHIFT_HOLD -> out = "___";
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
    public long getMinHoldLengthOfNext(InputType holdType) {
        InputNode next = currentNode.getChild(holdType);
        if (next == null) return -1;
        return next.getMinHoldTime();
    }

    /**
     * Initializes the input tree with predefined combos and mapped {@link InputAction}s.
     * Sets up example combos for movement, grabbing, attacks, throwing, and utility actions.
     */
    public void initializeInputTree() {
        // Item independent actions:
        // dodge forward, dodge backward
        add(List.of(InputType.SWAP, InputType.SWAP),
                new InputAction(
                        executor -> MovementAction.dash(executor, true),
                        executor -> executor.calcCooldown(AspectType.CELERITY, 200L, 1000L, 10),
                        Combatant::canAirDash,
                        false,
                        true),
                false,
                true,
                true);

        add(List.of(InputType.SHIFT, InputType.SHIFT),
                new InputAction(
                        executor -> MovementAction.dash(executor, false),
                        executor -> executor.calcCooldown(AspectType.CELERITY, 200L, 1000L, 10),
                        Combatant::canAirDash,
                        false,
                        true),
                false,
                true,
                true);

        // grab
        add(List.of(InputType.SHIFT, InputType.LEFT),
                new InputAction(
                        GrabAction::grab,
                        executor -> executor.calcCooldown(AspectType.FORTITUDE, 200L, 1000L, 10),
                        Combatant::canPerformAction,
                        false,
                        true),
                false,
                true,
                true);


        // Item dependent actions:

        // basic attacks
        add(List.of(InputType.LEFT),
                new InputAction(
                        executor -> AttackAction.basicAttack(executor, AttackType.BASIC_1),
                        executor -> Math.max(0, (executor.getTimeOfLastAttack() + executor.getDurationOfLastAttack()) - System.currentTimeMillis()),
                        Combatant::canPerformAction,
                        false,
                        true),
                true,
                true,
                true);

        add(List.of(InputType.LEFT, InputType.LEFT),
                new InputAction(
                        executor -> AttackAction.basicAttack(executor, AttackType.BASIC_2),
                        executor -> 0L,
                        Combatant::canPerformAction,
                        false,
                        true),
                true,
                true,
                true);

        add(List.of(InputType.LEFT, InputType.LEFT, InputType.LEFT),
                new InputAction(
                        executor -> AttackAction.basicAttack(executor, AttackType.BASIC_3),
                        executor -> 0L,
                        Combatant::canPerformAction,
                        false,
                        true),
                true,
                true,
                true);

        // throw hold action
        add(List.of(InputType.DROP, InputType.RIGHT),
                new InputAction(
                        ThrowAction::throwReady,
                        executor -> 0L,
                        Combatant::canPerformAction,
                        false,
                        false),
                true,
                true,
                true);

        // throw
        add(List.of(InputType.DROP, InputType.RIGHT, InputType.RIGHT_HOLD),
                new InputAction(
                        ThrowAction::throwItem,
                        executor -> 0L,
                        Combatant::canPerformAction,
                        false,
                        false),
                true,
                true,
                true,
                600L);

        add(List.of(InputType.DROP, InputType.DROP),
                new InputAction(
                        UtilityAction::death,
                        executor -> 0L,
                        Combatant::canPerformAction,
                        true,
                        true),
                true,
                true,
                true);
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
        private final HashMap<InputType, InputNode> children = new HashMap<>();
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
         * @param input the {@link InputType} input triggering the child node
         * @param action the {@link InputAction} associated with the child node, or null
         */
        public void addChild(InputType input, InputAction action) {
            children.putIfAbsent(input, new InputNode(action));
        }

        /**
         * Adds a child node for the specified input with an associated action and minimum hold time.
         *
         * @param input the input triggering the child node
         * @param action the action associated with the child node
         * @param minHoldLength minimum hold time in ms required for this input
         */
        public void addChild(InputType input, InputAction action, long minHoldLength) {
            children.putIfAbsent(input, new InputNode(action, minHoldLength));
        }

        /**
         * Checks if there is no child node matching the specified input.
         *
         * @param input the input to check for child
         * @return true if no child exists for input, false otherwise
         */
        public boolean noChild(InputType input) {
            return !children.containsKey(input);
        }

        /**
         * Retrieves the child node corresponding to the specified input.
         *
         * @param input the input to get the child node for
         * @return the child {@link InputNode}, or null if none exists
         */
        public InputNode getChild(InputType input) {
            return children.get(input);
        }
    }
}
