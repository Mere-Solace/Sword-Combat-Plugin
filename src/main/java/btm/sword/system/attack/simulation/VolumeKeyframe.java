package btm.sword.system.attack.simulation;

import org.jetbrains.annotations.Nullable;
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
 * <p>The optional {@link #effect} bundle fires particle and sound effects when the simulation's
 * normalized time crosses this keyframe's {@code t} value.</p>
 *
 * @param t             normalized time of this keyframe, {@code 0.0} to {@code 1.0}
 * @param localPosition volume center relative to the attacker's body origin
 * @param halfExtents   for OBB: {@code (width/2, height/2, depth/2)};
 *                      for SPHERE: {@code x} is the radius, {@code y}/{@code z} are unused
 * @param rotation      local orientation as a unit quaternion (OBB only)
 * @param shape         the volume shape — determines rendering and collision geometry
 * @param effect        optional particle/sound effects fired when {@code t} is crossed;
 *                      {@code null} for no effects
 * @param jump          when {@code true}, the trajectory holds at the previous keyframe's
 *                      value until {@code t} is crossed — no interpolation occurs entering
 *                      this keyframe
 */
public record VolumeKeyframe(
    float t,
    Vector3f localPosition,
    Vector3f halfExtents,
    Quaternionf rotation,
    VolumeShape shape,
    @Nullable KeyframeEffect effect,
    boolean jump
) {

    /**
     * Returns a copy of this keyframe with the given effect bundle replacing the current one.
     *
     * @param newEffect the effect to attach, or {@code null} to clear effects
     * @return a new {@link VolumeKeyframe} identical to this one except for the effect
     */
    public VolumeKeyframe withEffect(@Nullable KeyframeEffect newEffect) {
        return new VolumeKeyframe(t, localPosition, halfExtents, rotation, shape, newEffect, jump);
    }

    /**
     * Returns a copy of this keyframe with the {@code jump} flag set to {@code j}.
     *
     * @param j the new jump value
     * @return a new {@link VolumeKeyframe} identical to this one except for the jump flag
     */
    public VolumeKeyframe withJump(boolean j) {
        return new VolumeKeyframe(t, localPosition, halfExtents, rotation, shape, effect, j);
    }

    /**
     * Returns a copy of this keyframe with the normalized time set to {@code newT}.
     *
     * @param newT the new normalized time
     * @return a new {@link VolumeKeyframe} identical to this one except for the time value
     */
    public VolumeKeyframe withT(float newT) {
        return new VolumeKeyframe(newT, localPosition, halfExtents, rotation, shape, effect, jump);
    }
}
