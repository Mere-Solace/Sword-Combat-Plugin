package btm.sword.system.attack.simulation;

import java.util.Set;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import btm.sword.system.attack.HitValuePacket;
import lombok.Getter;
import lombok.Setter;

/**
 * Represents an attack actively running in the {@link VolumeSimulation} loop.
 *
 * <p>Holds all simulation-side state needed to evaluate the attack each tick:
 * the attacker identity, trajectory, pre-allocated volume buffer, timing window,
 * hit-value data, the set of already-hit entities, and an optional end callback.</p>
 *
 * <p>When {@link #isLockOriginOnFire()} is {@code true} the world transform is built
 * from {@link #getLockedCenter()}, {@link #getLockedYaw()}, and
 * {@link #getLockedPitch()} — which are captured once at add-time — rather than the
 * live {@link EntitySnapshotMap} snapshot.</p>
 *
 * <p>Instances are created on the main thread and handed to
 * {@link VolumeSimulation#addAttack(SimulationAttack)} to be evaluated at 200 Hz
 * on the simulation thread. All mutable state ({@code hitThisAttack}, {@code prevT})
 * must be thread-safe at the call site.</p>
 */
@Getter
public final class SimulationAttack {

    private final UUID attackerUuid;
    private final VolumeTrajectory trajectory;
    private final Volume volume;
    private final long startMs;
    private final long durationMs;
    private final HitValuePacket hitValue;
    private final Set<UUID> hitThisAttack;
    @Nullable
    private final Runnable onEnd;

    // ── Orientation flags ─────────────────────────────────────────────────────
    private final boolean orientWithPitch;
    private final boolean lockOriginOnFire;

    /** Locked origin centre, set once at add-time when {@code lockOriginOnFire} is true. */
    @Nullable
    private final Vector3f lockedCenter;

    /** Locked yaw in degrees, set once at add-time when {@code lockOriginOnFire} is true. */
    @Nullable
    private final Float lockedYaw;

    /** Locked pitch in degrees, set once at add-time when {@code lockOriginOnFire} is true. */
    @Nullable
    private final Float lockedPitch;

    /**
     * Previous normalized time ({@code t}) evaluated in the last tick.
     * Initialized to {@code -1} so the first tick's {@code t=0} triggers {@code t=0} effects.
     * Updated by the simulation each tick via {@link #setPrevT(float)}.
     */
    @Setter
    private float prevT = -1f;

    /**
     * Constructs a simulation attack.
     *
     * @param attackerUuid     UUID of the entity performing the attack
     * @param trajectory       trajectory that populates {@code volume} each tick
     * @param volume           pre-allocated mutable volume buffer (reused every tick)
     * @param startMs          wall-clock start time in milliseconds
     * @param durationMs       total attack duration in milliseconds
     * @param hitValue         damage values applied on collision
     * @param hitThisAttack    thread-safe set of already-hit entity UUIDs
     * @param onEnd            optional main-thread callback fired when the attack expires
     * @param orientWithPitch  whether to apply the attacker's pitch to the world transform
     * @param lockOriginOnFire whether to lock the origin at fire time (ignored if true and
     *                         no snapshot is available)
     * @param lockedCenter     pre-captured locked origin centre; {@code null} if not locking
     * @param lockedYaw        pre-captured locked yaw in degrees; {@code null} if not locking
     * @param lockedPitch      pre-captured locked pitch in degrees; {@code null} if not locking
     */
    public SimulationAttack(
            UUID attackerUuid,
            VolumeTrajectory trajectory,
            Volume volume,
            long startMs,
            long durationMs,
            HitValuePacket hitValue,
            Set<UUID> hitThisAttack,
            @Nullable Runnable onEnd,
            boolean orientWithPitch,
            boolean lockOriginOnFire,
            @Nullable Vector3f lockedCenter,
            @Nullable Float lockedYaw,
            @Nullable Float lockedPitch) {
        this.attackerUuid = attackerUuid;
        this.trajectory = trajectory;
        this.volume = volume;
        this.startMs = startMs;
        this.durationMs = durationMs;
        this.hitValue = hitValue;
        this.hitThisAttack = hitThisAttack;
        this.onEnd = onEnd;
        this.orientWithPitch = orientWithPitch;
        this.lockOriginOnFire = lockOriginOnFire;
        this.lockedCenter = lockedCenter;
        this.lockedYaw = lockedYaw;
        this.lockedPitch = lockedPitch;
    }
}
