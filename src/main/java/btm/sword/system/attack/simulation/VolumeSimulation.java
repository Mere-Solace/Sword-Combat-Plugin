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
import org.bukkit.World;
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
        processBroadPhase(now, finished);
        processNarrowPhase();
        expireFinished(finished);
    }

    /**
     * Evaluates each active attack's trajectory at normalized time {@code t}, inserts its
     * volume into the {@link SpatialGrid}, and dispatches any keyframe effects.
     * Attacks whose {@code t >= 1.0} are added to {@code finished} and skipped.
     */
    private void processBroadPhase(long now, List<SimulationAttack> finished) {
        for (SimulationAttack attack : activeAttacks) {
            float t = (float) ((now - attack.getStartMs()) / (double) attack.getDurationMs());
            if (t >= 1.0f) {
                finished.add(attack);
                continue;
            }

            // Determine origin: locked (captured at fire time) or live snapshot
            Vector3f origin;
            float yaw;
            float pitch;
            if (attack.isLockOriginOnFire() && attack.getLockedCenter() != null) {
                origin = attack.getLockedCenter();
                yaw = attack.getLockedYaw() != null ? attack.getLockedYaw() : 0f;
                pitch = attack.getLockedPitch() != null ? attack.getLockedPitch() : 0f;
            } else {
                EntitySnapshotMap.EntityBoundingBoxSnapshot snap =
                    EntitySnapshotMap.INSTANCE.get(attack.getAttackerUuid());
                if (snap == null) continue;
                origin = snap.center();
                yaw = snap.yaw();
                pitch = snap.pitch();
            }

            // Minecraft yaw is clockwise from south; negate for JOML's counter-clockwise rotateY
            Matrix4f worldTransform = new Matrix4f()
                .translate(origin)
                .rotateY(-(float) Math.toRadians(yaw));
            if (attack.isOrientWithPitch()) {
                worldTransform.rotateX((float) Math.toRadians(pitch));
            }

            attack.getTrajectory().sample(t, worldTransform, attack.getVolume());

            // For RAYCAST / ORIGIN_RAY keyframes only, build a thin ray capsule from the
            // recorded origin to the keyframe tip for line-of-attack detection. obbVol.rayOrigin
            // is non-null only for those keyframe types (written by KeyframedTrajectory.sample).
            if (attack.getVolume() instanceof ObbVolume obbVol && obbVol.rayOrigin != null) {
                if (attack.getRayVolume() == null) {
                    attack.setRayVolume(new CapsuleVolume());
                }
                CapsuleVolume ray = attack.getRayVolume();
                Vector3f rayStart = obbVol.rayOrigin;
                ray.start.set(rayStart);
                ray.end.set(obbVol.center);
                ray.radius = 0.1f;
                float r = ray.radius;
                ray.aabbMin.set(
                    Math.min(rayStart.x, obbVol.center.x) - r,
                    Math.min(rayStart.y, obbVol.center.y) - r,
                    Math.min(rayStart.z, obbVol.center.z) - r);
                ray.aabbMax.set(
                    Math.max(rayStart.x, obbVol.center.x) + r,
                    Math.max(rayStart.y, obbVol.center.y) + r,
                    Math.max(rayStart.z, obbVol.center.z) + r);
                spatialGrid.insert(attack.getAttackerUuid(), ray.aabbMin, ray.aabbMax);
                // Also insert the volume AABB — the capsule AABB does not fully enclose the OBB
                spatialGrid.insert(
                    attack.getAttackerUuid(),
                    attack.getVolume().aabbMin,
                    attack.getVolume().aabbMax);
            } else {
                // Non-ray keyframe: clear any stale capsule so narrow phase does not test it
                attack.setRayVolume(null);
                spatialGrid.insert(
                    attack.getAttackerUuid(),
                    attack.getVolume().aabbMin,
                    attack.getVolume().aabbMax);
            }

            if (attack.getTrajectory() instanceof KeyframedTrajectory kt) {
                World world = Bukkit.getWorld(attack.getWorldUuid());
                if (world != null) {
                    EffectsDispatcher.dispatch(kt, attack, attack.getPrevT(), t, worldTransform, world);
                }
            }
            attack.setPrevT(t);

            Debug.attackVolume("TICK t=" + String.format("%.2f", t)
                + " aabbMin=" + fmtVec(attack.getVolume().aabbMin)
                + " aabbMax=" + fmtVec(attack.getVolume().aabbMax)
                + " entities_in_map=" + EntitySnapshotMap.INSTANCE.entrySet().size());
        }
    }

    /**
     * Queries the {@link SpatialGrid} for each tracked entity, runs the narrow-phase
     * intersection test, and posts {@link CollisionEvent}s to {@link CollisionEventBridge}.
     */
    private void processNarrowPhase() {
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

                CapsuleVolume ray = attack.getRayVolume();
                boolean hit = attack.getVolume().intersects(snap.min(), snap.max())
                    || (ray != null && ray.intersects(snap.min(), snap.max()));
                if (!hit) {
                    attack.getHitThisAttack().remove(entityUuid);
                    continue;
                }

                Vector3f contact = new Vector3f(attack.getVolume().aabbMin)
                    .add(attack.getVolume().aabbMax).mul(0.5f);

                Debug.attackVolume("NARROW_HIT attacker=" + attack.getAttackerUuid()
                    + " victim=" + entityUuid
                    + " victimAABB=[" + fmtVec(snap.min()) + " → " + fmtVec(snap.max()) + "]"
                    + " volAABB=[" + fmtVec(attack.getVolume().aabbMin) + " → " + fmtVec(attack.getVolume().aabbMax) + "]");

                CollisionEventBridge.INSTANCE.post(
                    new CollisionEvent(attack.getAttackerUuid(), entityUuid, contact,
                        attack.getHitValue(), attack.getKnockbackFunction())
                );
            }
        }
    }

    /**
     * Removes expired attacks from both the active list and the owner map, then
     * schedules any {@code onEnd} callbacks on the main thread.
     */
    private void expireFinished(List<SimulationAttack> finished) {
        if (finished.isEmpty()) return;
        activeAttacks.removeAll(finished);
        for (SimulationAttack attack : finished) {
            attackByOwner.remove(attack.getAttackerUuid());
            Debug.attackVolume("EXPIRE attacker=" + attack.getAttackerUuid()
                + " hits=" + attack.getHitThisAttack().size());
            if (attack.getOnEnd() != null) {
                Bukkit.getScheduler().runTask(Sword.getInstance(), attack.getOnEnd());
            }
        }
    }

    private static String fmtVec(Vector3f v) {
        return String.format("(%.2f,%.2f,%.2f)", v.x, v.y, v.z);
    }
}
