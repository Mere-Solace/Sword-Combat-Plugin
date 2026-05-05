package btm.sword.input;

import btm.sword.input.trie.InputExecutionTree;

/** Enumerates all discrete player inputs that can be routed through the {@link InputExecutionTree}. */
public enum InputType {
    LEFT,
    RIGHT,
    RIGHT_TAP,
    RIGHT_HOLD,
    DROP,
    SWAP,
    SHIFT,
    SHIFT_TAP,
    SHIFT_HOLD
}
