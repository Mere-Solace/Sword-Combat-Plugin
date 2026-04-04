package btm.sword.system.attack.simulation;

import java.util.Set;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import btm.sword.system.attack.HitValuePacket;
import lombok.Getter;

/**
 * Represents an attack actively running in the {@link VolumeSimulation} loop.
 *
 * <p>Holds all simulation-side state needed to evaluate the attack each tick:
 * the attacker identity, trajectory, pre-allocated volume buffer, timing window,
 * hit-value data, the set of already-hit entities, and an optional end callback.</p>
 *
 * <p>Instances are created on the main thread and handed to
 * {@link VolumeSimulation#addAttack(SimulationAttack)} to be evaluated at 200 Hz
 * on the simulation thread. All mutable state ({@code hitThisAttack}) must be
 * thread-safe at the call site.</p>
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

    /**
     * Constructs a simulation attack.
     *
     * @param attackerUuid  UUID of the entity performing the attack
     * @param trajectory    trajectory that populates {@code volume} each tick
     * @param volume        pre-allocated mutable volume buffer (reused every tick)
     * @param startMs       wall-clock start time in milliseconds
     * @param durationMs    total attack duration in milliseconds
     * @param hitValue      damage values applied on collision
     * @param hitThisAttack thread-safe set of already-hit entity UUIDs
     * @param onEnd         optional main-thread callback fired when the attack expires
     */
    public SimulationAttack(
            UUID attackerUuid,
            VolumeTrajectory trajectory,
            Volume volume,
            long startMs,
            long durationMs,
            HitValuePacket hitValue,
            Set<UUID> hitThisAttack,
            @Nullable Runnable onEnd) {
        this.attackerUuid = attackerUuid;
        this.trajectory = trajectory;
        this.volume = volume;
        this.startMs = startMs;
        this.durationMs = durationMs;
        this.hitValue = hitValue;
        this.hitThisAttack = hitThisAttack;
        this.onEnd = onEnd;
    }
}
