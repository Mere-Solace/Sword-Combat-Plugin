package btm.sword.system.action.throwing;

/** Defines the trajectory and rotation style applied when an item is thrown. */
public enum ItemThrowStyle {
    SPEAR, // Strong, direct throw with no rotation, impaling enemies hit
    LOB, // for heavy objects like blocks, slight upward arc forward
    PITCH, // for smaller objects, a direct throw with more speed and
    ROTATE, // a balanced spinning throw used by smithing refits and similar item mods
    HATCHET, // for axes and knives and short swords that would twirl forward when thrown
    // BOOMERANG ???
    // THROW_FOR_SHOW Twirl upwards for a cool catch or setup for other moves ???
}
