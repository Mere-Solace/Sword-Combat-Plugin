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
 */
@Getter
public final class ActiveAttack {

    private final UUID attackerUuid;
    private final Trajectory trajectory;
    /** Pre-allocated output buffer reused each tick to avoid per-frame allocation. */
    private final VolumeOutput volumeOutput;
    private final long startMs;
    private final long durationMs;
    private final HitValuePacket hitValue;
    /** UUIDs struck by this attack — prevents the same entity being hit twice. */
    private final Set<UUID> hitThisAttack = ConcurrentHashMap.newKeySet();
    @Nullable private final Runnable onEnd;

    /**
     * Creates a new active attack. {@code startMs} is captured at construction time.
     *
     * @param attackerUuid UUID of the attacking entity
     * @param trajectory   trajectory that defines the attack volume over time
     * @param durationMs   total attack duration in milliseconds
     * @param hitValue     damage values to deliver on each hit
     * @param onEnd        optional callback invoked on the main thread when the attack expires
     */
    public ActiveAttack(UUID attackerUuid, Trajectory trajectory, long durationMs,
                        HitValuePacket hitValue, @Nullable Runnable onEnd) {
        this.attackerUuid = attackerUuid;
        this.trajectory = trajectory;
        this.volumeOutput = trajectory.createOutput();
        this.startMs = System.currentTimeMillis();
        this.durationMs = durationMs;
        this.hitValue = hitValue;
        this.onEnd = onEnd;
    }
}
