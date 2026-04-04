package btm.sword.system.attack.simulation;

import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * A single keyframe of an OBB volume attack, expressed in the attacker's local space.
 * <p>
 * Used by {@code KeyframedTrajectory} to define the shape and position of an oriented
 * bounding box at a point along the normalized attack timeline. The trajectory interpolates
 * between consecutive keyframes to produce a smooth volume sequence.
 * </p>
 *
 * @param t             normalized time of this keyframe, {@code 0.0} to {@code 1.0}
 * @param localPosition OBB center relative to the attacker's body origin
 * @param halfExtents   OBB half-extents: {@code (width/2, height/2, depth/2)}
 * @param rotation      local OBB orientation as a unit quaternion
 */
public record VolumeKeyframe(
    float t,
    Vector3f localPosition,
    Vector3f halfExtents,
    Quaternionf rotation
) {}
