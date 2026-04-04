package btm.sword.system.attack.simulation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import btm.sword.Sword;
import btm.sword.utility.Debug;

/**
 * Off-thread 5ms fixed-rate collision loop for the new attack system.
 * <p>
 * Runs at 200 Hz on a dedicated simulation thread. Each tick:
 * <ol>
 *   <li>Evaluates all {@link SimulationAttack} trajectories against the attacker's latest
 *       {@link EntitySnapshotMap} snapshot to produce world-space volumes.</li>
 *   <li>Inserts volumes into a {@link SpatialGrid} for broad-phase culling.</li>
 *   <li>Queries the grid per entity, runs {@link Volume#intersects} for narrow-phase
 *       confirmation, and posts {@link CollisionEvent}s to {@link CollisionEventBridge}.</li>
 *   <li>Expires attacks whose normalized time has reached {@code 1.0} and posts their
 *       {@code onEnd} callbacks back to the main thread.</li>
 * </ol>
 * </p>
 *
 * <p>Zero Bukkit calls occur inside the tick — all entity data is read from immutable
 * {@link EntitySnapshotMap} snapshots written by the main thread every 50ms.</p>
 */
public final class VolumeSimulation {

    /** Global singleton instance. */
    public static final VolumeSimulation INSTANCE = new VolumeSimulation();

    private static final int TICK_RATE_MS = 5;

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "sword-volume-sim");
        thread.setDaemon(true);
        return thread;
    });

    private final SpatialGrid spatialGrid = new SpatialGrid();
    private final CopyOnWriteArrayList<SimulationAttack> activeAttacks = new CopyOnWriteArrayList<>();
    /** O(1) lookup from attacker UUID to active attack for the narrow phase. */
    private final ConcurrentHashMap<UUID, SimulationAttack> attackByOwner = new ConcurrentHashMap<>();

    private VolumeSimulation() {}

    /**
     * Starts the 5ms simulation loop. Called from {@code Sword.onEnable()}.
     */
    public void start() {
        executor.scheduleAtFixedRate(this::tick, 0, TICK_RATE_MS, TimeUnit.MILLISECONDS);
    }

    /**
     * Stops the simulation loop gracefully, waiting up to 5 seconds for the current tick
     * to finish. Called from {@code Sword.onDisable()}.
     */
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Registers an attack with the simulation. Safe to call from the main thread.
     * The attack will be evaluated on the next simulation tick.
     *
     * @param attack the attack to register
     */
    public void addAttack(SimulationAttack attack) {
        activeAttacks.add(attack);
        attackByOwner.put(attack.getAttackerUuid(), attack);
    }

    private void tick() {
        try {
            doTick();
        } catch (Exception e) {
            Debug.system("VolumeSimulation tick threw: " + e.getMessage());
        }
    }

    private void doTick() {
        long now = System.currentTimeMillis();
        spatialGrid.clear();

        List<SimulationAttack> finished = new ArrayList<>();

        // ── Trajectory evaluation + broad phase ──────────────────────────────
        for (SimulationAttack attack : activeAttacks) {
            float t = (float) ((now - attack.getStartMs()) / (double) attack.getDurationMs());
            if (t >= 1.0f) {
                finished.add(attack);
                continue;
            }

            EntitySnapshotMap.EntityBoundingBoxSnapshot snap =
                EntitySnapshotMap.INSTANCE.get(attack.getAttackerUuid());
            if (snap == null) continue;

            // Minecraft yaw is clockwise from south; negate for JOML's counter-clockwise rotateY
            Matrix4f worldTransform = new Matrix4f()
                .translate(snap.center())
                .rotateY(-(float) Math.toRadians(snap.yaw()));

            attack.getTrajectory().sample(t, worldTransform, attack.getVolume());
            spatialGrid.insert(
                attack.getAttackerUuid(),
                attack.getVolume().aabbMin,
                attack.getVolume().aabbMax
            );
        }

        // ── Narrow phase ─────────────────────────────────────────────────────
        for (Map.Entry<UUID, EntitySnapshotMap.EntityBoundingBoxSnapshot> entry :
                EntitySnapshotMap.INSTANCE.entrySet()) {

            UUID entityUuid = entry.getKey();
            EntitySnapshotMap.EntityBoundingBoxSnapshot snap = entry.getValue();

            List<SpatialGrid.VolumeEntry> candidates = spatialGrid.query(snap.min(), snap.max());
            for (SpatialGrid.VolumeEntry candidate : candidates) {
                if (candidate.ownerUuid().equals(entityUuid)) continue;

                SimulationAttack attack = attackByOwner.get(candidate.ownerUuid());
                if (attack == null) continue;
                if (!attack.getHitThisAttack().add(entityUuid)) continue;

                if (!attack.getVolume().intersects(snap.min(), snap.max())) {
                    attack.getHitThisAttack().remove(entityUuid);
                    continue;
                }

                Vector3f contact = new Vector3f(attack.getVolume().aabbMin)
                    .add(attack.getVolume().aabbMax).mul(0.5f);

                CollisionEventBridge.INSTANCE.post(
                    new CollisionEvent(attack.getAttackerUuid(), entityUuid, contact, attack.getHitValue())
                );
            }
        }

        // ── Expire finished attacks ───────────────────────────────────────────
        if (!finished.isEmpty()) {
            activeAttacks.removeAll(finished);
            for (SimulationAttack attack : finished) {
                attackByOwner.remove(attack.getAttackerUuid());
                if (attack.getOnEnd() != null) {
                    Bukkit.getScheduler().runTask(Sword.getInstance(), attack.getOnEnd());
                }
            }
        }
    }
}
