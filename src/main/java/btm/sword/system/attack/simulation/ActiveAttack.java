package btm.sword.system.attack.simulation;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import btm.sword.system.attack.HitValuePacket;
import lombok.Getter;

/**
 * Represents an in-flight attack being evaluated by the {@link VolumeSimulation}.
 * <p>
 * Constructed on the main thread and handed off to {@link VolumeSimulation#addAttack(ActiveAttack)}.
 * After that point it is owned exclusively by the simulation thread until {@code t >= 1.0},
 * at which point the optional {@link #onEnd} callback is posted back to the main thread.
 * </p>
 *
 * <p>The {@link Volume} buffer is allocated by the caller before construction and reused
 * every tick to avoid per-frame allocation in the 200 Hz loop. The type must match the
 * trajectory: {@link ObbVolume} for {@link KeyframedTrajectory},
 * {@link CapsuleVolume} for {@link SweepTrajectory}.</p>
 */
@Getter
public final class ActiveAttack {

    private final UUID attackerUuid;
    private final VolumeTrajectory trajectory;
    /** Pre-allocated output buffer reused each tick. Must match the trajectory's expected type. */
    private final Volume volume;
    private final long startMs;
    private final long durationMs;
    private final HitValuePacket hitValue;
    /** UUIDs struck by this attack — prevents the same entity being hit twice per swing. */
    private final Set<UUID> hitThisAttack = ConcurrentHashMap.newKeySet();
    @Nullable private final Runnable onEnd;

    /**
     * Creates a new active attack. {@code startMs} is captured at construction time.
     *
     * @param attackerUuid UUID of the attacking entity
     * @param trajectory   trajectory that defines the attack volume over time
     * @param volume       pre-allocated output buffer compatible with {@code trajectory}
     * @param durationMs   total attack duration in milliseconds
     * @param hitValue     damage values to deliver on each hit
     * @param onEnd        optional callback invoked on the main thread when the attack expires
     */
    public ActiveAttack(UUID attackerUuid, VolumeTrajectory trajectory, Volume volume,
                        long durationMs, HitValuePacket hitValue, @Nullable Runnable onEnd) {
        this.attackerUuid = attackerUuid;
        this.trajectory = trajectory;
        this.volume = volume;
        this.startMs = System.currentTimeMillis();
        this.durationMs = durationMs;
        this.hitValue = hitValue;
        this.onEnd = onEnd;
    }
}
