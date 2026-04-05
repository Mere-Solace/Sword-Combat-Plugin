package btm.sword.system.attack.simulation;

import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * A single keyframe of a volume attack, expressed in the attacker's local space.
 * <p>
 * Used by {@code KeyframedTrajectory} to define the shape and position of a volume at a
 * point along the normalized attack timeline. The trajectory interpolates between consecutive
 * keyframes to produce a smooth volume sequence.
 * </p>
 *
 * <p>The {@link #shape} field controls which fields are meaningful:
 * {@link VolumeShape#SPHERE} uses only {@code localPosition} and {@code halfExtents.x} (radius);
 * {@link VolumeShape#OBB} uses all fields.</p>
 *
 * @param t             normalized time of this keyframe, {@code 0.0} to {@code 1.0}
 * @param localPosition volume center relative to the attacker's body origin
 * @param halfExtents   for OBB: {@code (width/2, height/2, depth/2)};
 *                      for SPHERE: {@code x} is the radius, {@code y}/{@code z} are unused
 * @param rotation      local orientation as a unit quaternion (OBB only)
 * @param shape         the volume shape — determines rendering and collision geometry
 */
public record VolumeKeyframe(
    float t,
    Vector3f localPosition,
    Vector3f halfExtents,
    Quaternionf rotation,
    VolumeShape shape
) {}
