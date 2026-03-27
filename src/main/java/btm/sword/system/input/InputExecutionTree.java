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

import javax.annotation.Nullable;

import btm.sword.config.Config;
import btm.sword.system.control.SwordScheduler;
import btm.sword.system.entity.impl.SwordPlayer;
import btm.sword.utility.Debug;
import btm.sword.utility.SwordTimeUnit;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
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

    @Getter
    private final InputNode root = new InputNode(null);
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

        if (!hasChildren()) return next;

        boolean alreadyAppendedArrow = false;

        Iterator<Map.Entry<InputType, InputNode>> it = currentNode.getChildren().entrySet().iterator();
        boolean inputAccepted = false;
        while (it.hasNext()) { // iterate through all children nodes
            Map.Entry<InputType, InputNode> entry = it.next();

            // Skip paths that are not visible or not accessible to this player in the current state
            if (entry.getValue().hiddenFor(owner)) {
                continue;
            }
            else if (!alreadyAppendedArrow) {
                baseSequenceToDisplay = baseSequenceToDisplay.append(Component.text(" ➞ ",
                    Config.SwordColor.TITLE_INPUT_STRING, TextDecoration.ITALIC));
                alreadyAppendedArrow = true;
            }

            InputAction action = entry.getValue().resolveAction(owner);

            InputActionExecutor.ReadinessState readiness;

            if (action != null) {
                if (entry.getKey().equals(InputType.DROP) && owner.getItemTypeInHand(true).isAir()) {
                    readiness = InputActionExecutor.ReadinessState.DISABLED;
                }
                else {
                    readiness = InputActionExecutor.readiness(action, owner);
                }
            }
            else {
                // Pure prefix node (no own actions): navigable if it has children.
                // Leaf node whose context predicates all failed: nothing to cast → DISABLED.
                boolean isPurePrefix = entry.getValue().getActions() == null
                    || entry.getValue().getActions().isEmpty();
                readiness = (isPurePrefix && !entry.getValue().getChildren().isEmpty())
                    ? InputActionExecutor.ReadinessState.READY
                    : InputActionExecutor.ReadinessState.DISABLED;
            }

            TextColor hudColor = switch (readiness) {
                case READY -> Config.SwordColor.TEXT_ITEM_BASE;
                case ON_COOLDOWN -> {
                    long elapsed = System.currentTimeMillis() - action.getTimeLastExecuted();
                    long total = action.calcCooldown(owner);
                    float t = total > 0 ? Math.min(1f, (float) elapsed / total) : 1f;
                    yield cooldownColor(t);
                }
                case INSUFFICIENT_SOULFIRE -> {
                    float required = action.getRequiredSoulfire(owner);
                    float t = required > 0
                        ? Math.min(1f, owner.getAspects().soulfireCur() / required)
                        : 1f;
                    yield InputActionExecutor.soulfireStatusColor(t);
                }
                case DISABLED -> Config.SwordColor.TEXT_COOL_DARK;
            };
            TextDecoration hudDeco = (readiness == InputActionExecutor.ReadinessState.READY)
                ? TextDecoration.BOLD : TextDecoration.STRIKETHROUGH;

            potentialInputSelectionText = potentialInputSelectionText
                .append(Component.text(inputToString(entry.getKey()), hudColor, hudDeco))
                .append(it.hasNext() ?
                    Component.text(", ", Config.SwordColor.TEXT_COOL_DARK) :
                    Component.text(""));
            inputAccepted = true;
        }
        if (inputAccepted) startTimeoutTimer();

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
     * Returns a magenta-range {@link TextColor} for a cooldown progress fraction.
     * {@code t = 0} (just fired, no progress) → dark magenta {@code (60, 10, 60)}.
     * {@code t = 1} (cooldown nearly expired) → bright magenta {@code (220, 80, 220)}.
     *
     * @param t cooldown progress fraction (0.0–1.0, clamped)
     * @return interpolated TextColor
     */
    private static TextColor cooldownColor(float t) {
//        TextColor.color(0, 34, 64);
//        TextColor.color(110, 144, 174);
        int r0 = 0;
        int b0 = 34;
        int g0 = 64;
        int r1 = 110;
        int g1 = 144;
        int b1 = 174;
        int r_diff = r1-r0;
        int g_diff = g1-g0;
        int b_diff = b1-b0;
        float clamped = Math.max(0f, Math.min(1f, t));
        int r = (int) (r0 + clamped * (r_diff));
        int g = (int) (g0 + clamped * (g_diff));
        int b = (int) (b0 + clamped * (b_diff));
        return TextColor.color(r, g, b);
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

    public static final ActionContextPair NOOP = new ActionContextPair(
        () -> new InputAction(
            "NOOP",
            c -> {
                Debug.system("Noop action 'fired'");
            },
            c -> 0,
            c -> true,
            null,
            c -> 0f,
            false,
            false,
            false,
            c -> 0),
        c -> true);

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
        /**
         * Optional node-level visibility gate. When non-null and returning {@code false},
         * the node is treated as hidden regardless of its action context predicates.
         * Set via {@link InputNodeBuilder#visibleIf}; apply to leaf nodes only.
         */
        private Predicate<SwordPlayer> visibleIf;
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
         * Appends new action pairs to this node's existing action list and clears all caches.
         * Used when multiple {@link InputNodeBuilder} registrations share the same terminal node
         * (e.g. ACTIVE_1 and ACTIVE_2 both live on SWAP {@literal >} RIGHT).
         *
         * @param newActions the action pairs to append in priority order
         */
        public void appendActions(LinkedList<ActionContextPair> newActions) {
            if (this.actions == null) {
                this.actions = new LinkedList<>(newActions);
            } else {
                this.actions.addAll(newActions);
            }
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
            if (actions == null || actions.isEmpty()) return null;
            if (!dynamic && actions.size() == 1) {
                ActionContextPair pair = actions.getFirst();
                if (!pair.context().test(owner)) return null;
                if (cachedAction == null) cachedAction = pair.action().get();
                return cachedAction;
            }
            for (ActionContextPair pair : actions) {
                if (pair == null || pair.action == null || pair.action.get() == null) {
                    continue;
                }
                if (pair.context().test(owner)) {
                    if (dynamic) return pair.action().get();
                    if (pairCache == null) pairCache = new IdentityHashMap<>();
                    InputAction action = pairCache.computeIfAbsent(pair, p -> p.action().get());
                    if (action.unableToCast(owner)) {
                        continue;
                    }
                    return action;
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
         * it will append to the existing node's action list.
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
         * Returns true if this node is inaccessible for the given player.
         * <p>
         * For leaf nodes (with actions): hidden if no action's context predicate passes.
         * For intermediate nodes (null or empty actions): hidden only if every child is also
         * hidden — this prevents stepping into a path whose entire subtree is context-gated
         * (e.g. the F {@literal >} L umbral-skill prefix when not holding soul link).
         * </p>
         *
         * @param player the player to evaluate
         * @return true if no accessible path exists through this node, false otherwise
         */
        /**
         * Sets the node-level visibility predicate. When the predicate returns {@code false}
         * the node is hidden and not navigable, regardless of its action context predicates.
         *
         * @param visibleIf predicate returning {@code true} when this node should be visible
         */
        public void setVisibleIf(Predicate<SwordPlayer> visibleIf) {
            this.visibleIf = visibleIf;
        }

        public boolean hiddenFor(SwordPlayer player) {
            if (visibleIf != null && !visibleIf.test(player)) return true;
            if (actions == null || actions.isEmpty()) {
                if (children.isEmpty()) return true;
                for (InputNode child : children.values()) {
                    if (!child.hiddenFor(player)) return false;
                }
                return true;
            }
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
        private Predicate<SwordPlayer> visibleIf = null;

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
         * Attaches a node-level visibility predicate to the terminal node.
         * When the predicate returns {@code false}, the node is hidden in the HUD and not
         * navigable. Apply to leaf nodes only — see {@link InputNode#hiddenFor(SwordPlayer)}.
         *
         * @param predicate returns {@code true} when this node should be visible/navigable
         * @return this builder
         */
        public InputNodeBuilder visibleIf(Predicate<SwordPlayer> predicate) {
            this.visibleIf = predicate;
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
                    if (dummy.getActions() == null || dummy.getActions().isEmpty()) {
                        dummy.setActions(actions);
                    } else {
                        dummy.appendActions(actions);
                    }
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
                if (visibleIf != null) {
                    dummy.setVisibleIf(visibleIf);
                }
            }
        }
    }
}
