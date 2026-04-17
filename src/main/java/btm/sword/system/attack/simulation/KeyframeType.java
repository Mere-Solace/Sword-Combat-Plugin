package btm.sword.system.attack.simulation;

/**
 * Semantic role assigned to a {@link VolumeKeyframe} at recording time.
 *
 * <p>Used by the volume editor and menus to apply per-type colour coding.
 * Persisted in the YAML file under the {@code keyframe-type} key;
 * omitted when {@link #STANDARD} to keep existing files clean.</p>
 */
public enum KeyframeType {

    /** Default untagged keyframe. */
    STANDARD,

    /** First anchor of a Bézier curve group (every 4th point, index 0). */
    BEZIER_START,

    /** First control handle of a Bézier curve group (index 1). */
    BEZIER_C1,

    /** Second control handle of a Bézier curve group (index 2). */
    BEZIER_C2,

    /** Last anchor of a Bézier curve group (index 3). */
    BEZIER_END,

    /** Keyframe placed in {@link PlacementMode#LINE_SEGMENT} recording mode. */
    LINE,

    /**
     * Keyframe placed in {@link PlacementMode#ORIGIN_RAY} recording mode.
     * The ray that defines this keyframe's hitbox starts at
     * {@code origin + normalize(origin → tip) * originRayOffset}, not at the origin itself.
     * The offset is stored in {@link btm.sword.system.attack.simulation.VolumeKeyframe#originRayOffset()}.
     */
    ORIGIN_RAY,

    /**
     * Keyframe placed in {@link PlacementMode#RAYCAST} recording mode.
     * The hitbox ray runs from {@code localRayOrigin} (where the ray started when placed)
     * to {@code localPosition} (the tip). Both are stored in local space so that the
     * trajectory can independently spline origins and tips during playback.
     * The origin is stored in {@link btm.sword.system.attack.simulation.VolumeKeyframe#localRayOrigin()}.
     */
    RAYCAST
}
