package btm.sword.system.attack.dev;

/**
 * Represents the current state of an {@link AttackDevSession}.
 *
 * <ul>
 *   <li>{@link #IDLE}      — no active dev session; the player is not creating or editing an attack.</li>
 *   <li>{@link #RECORDING} — capturing raw local-space tip positions from blaze rod motion to build
 *                             a sweep curve.</li>
 *   <li>{@link #EDITING}   — interactively placing or adjusting volume keyframes on an existing or
 *                             in-progress attack definition.</li>
 * </ul>
 */
public enum DevMode {
    IDLE,
    RECORDING,
    EDITING
}
