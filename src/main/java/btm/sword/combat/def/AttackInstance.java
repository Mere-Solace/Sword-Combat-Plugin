package btm.sword.combat.def;

import java.util.List;
import java.util.function.BiFunction;

import org.joml.Vector3f;

import btm.sword.combat.attack.ActiveAttack;
import btm.sword.combat.hit.HitValuePacket;
import btm.sword.combat.simulation.ControlMode;
import btm.sword.combat.simulation.ControlPoint;
import btm.sword.combat.simulation.ControlPointSequence;
import btm.sword.combat.simulation.KeyframedSequence;
import btm.sword.combat.simulation.SweepCurve;
import btm.sword.combat.simulation.SweepSequence;
import btm.sword.combat.simulation.Volume;
import btm.sword.combat.simulation.VolumeKeyframe;
import btm.sword.combat.simulation.VolumeSequence;
import btm.sword.config.Config;
import btm.sword.entity.base.SwordEntity;
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
 * {@link #createVolume()} and pass it to {@link ActiveAttack}.</p>
 */
@Getter
public final class AttackInstance {

    private final String id;
    private final VolumeType type;
    private final int durationMs;
    private final VolumeSequence trajectory;
    private final HitValuePacket hitValue;
    private final BiFunction<Vector3f, SwordEntity, Vector3f> knockbackFunction;
    private final boolean orientWithPitch;
    private final boolean lockOriginOnFire;

    private AttackInstance(Builder b) {
        this.id = b.id;
        this.type = b.type;
        this.durationMs = b.durationMs;
        this.trajectory = b.trajectory;
        this.hitValue = b.hitValue;
        this.knockbackFunction = b.knockbackFunction;
        this.orientWithPitch = b.orientWithPitch;
        this.lockOriginOnFire = b.lockOriginOnFire;
    }

    /**
     * Allocates a fresh {@link Volume} buffer appropriate for this attack's {@link VolumeType}.
     * Call once per activation and pass the result to
     * {@link ActiveAttack}.
     *
     * @return a new, empty volume buffer
     */
    public Volume createVolume() {
        return type.createVolume();
    }

    // ── Builder ───────────────────────────────────────────────────────────────

    /**
     * Fluent builder for {@link AttackInstance}.
     */
    public static final class Builder {

        private final String id;
        private VolumeType type;
        private int durationMs;
        private VolumeSequence trajectory;
        private HitValuePacket hitValue;
        private BiFunction<Vector3f, SwordEntity, Vector3f> knockbackFunction =
            (contact, src) -> new Vector3f();
        private boolean orientWithPitch = Config.Combat.ATTACKS_ORIENT_WITH_PITCH;
        private boolean lockOriginOnFire = Config.Combat.ATTACKS_LOCK_ORIGIN_ON_FIRE;

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
         * Explicitly sets the primitive type. Required when using {@link #trajectory(VolumeSequence)};
         * implied automatically by {@link #keyframes} and {@link #sweep}.
         *
         * @param type the collision primitive
         * @return this builder
         */
        public Builder type(VolumeType type) {
            this.type = type;
            return this;
        }

        /**
         * Builds a {@link KeyframedSequence} from the given keyframes and sets {@code type = VOLUME}.
         * Overridden by a subsequent {@link #trajectory} call.
         *
         * @param keyframes ordered list of OBB keyframes
         * @return this builder
         */
        public Builder keyframes(List<VolumeKeyframe> keyframes) {
            this.type = VolumeType.VOLUME;
            this.trajectory = new KeyframedSequence(keyframes);
            return this;
        }

        /**
         * Builds a {@link SweepSequence} from the given curve and sets {@code type = SWEEP}.
         * Overridden by a subsequent {@link #trajectory} call.
         *
         * @param curve the Catmull-Rom sweep curve
         * @return this builder
         */
        public Builder sweep(SweepCurve curve) {
            this.type = VolumeType.SWEEP;
            this.trajectory = new SweepSequence(curve);
            return this;
        }

        /**
         * Builds a {@link ControlPointSequence} from the given control points and mode,
         * and sets {@code type = CTRL_POINT}. Overridden by a subsequent {@link #trajectory} call.
         *
         * @param points ordered list of control points; must be 2 for {@link ControlMode#LINEAR}
         *               or 4 for {@link ControlMode#BEZIER}
         * @param mode   interpolation mode
         * @return this builder
         */
        public Builder controlPoints(List<ControlPoint> points, ControlMode mode) {
            this.type = VolumeType.CTRL_POINT;
            this.trajectory = new ControlPointSequence(points, mode);
            return this;
        }

        /**
         * Sets a custom trajectory, overriding any prior {@link #keyframes} or {@link #sweep} call.
         * {@link #type} must be set explicitly when using this method.
         *
         * @param trajectory custom trajectory implementation or lambda
         * @return this builder
         */
        public Builder trajectory(VolumeSequence trajectory) {
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
         * Overrides whether OBBs are tilted by the attacker's pitch angle.
         * Defaults to {@link Config.Combat#ATTACKS_ORIENT_WITH_PITCH}.
         *
         * @param orientWithPitch {@code true} to apply pitch rotation to the world transform
         * @return this builder
         */
        public Builder orientWithPitch(boolean orientWithPitch) {
            this.orientWithPitch = orientWithPitch;
            return this;
        }

        /**
         * Overrides whether the attack origin is locked at fire time.
         * When {@code true} the OBBs travel in the direction the player was facing when
         * the attack was launched, regardless of subsequent movement.
         * Defaults to {@link Config.Combat#ATTACKS_LOCK_ORIGIN_ON_FIRE}.
         *
         * @param lockOriginOnFire {@code true} to lock origin and direction at fire time
         * @return this builder
         */
        public Builder lockOriginOnFire(boolean lockOriginOnFire) {
            this.lockOriginOnFire = lockOriginOnFire;
            return this;
        }

        /**
         * Builds and returns an immutable {@link AttackInstance}.
         *
         * @return the constructed attack definition
         * @throws IllegalStateException if {@code type}, {@code trajectory}, {@code hitValue},
         *                               or {@code durationMs} have not been set
         */
        public AttackInstance build() {
            if (type == null) throw new IllegalStateException("AttackDef '" + id + "': type not set");
            if (trajectory == null) throw new IllegalStateException("AttackDef '" + id + "': trajectory not set");
            if (hitValue == null) throw new IllegalStateException("AttackDef '" + id + "': hitValue not set");
            if (durationMs <= 0) throw new IllegalStateException("AttackDef '" + id + "': durationMs must be > 0");
            return new AttackInstance(this);
        }
    }
}
