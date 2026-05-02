package btm.sword.combat.simulation;

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
 * @param linearToNext  when {@code true}, the segment from this keyframe to the next uses
 *                      linear interpolation instead of Catmull-Rom
 * @param keyframeType    semantic role of this keyframe, used for colour-coding in the editor;
 *                        {@link KeyframeType#STANDARD} for untagged frames
 * @param originRayOffset for {@link KeyframeType#ORIGIN_RAY} keyframes: the hitbox ray starts at
 *                        {@code origin + normalize(origin → tip) * originRayOffset}, not at the
 *                        origin itself. {@code 0f} for all other keyframe types.
 * @param localRayOrigin  for {@link KeyframeType#RAYCAST} keyframes: the local-space position
 *                        where the ray started when this point was placed. {@code null} for all
 *                        other keyframe types. The trajectory independently splines origins and
 *                        tips during playback.
 */
public record VolumeKeyframe(
    float t,
    Vector3f localPosition,
    Vector3f halfExtents,
    Quaternionf rotation,
    VolumeShape shape,
    @Nullable KeyframeEffect effect,
    boolean jump,
    boolean linearToNext,
    KeyframeType keyframeType,
    float originRayOffset,
    @Nullable Vector3f localRayOrigin
) {

    /**
     * Convenience constructor — {@code originRayOffset} defaults to {@code 0f},
     * {@code localRayOrigin} defaults to {@code null}.
     * All existing call sites that don't need these fields use this form.
     */
    public VolumeKeyframe(float t, Vector3f localPosition, Vector3f halfExtents,
            Quaternionf rotation, VolumeShape shape, @Nullable KeyframeEffect effect,
            boolean jump, boolean linearToNext, KeyframeType keyframeType) {
        this(t, localPosition, halfExtents, rotation, shape, effect, jump, linearToNext, keyframeType, 0f, null);
    }

    /**
     * Returns a copy of this keyframe with the given effect bundle replacing the current one.
     *
     * @param newEffect the effect to attach, or {@code null} to clear effects
     * @return a new {@link VolumeKeyframe} identical to this one except for the effect
     */
    public VolumeKeyframe withEffect(@Nullable KeyframeEffect newEffect) {
        return new VolumeKeyframe(t, localPosition, halfExtents, rotation, shape, newEffect, jump, linearToNext, keyframeType, originRayOffset, localRayOrigin);
    }

    /**
     * Returns a copy of this keyframe with the {@code jump} flag set to {@code j}.
     *
     * @param j the new jump value
     * @return a new {@link VolumeKeyframe} identical to this one except for the jump flag
     */
    public VolumeKeyframe withJump(boolean j) {
        return new VolumeKeyframe(t, localPosition, halfExtents, rotation, shape, effect, j, linearToNext, keyframeType, originRayOffset, localRayOrigin);
    }

    /**
     * Returns a copy of this keyframe with the normalized time set to {@code newT}.
     *
     * @param newT the new normalized time
     * @return a new {@link VolumeKeyframe} identical to this one except for the time value
     */
    public VolumeKeyframe withT(float newT) {
        return new VolumeKeyframe(newT, localPosition, halfExtents, rotation, shape, effect, jump, linearToNext, keyframeType, originRayOffset, localRayOrigin);
    }

    /**
     * Returns a copy of this keyframe with the {@code linearToNext} flag set to {@code linear}.
     *
     * @param linear {@code true} to use linear interpolation to the following keyframe
     * @return a new {@link VolumeKeyframe} identical to this one except for the linearToNext flag
     */
    public VolumeKeyframe withLinearToNext(boolean linear) {
        return new VolumeKeyframe(t, localPosition, halfExtents, rotation, shape, effect, jump, linear, keyframeType, originRayOffset, localRayOrigin);
    }

    /**
     * Returns a copy of this keyframe with {@code localPosition} replaced.
     *
     * @param pos the new local position
     * @return a new {@link VolumeKeyframe} identical to this one except for the position
     */
    public VolumeKeyframe withLocalPosition(Vector3f pos) {
        return new VolumeKeyframe(t, pos, halfExtents, rotation, shape, effect, jump, linearToNext, keyframeType, originRayOffset, localRayOrigin);
    }

    /**
     * Returns a copy of this keyframe with {@code halfExtents} replaced.
     *
     * @param he the new half-extents
     * @return a new {@link VolumeKeyframe} identical to this one except for the half-extents
     */
    public VolumeKeyframe withHalfExtents(Vector3f he) {
        return new VolumeKeyframe(t, localPosition, he, rotation, shape, effect, jump, linearToNext, keyframeType, originRayOffset, localRayOrigin);
    }
}
