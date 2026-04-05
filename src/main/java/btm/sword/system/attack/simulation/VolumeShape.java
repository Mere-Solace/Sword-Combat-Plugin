package btm.sword.system.attack.simulation;

/**
 * Describes the collision and visualization shape of a {@link VolumeKeyframe}.
 *
 * <p>The shape determines which fields of the keyframe are meaningful and how the keyframe
 * is rendered in {@link btm.sword.system.attack.dev.VolumeEditorMode}.</p>
 *
 * <ul>
 *   <li>{@link #OBB}    — Oriented Bounding Box. Uses {@code localPosition}, {@code halfExtents},
 *                         and {@code rotation}.</li>
 *   <li>{@link #SPHERE} — Sphere. Uses {@code localPosition} and {@code halfExtents.x} as radius.
 *                         {@code halfExtents.y}/{@code z} and {@code rotation} are ignored.</li>
 * </ul>
 */
public enum VolumeShape {
    OBB,
    SPHERE
}
