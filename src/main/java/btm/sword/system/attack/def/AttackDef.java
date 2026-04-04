package btm.sword.system.attack.def;

import java.util.List;
import java.util.function.BiFunction;

import org.joml.Vector3f;

import btm.sword.system.attack.HitValuePacket;
import btm.sword.system.attack.simulation.KeyframedTrajectory;
import btm.sword.system.attack.simulation.SweepCurve;
import btm.sword.system.attack.simulation.SweepTrajectory;
import btm.sword.system.attack.simulation.Volume;
import btm.sword.system.attack.simulation.VolumeKeyframe;
import btm.sword.system.attack.simulation.VolumeTrajectory;
import btm.sword.system.entity.base.SwordEntity;
import lombok.Getter;

/**
 * Immutable definition of a named attack — its timing, volume trajectory, damage values,
 * and knockback behaviour.
 *
 * <p>Build via {@link Builder}:</p>
 * <pre>{@code
 * AttackDef def = new AttackDef.Builder("quick_slash")
 *     .duration(400)
 *     .keyframes(keyframeList)          // implicitly sets type = VOLUME
 *     .onHit(hitValuePacket)
 *     .knockback((contact, attacker) -> ...)
 *     .build();
 * }</pre>
 *
 * <p>When activating an attack, allocate a fresh {@link Volume} buffer via
 * {@link #createVolume()} and pass it to {@link btm.sword.system.attack.simulation.ActiveAttack}.</p>
 */
@Getter
public final class AttackDef {

    private final String id;
    private final AttackPrimitive type;
    private final int durationMs;
    private final VolumeTrajectory trajectory;
    private final HitValuePacket hitValue;
    private final BiFunction<Vector3f, SwordEntity, Vector3f> knockbackFunction;

    private AttackDef(Builder b) {
        this.id = b.id;
        this.type = b.type;
        this.durationMs = b.durationMs;
        this.trajectory = b.trajectory;
        this.hitValue = b.hitValue;
        this.knockbackFunction = b.knockbackFunction;
    }

    /**
     * Allocates a fresh {@link Volume} buffer appropriate for this attack's {@link AttackPrimitive}.
     * Call once per activation and pass the result to
     * {@link btm.sword.system.attack.simulation.ActiveAttack}.
     *
     * @return a new, empty volume buffer
     */
    public Volume createVolume() {
        return type.createVolume();
    }

    // ── Builder ───────────────────────────────────────────────────────────────

    /**
     * Fluent builder for {@link AttackDef}.
     */
    public static final class Builder {

        private final String id;
        private AttackPrimitive type;
        private int durationMs;
        private VolumeTrajectory trajectory;
        private HitValuePacket hitValue;
        private BiFunction<Vector3f, SwordEntity, Vector3f> knockbackFunction =
            (contact, src) -> new Vector3f();

        /**
         * @param id unique attack identifier; must not be null or empty
         */
        public Builder(String id) {
            if (id == null || id.isBlank()) throw new IllegalArgumentException("AttackDef id must not be blank");
            this.id = id;
        }

        /**
         * Sets the attack duration in milliseconds.
         *
         * @param durationMs duration in ms
         * @return this builder
         */
        public Builder duration(int durationMs) {
            this.durationMs = durationMs;
            return this;
        }

        /**
         * Explicitly sets the primitive type. Required when using {@link #trajectory(VolumeTrajectory)};
         * implied automatically by {@link #keyframes} and {@link #sweep}.
         *
         * @param type the collision primitive
         * @return this builder
         */
        public Builder type(AttackPrimitive type) {
            this.type = type;
            return this;
        }

        /**
         * Builds a {@link KeyframedTrajectory} from the given keyframes and sets {@code type = VOLUME}.
         * Overridden by a subsequent {@link #trajectory} call.
         *
         * @param keyframes ordered list of OBB keyframes
         * @return this builder
         */
        public Builder keyframes(List<VolumeKeyframe> keyframes) {
            this.type = AttackPrimitive.VOLUME;
            this.trajectory = new KeyframedTrajectory(keyframes);
            return this;
        }

        /**
         * Builds a {@link SweepTrajectory} from the given curve and sets {@code type = SWEEP}.
         * Overridden by a subsequent {@link #trajectory} call.
         *
         * @param curve the Catmull-Rom sweep curve
         * @return this builder
         */
        public Builder sweep(SweepCurve curve) {
            this.type = AttackPrimitive.SWEEP;
            this.trajectory = new SweepTrajectory(curve);
            return this;
        }

        /**
         * Sets a custom trajectory, overriding any prior {@link #keyframes} or {@link #sweep} call.
         * {@link #type} must be set explicitly when using this method.
         *
         * @param trajectory custom trajectory implementation or lambda
         * @return this builder
         */
        public Builder trajectory(VolumeTrajectory trajectory) {
            this.trajectory = trajectory;
            return this;
        }

        /**
         * Sets the damage values delivered on each hit.
         *
         * @param hitValue the hit value packet
         * @return this builder
         */
        public Builder onHit(HitValuePacket hitValue) {
            this.hitValue = hitValue;
            return this;
        }

        /**
         * Sets a custom knockback function. Receives the contact point and the attacking entity;
         * returns the knockback velocity to apply to the victim.
         *
         * @param knockbackFunction {@code (contactPoint, attacker) -> knockbackVelocity}
         * @return this builder
         */
        public Builder knockback(BiFunction<Vector3f, SwordEntity, Vector3f> knockbackFunction) {
            this.knockbackFunction = knockbackFunction;
            return this;
        }

        /**
         * Builds and returns an immutable {@link AttackDef}.
         *
         * @return the constructed attack definition
         * @throws IllegalStateException if {@code type}, {@code trajectory}, {@code hitValue},
         *                               or {@code durationMs} have not been set
         */
        public AttackDef build() {
            if (type == null) throw new IllegalStateException("AttackDef '" + id + "': type not set");
            if (trajectory == null) throw new IllegalStateException("AttackDef '" + id + "': trajectory not set");
            if (hitValue == null) throw new IllegalStateException("AttackDef '" + id + "': hitValue not set");
            if (durationMs <= 0) throw new IllegalStateException("AttackDef '" + id + "': durationMs must be > 0");
            return new AttackDef(this);
        }
    }
}
