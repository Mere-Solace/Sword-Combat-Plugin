package btm.sword.system.entity.umbral.input;

/** Enumerates all requests that can be pushed into an UmbralBlade's {@link InputBuffer} to drive state transitions. */
public enum BladeRequest {
    SHEATH,
    STANDBY,
    TOGGLE,

    WIELD,

    ATTACK_QUICK,
    ATTACK_HEAVY,

    LUNGE,
    GRAB_IMPALE,

    FINISHER,

    RECALL,
    WAITING,

    ACTIVATE_TO_PREVIOUS,
    DEACTIVATE,
    RESUME_FROM_REPAIR
}
