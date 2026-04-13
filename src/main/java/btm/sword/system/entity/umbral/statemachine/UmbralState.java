package btm.sword.system.entity.umbral.statemachine;

/** Legacy enum of named UmbralBlade states; superseded by the FSM state class hierarchy. */
public enum UmbralState {
    WIELD,  // display is invis and all update ops stop (still keep as passenger for quick retrieval, though
            // display the soul link as the weapon to the player.

    SHEATHED,
    STANDBY,

    RECALLING,
    RETURNING,

    WAITING,

    ATTACKING_QUICK,
    ATTACKING_HEAVY,
    LUNGING,
    FLYING,

    LODGED
}
