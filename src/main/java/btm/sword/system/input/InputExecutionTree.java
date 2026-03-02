package btm.sword.system.input;

import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import btm.sword.config.Config;
import btm.sword.system.control.SwordScheduler;
import btm.sword.system.entity.impl.SwordPlayer;
import btm.sword.utility.SwordTimeUnit;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;

import javax.annotation.Nullable;

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

    @Getter
    private final InputNode root = new InputNode((ActionContextPair) null);
    @Getter
    private InputAction nextAction = null;
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
    public InputNode step(InputType input) {
        stopTimeoutTimer();
        potentialInputSelectionText = Component.text(""); // reset potential input before new step
        // before taking input, if it is known that the current node is a leaf, reset and take input from the root
        if (!hasChildren()) reset();

        if (currentNode == null) {
            reset();
            return null;
        }

        // initialize a new node that points to the traversal of the input
        InputNode next = currentNode.retrieveAvailableNode(input, owner);

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

        nextAction = next.resolveAction(owner);

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

        baseSequenceToDisplay = baseSequenceToDisplay.append(Component.text(inputToString(input),
            Config.SwordColor.TITLE_INPUT_STRING, TextDecoration.BOLD));

        currentNode = next;

        if (hasChildren()) {
            boolean alreadyAppended = false;

            Iterator<Map.Entry<InputType, InputNode>> it = currentNode.getChildren().entrySet().iterator();
            boolean inputAccepted = false;
            while (it.hasNext()) {
                Map.Entry<InputType, InputNode> entry = it.next();

                // Skip paths that are not visible or not accessible to this player in the current state
                if (entry.getValue().hiddenFor(owner)) {
                    continue;
                }
                else if (!alreadyAppended) {
                    baseSequenceToDisplay = baseSequenceToDisplay.append(Component.text(" ➞ ",
                        Config.SwordColor.TITLE_INPUT_STRING, TextDecoration.ITALIC));
                    alreadyAppended = true;
                }

                InputAction action = entry.getValue().resolveAction(owner);

                boolean ready;
                if (action == null) {
                    ready = !entry.getValue().getChildren().isEmpty();
                }
                else if (entry.getKey().equals(InputType.DROP)) {
                    ready = InputActionExecutor.canCast(action, owner) && !owner.getItemTypeInHand(true).isAir();
                }
                else {
                    ready = InputActionExecutor.canCast(action, owner);
                }

                potentialInputSelectionText = potentialInputSelectionText
                    .append(Component.text(inputToString(entry.getKey()),
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
     * @param input the {@link InputType} to check
     * @return true if a child node exists, false otherwise
     */
    public boolean nextExists(InputType input) {
        return currentNode.retrieveAvailableNode(input, owner) != null;
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
    public long getMinHoldLengthOfNext(InputType holdType) {
        InputNode next = currentNode.retrieveAvailableNode(holdType, owner);
        if (next == null) return -1;
        return next.getMinHoldTime();
    }

    public long timeoutTicks() {
        return currentNode.timeoutTicks;
    }

    /**
     * Returns whether movement (dash) inputs are enabled.
     * Movement inputs are always initialized at construction time via
     * {@link InputRegistrar#initializeMovementInputs(InputNode)}.
     *
     * @return always true
     */
    public boolean isMovementEnabled() {
        return true;
    }

    public record ActionContextPair(Supplier<InputAction> action, Predicate<SwordPlayer> context) { }

    /**
     * Represents a node in the {@link InputExecutionTree}, used to map input sequences to actions.
     * Each node maintains children inputs leading to subsequent nodes,
     * and stores metadata like whether the sequence requires same item, is cancellable, or displayable.
     * <p>
     * For static actions the {@link InputAction} is created once and cached so that
     * {@code timeLastExecuted} persists correctly across executions, enabling cooldowns to work.
     * Nodes marked {@code dynamic = true} re-invoke their supplier on every call — used for
     * skill-slot nodes whose resolved action changes when skills are re-equipped.
     * </p>
     */
    @Getter
    @Setter
    public static class InputNode {
        /**
         * When adding entries, note that order of insertion creates an implicit priority system.
         * Add higher priority context + action 's first.
         */
        @lombok.Setter(lombok.AccessLevel.NONE)
        private LinkedList<ActionContextPair> actions;
        /** Cached InputAction for non-dynamic single-action nodes. Invalidated when the action supplier changes. */
        private transient InputAction cachedAction;
        /** Per-pair cache for multi-action non-dynamic nodes. Keyed by identity. */
        private transient Map<ActionContextPair, InputAction> pairCache;
        /**
         * When true, the action supplier is invoked on every {@link #resolveAction(SwordPlayer)} call
         * (for skill-slot nodes that depend on the currently equipped skill).
         * When false (default), the result is cached after the first call.
         */
        private boolean dynamic = false;
        private Consumer<SwordPlayer> internalAction;
        private int timeoutTicks;
        private final LinkedHashMap<InputType, InputNode> children = new LinkedHashMap<>();
        private boolean sameItemRequired;
        private boolean cancellable;
        private boolean display;
        private final int minHoldTime;

        /**
         * Constructs an InputNode with an associated action list and hold time.
         *
         * @param actions the associated Linked List of {@link ActionContextPair}'s, or null if none
         * @param minHoldTime minimum hold time in milliseconds for hold inputs, or -1 if none
         */
        public InputNode(LinkedList<ActionContextPair> actions, int minHoldTime) {
            this.actions = actions;
            this.minHoldTime = minHoldTime;
            this.timeoutTicks = 20;
        }

        /**
         * Constructs an InputNode with a single action and no hold time limit.
         *
         * @param action the associated {@link ActionContextPair}, or null if none
         */
        public InputNode(ActionContextPair action) {
            this(action != null ? new LinkedList<>(List.of(action)) : null, -1);
        }

        /**
         * Sets the full action list and clears all caches.
         *
         * @param actions the new action list
         */
        public void setActions(LinkedList<ActionContextPair> actions) {
            this.actions = actions;
            this.cachedAction = null;
            this.pairCache = null;
        }

        /**
         * Resolves and returns the {@link InputAction} for this node given the player's current state.
         * <p>
         * Iterates {@link #actions} in insertion order and returns the first pair whose
         * {@link ActionContextPair#context()} predicate passes. Single-action non-dynamic nodes
         * cache their result in {@link #cachedAction} to preserve cooldown state. Multi-action
         * or dynamic nodes use a per-pair {@link IdentityHashMap} cache for the same reason.
         * </p>
         *
         * @param owner the player to evaluate context predicates against
         * @return the resolved InputAction, or null if no action is set or no context passes
         */
        public InputAction resolveAction(SwordPlayer owner) {
            owner.message("Resolving an action");

            if (actions == null || actions.isEmpty()) return null;
            if (!dynamic && actions.size() == 1) {
                ActionContextPair pair = actions.getFirst();
                if (!pair.context().test(owner)) return null;
                if (cachedAction == null) cachedAction = pair.action().get();
                return cachedAction;
            }
            for (ActionContextPair pair : actions) {
                if (pair.context().test(owner)) {
                    if (dynamic) return pair.action().get();
                    if (pairCache == null) pairCache = new IdentityHashMap<>();
                    return pairCache.computeIfAbsent(pair, p -> p.action().get());
                }
            }
            return null;
        }

        /**
         * Adds a child node for the specified input with no hold time.
         *
         * @param input the {@link InputType} input triggering the child node
         * @param action unused parameter kept for API compatibility; node starts with null actions
         */
        public void addChild(InputType input, ActionContextPair action) {
            children.putIfAbsent(input, new InputNode(action));
        }

        /**
         * Adds a child node for the specified input with a minimum hold time.
         *
         * @param input the input triggering the child node
         * @param action unused parameter kept for API compatibility; node starts with null actions
         * @param minHoldLength minimum hold time in ms required for this input
         */
        public void addChild(InputType input, @Nullable ActionContextPair action, int minHoldLength) {
            InputNode child = children.get(input);
            if (child != null && child.actions != null) {
                child.actions.add(action);
                return;
            }
            children.putIfAbsent(input,
                new InputNode(action == null ? null : new LinkedList<>(List.of(action)), minHoldLength)
            );
        }

        /**
         * Finds a child node by a concrete InputType and owner. This is the runtime path resolver
         * that takes into account predicates attached to keys or nodes.
         *
         * @param input the key to look up
         * @param owner the player for predicate evaluation
         * @return the matching child node, or null if not found
         */
        public InputNode retrieveAvailableNode(InputType input, SwordPlayer owner) {
            for (Map.Entry<InputType, InputNode> entry : children.entrySet()) {
                if (!entry.getKey().equals(input)) continue;
                // node-level visibility
                if (entry.getValue().hiddenFor(owner)) continue;
                return entry.getValue();
            }
            return null;
        }

        /**
         * Gets a child node by exact key reference (used by the builder during construction).
         *
         * @param key the exact key reference to look up
         * @return the child node, or null if not found
         */
        public InputNode getChildByKey(InputType key) {
            return children.get(key);
        }

        /**
         * Returns true if no existing child has the same {@link InputType} as {@code input}.
         * Since all paths with the same InputType now share a single node (with multiple
         * context-gated actions), a second registration for the same InputType is intentional —
         * it will append to the existing node's action list via {@link #addAction(ActionContextPair)}.
         *
         * @param input the key to check for matching children
         * @return true if no child with this InputType exists
         */
        public boolean noMatchingChild(InputType input) {
            for (InputType k : children.keySet()) {
                if (k.equals(input)) return false;
            }
            return true;
        }

        /**
         * Returns true if this node has no passing context for the given player,
         * meaning it should not be shown in the HUD display.
         *
         * @param player the player to evaluate
         * @return true if all contexts fail (node is hidden), false if any context passes
         */
        public boolean hiddenFor(SwordPlayer player) {
            if (actions == null || actions.isEmpty()) return false;
            for (ActionContextPair pair : actions) {
                if (pair.context().test(player)) return false;
            }
            return true;
        }
    }

    /**
     * Builder for registering input sequences into an {@link InputExecutionTree}.
     */
    public static class InputNodeBuilder {
        private final List<InputType> inputSequence;
        private final InputNode root;

        private LinkedList<ActionContextPair> actions;
        private Integer timeoutTicks = null;
        private Integer minHoldTime = null;
        private Boolean sameItemRequired = null;
        private Boolean cancellable = null;
        private Boolean display = null;
        private Boolean dynamic = null;

        public InputNodeBuilder(InputNode root, List<InputType> inputSequence) {
            this.root = root;
            this.inputSequence = inputSequence;
        }

        public InputNodeBuilder action(ActionContextPair action) {
            this.actions = new LinkedList<>(List.of(action));
            return this;
        }

        public InputNodeBuilder action(LinkedList<ActionContextPair> actions) {
            this.actions = actions;
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

        /**
         * Marks the terminal node as dynamic, meaning its action supplier is re-invoked on every
         * {@link InputNode#resolveAction(SwordPlayer)} call rather than being cached.
         * <p>
         * Use this for skill-slot nodes whose action changes when the player re-equips a skill.
         * Dynamic nodes do NOT persist {@code timeLastExecuted} — skill-level cooldown tracking
         * must be handled on the skill itself when needed.
         * </p>
         *
         * @param dynamic true to disable action caching on the leaf node
         * @return this builder
         */
        public InputNodeBuilder dynamic(boolean dynamic) {
            this.dynamic = dynamic;
            return this;
        }

        /**
         * Builds and registers the input sequence into the tree.
         * If the leaf node already exists (shared path), the action is appended to its list.
         */
        public void build() {
            InputNode dummy = root;
            for (InputType input : inputSequence) {
                if (dummy.noMatchingChild(input)) {
                    if (minHoldTime != null &&
                        (input == InputType.RIGHT_HOLD || input == InputType.SHIFT_HOLD)) {
                        dummy.addChild(input, null, minHoldTime);
                    } else {
                        dummy.addChild(input, null);
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
                dummy = dummy.getChildByKey(input);
                if (dummy == null) break; // Defensive null check to avoid NPE
            }
            if (dummy != null) {
                if (actions != null) {
                    dummy.setActions(actions);
                }
                if (timeoutTicks != null) {
                    dummy.setTimeoutTicks(timeoutTicks);
                }
                if (display != null) {
                    dummy.setDisplay(display);
                }
                if (dynamic != null) {
                    dummy.setDynamic(dynamic);
                }
            }
        }
    }
}
