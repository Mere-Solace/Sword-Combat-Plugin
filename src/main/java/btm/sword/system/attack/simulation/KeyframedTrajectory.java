package btm.sword.system.attack.simulation;

import java.util.List;

import org.joml.Matrix3f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import btm.sword.utility.math.BezierUtil;

/**
 * A {@link VolumeTrajectory} that animates an OBB volume through a sequence of
 * {@link VolumeKeyframe}s using Catmull-Rom interpolation.
 *
 * <p>Position is interpolated with a Catmull-Rom spline across the keyframe sequence.
 * Rotation is slerped between the two bracketing keyframes. Half-extents are linearly
 * interpolated. All values are expressed in the attacker's local space and transformed
 * to world space via the supplied {@code worldTransform}.</p>
 *
 * <p>Use with an {@link ObbVolume} buffer passed to {@link ActiveAttack}.</p>
 */
public final class KeyframedTrajectory implements VolumeTrajectory {

    private final List<VolumeKeyframe> keyframes;

    /**
     * Creates a keyframed OBB trajectory.
     *
     * @param keyframes ordered list of keyframes; must contain at least one entry
     * @throws IllegalArgumentException if the list is empty
     */
    public KeyframedTrajectory(List<VolumeKeyframe> keyframes) {
        if (keyframes.isEmpty()) {
            throw new IllegalArgumentException("KeyframedTrajectory requires at least one keyframe");
        }
        this.keyframes = List.copyOf(keyframes);
    }

    /**
     * Returns the immutable keyframe list.
     *
     * @return keyframes in order
     */
    public List<VolumeKeyframe> getKeyframes() {
        return keyframes;
    }

    /**
     * {@inheritDoc}
     *
     * @param out must be an {@link ObbVolume}
     */
    @Override
    public void sample(float t, Matrix4fc worldTransform, Volume out) {
        ObbVolume obbOut = (ObbVolume) out;
        int n = keyframes.size();

        if (n == 1 || t <= keyframes.getFirst().t()) {
            writeKeyframe(keyframes.getFirst(), worldTransform, obbOut);
            return;
        }
        if (t >= keyframes.getLast().t()) {
            writeKeyframe(keyframes.getLast(), worldTransform, obbOut);
            return;
        }

        // Find the segment [i, i+1] that contains t
        int i = 0;
        for (int k = 0; k < n - 1; k++) {
            if (t < keyframes.get(k + 1).t()) {
                i = k;
                break;
            }
        }

        VolumeKeyframe ka = keyframes.get(Math.max(0, i - 1));
        VolumeKeyframe kb = keyframes.get(i);
        VolumeKeyframe kc = keyframes.get(i + 1);
        VolumeKeyframe kd = keyframes.get(Math.min(n - 1, i + 2));

        // Jump: hold at kb until kc's t is crossed — no interpolation into this keyframe
        if (kc.jump()) {
            writeKeyframe(kb, worldTransform, obbOut);
            return;
        }

        float span = kc.t() - kb.t();
        float localT = span > 1e-6f ? (t - kb.t()) / span : 0f;

        // Interpolate in local space
        Vector3f localPos = BezierUtil.catmullRom(
            ka.localPosition(), kb.localPosition(), kc.localPosition(), kd.localPosition(), localT
        );
        Quaternionf localRot = new Quaternionf(kb.rotation()).slerp(kc.rotation(), localT);
        Vector3f he = new Vector3f(kb.halfExtents()).lerp(kc.halfExtents(), localT);

        applyToOutput(localPos, localRot, he, worldTransform, obbOut);
    }

    private static void writeKeyframe(VolumeKeyframe kf, Matrix4fc worldTransform, ObbVolume out) {
        applyToOutput(
            new Vector3f(kf.localPosition()),
            new Quaternionf(kf.rotation()),
            new Vector3f(kf.halfExtents()),
            worldTransform,
            out
        );
    }

    private static void applyToOutput(Vector3f localPos, Quaternionf localRot, Vector3f he,
                                      Matrix4fc worldTransform, ObbVolume out) {
        // Transform position and rotation to world space
        worldTransform.transformPosition(localPos, out.center);
        Quaternionf worldRot = worldTransform.getNormalizedRotation(new Quaternionf());
        worldRot.mul(localRot, out.rotation);
        out.halfExtents.set(he);

        // Derive world AABB from OBB — project half-extents through absolute rotation matrix
        Matrix3f rot = new Matrix3f().rotation(out.rotation);
        float ex = Math.abs(rot.m00()) * he.x + Math.abs(rot.m10()) * he.y + Math.abs(rot.m20()) * he.z;
        float ey = Math.abs(rot.m01()) * he.x + Math.abs(rot.m11()) * he.y + Math.abs(rot.m21()) * he.z;
        float ez = Math.abs(rot.m02()) * he.x + Math.abs(rot.m12()) * he.y + Math.abs(rot.m22()) * he.z;
        out.aabbMin.set(out.center.x - ex, out.center.y - ey, out.center.z - ez);
        out.aabbMax.set(out.center.x + ex, out.center.y + ey, out.center.z + ez);
    }
}
