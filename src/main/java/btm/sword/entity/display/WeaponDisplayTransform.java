package btm.sword.entity.display;

/**
 * Configurable transform applied to the weapon-slot {@link org.bukkit.entity.ItemDisplay}
 * that follows a display-rig mob's main hand.
 *
 * <p>Offsets are in the mob's local coordinate frame (right/up/forward), rotations are
 * Euler angles in degrees, and scale is uniform.</p>
 *
 * @param offsetRight   world-right offset relative to the mob's facing (positive = mob's right)
 * @param offsetUp      vertical offset (positive = up)
 * @param offsetForward forward offset relative to the mob's facing (positive = in front)
 * @param rotX          local X-axis rotation in degrees
 * @param rotY          local Y-axis rotation in degrees
 * @param rotZ          local Z-axis rotation in degrees
 * @param scale         uniform scale applied to the item display
 */
public record WeaponDisplayTransform(
        float offsetRight, float offsetUp, float offsetForward,
        float rotX, float rotY, float rotZ,
        float scale) {

    /** Default transform: no offset, no rotation, scale 1. */
    public static final WeaponDisplayTransform DEFAULT =
        new WeaponDisplayTransform(0, 0, 0, 0, 0, 0, 1.0f);
}
