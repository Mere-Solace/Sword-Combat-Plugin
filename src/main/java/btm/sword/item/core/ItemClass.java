package btm.sword.item.core;

import btm.sword.input.binding.ItemInputDispatchTable;

/**
 * Classifies an item stack's interaction behavior within the Sword input system.
 * <p>
 * Used by {@link ItemClassifier} to determine how the input execution tree
 * and event handlers should respond when a player holds or uses an item.
 * </p>
 *
 * @see ItemClassifier
 */
public enum ItemClass {
    /**
     * The item can be thrown via the Sword throw system.
     * Throw combos are available while holding this item.
     */
    THROWABLE,

    /**
     * The item has vanilla Minecraft right-click behavior (eating, blocking, charging, etc.).
     * Right-click and drop events pass through to vanilla — the Sword input tree is only
     * suppressed for those inputs. Left, shift, and swap inputs route normally.
     */
    USABLE,

    /**
     * The item is fully managed by the Sword system and suppresses all vanilla inputs.
     * Any input (left, right, drop, swap, shift) is intercepted and routed through the
     * EARLY phase of the {@link ItemInputDispatchTable} rather than the
     * Sword input execution tree or vanilla Minecraft.
     */
    BLOCKED,

    /**
     * Standard item with no special classification.
     * Routes through the Sword input tree normally.
     */
    NEUTRAL
}
