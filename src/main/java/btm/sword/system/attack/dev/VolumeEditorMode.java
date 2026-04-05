package btm.sword.system.attack.dev;

import java.util.List;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.util.BoundingBox;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import btm.sword.Sword;
import btm.sword.system.attack.simulation.VolumeKeyframe;
import btm.sword.system.attack.simulation.VolumeShape;
import btm.sword.system.control.PredicateRunnablePair;
import btm.sword.system.control.TimeArbiter;

/**
 * Renders volume keyframe wireframes for all active {@link AttackDevSession}s in
 * {@link DevMode#EDITING} or {@link DevMode#VIEWING} mode.
 *
 * <p>A 100 ms main-thread repeating task iterates every active session. For each keyframe
 * the attacker's local-to-world transform is applied to produce a world-space volume, then
 * a wireframe is drawn with coloured {@link Particle#DUST} particles:</p>
 * <ul>
 *   <li>Gold — the currently selected keyframe ({@link AttackDevSession#getCurrentKeyframeIndex()}),
 *       only in EDITING mode</li>
 *   <li>Light-gray — all other keyframes (and all keyframes in VIEWING mode)</li>
 * </ul>
 *
 * <p>{@link VolumeShape#OBB} frames render as a 12-edge box wireframe;
 * {@link VolumeShape#SPHERE} frames render as three orthogonal ring circles.</p>
 *
 * <p>Call {@link #startForSession(AttackDevSession)} when entering EDITING mode, and
 * {@link #startViewingForSession(AttackDevSession)} when entering VIEWING mode. Tasks cancel
 * automatically when the session leaves the expected mode or the player goes offline.</p>
 */
public final class VolumeEditorMode {

    private static final int TICK_MS = 100;
    private static final float EDGE_SPACING = 0.18f;
    private static final int SPHERE_SEGMENTS = 16;

    private static final Particle.DustOptions DUST_SELECTED =
        new Particle.DustOptions(Color.fromRGB(255, 170, 0), 0.55f);
    private static final Particle.DustOptions DUST_DEFAULT =
        new Particle.DustOptions(Color.fromRGB(160, 160, 160), 0.4f);

    private VolumeEditorMode() {}

    /**
     * Starts the per-session visualization loop for an EDITING session.
     * The loop cancels automatically when the session leaves {@link DevMode#EDITING}
     * or the player goes offline.
     *
     * <p>Called from {@link AttackDevSession#startEditing} — not from plugin startup.</p>
     *
     * @param session the session that just entered EDITING mode
     */
    public static void startForSession(AttackDevSession session) {
        Sword.print("[VolumeEditorMode] starting edit loop for " + session.getPlayer().getName()
            + " attack=" + session.getCurrentAttackName());
        startLoop(session, DevMode.EDITING);
    }

    /**
     * Starts the per-session visualization loop for a VIEWING session.
     * The loop cancels automatically when the session leaves {@link DevMode#VIEWING}
     * or the player goes offline.
     *
     * <p>Called from {@link AttackDevSession#startViewing} — not from plugin startup.</p>
     *
     * @param session the session that just entered VIEWING mode
     */
    public static void startViewingForSession(AttackDevSession session) {
        Sword.print("[VolumeEditorMode] starting view loop for " + session.getPlayer().getName()
            + " attack=" + session.getCurrentAttackName());
        startLoop(session, DevMode.VIEWING);
    }

    private static void startLoop(AttackDevSession session, DevMode expectedMode) {
        int[] tickCount = {0};
        TimeArbiter.runTimeIndependentBukkitTaskOnTimer(
            () -> {
                tickCount[0]++;
                if (tickCount[0] % 40 == 0) {
                    Sword.print("[VolumeEditorMode] tick #" + tickCount[0]
                        + " mode=" + session.getMode()
                        + " keyframes=" + session.getEditKeyframes().size()
                        + " online=" + session.getPlayer().isOnline());
                }
                if (session.getMode() != expectedMode || !session.getPlayer().isOnline()) return;
                renderSession(session, tickCount[0]);
            },
            null,
            0, TICK_MS,
            VolumeEditorMode.class, "startLoop-" + expectedMode.name(),
            new PredicateRunnablePair(
                () -> session.getMode() != expectedMode || !session.getPlayer().isOnline(),
                () -> Sword.print("[VolumeEditorMode] render loop ended for "
                    + session.getPlayer().getName())
            )
        );
    }

    private static void renderSession(AttackDevSession session, int tickCount) {
        Location loc = session.getPlayer().getLocation();
        World world = loc.getWorld();
        if (world == null) {
            Sword.print("[VolumeEditorMode] world is null for player " + session.getPlayer().getName());
            return;
        }

        // Use bounding-box centre as the transform origin — matches VolumeSimulation exactly
        BoundingBox bb = session.getPlayer().getBoundingBox();

        // Minecraft yaw is clockwise from south; negate for JOML's counter-clockwise rotateY
        Matrix4f worldTransform = new Matrix4f()
            .translate((float) bb.getCenterX(), (float) bb.getCenterY(), (float) bb.getCenterZ())
            .rotateY(-(float) Math.toRadians(loc.getYaw()));

        Quaternionf worldBaseRot = worldTransform.getNormalizedRotation(new Quaternionf());

        List<VolumeKeyframe> keyframes = session.getEditKeyframes();
        // In VIEWING mode currentKeyframeIndex is -1; no highlight
        int selectedIdx = session.getMode() == DevMode.EDITING ? session.getCurrentKeyframeIndex() : -1;

        boolean log = tickCount % 40 == 0;
        if (log) {
            Sword.print("[VolumeEditorMode] rendering " + keyframes.size() + " keyframes for "
                + session.getPlayer().getName()
                + " bbCenter=(" + String.format("%.1f,%.1f,%.1f",
                    bb.getCenterX(), bb.getCenterY(), bb.getCenterZ()) + ")"
                + " yaw=" + String.format("%.1f", loc.getYaw())
                + " world=" + world.getName());
        }

        int totalParticles = 0;
        for (int i = 0; i < keyframes.size(); i++) {
            VolumeKeyframe kf = keyframes.get(i);

            // Transform local center to world space
            Vector3f worldCenter = worldTransform.transformPosition(
                new Vector3f(kf.localPosition()), new Vector3f());

            Particle.DustOptions dust = (i == selectedIdx) ? DUST_SELECTED : DUST_DEFAULT;

            if (kf.shape() == VolumeShape.SPHERE) {
                totalParticles += renderSphereWireframe(world, worldCenter, kf.halfExtents().x, dust);
            } else {
                // OBB
                Quaternionf worldRot = new Quaternionf(worldBaseRot).mul(kf.rotation());
                if (log) {
                    Sword.print("[VolumeEditorMode]   kf[" + i + "] local=" + fmtVec(kf.localPosition())
                        + " → world=" + fmtVec(worldCenter)
                        + " half=" + fmtVec(kf.halfExtents())
                        + (i == selectedIdx ? " [SELECTED]" : ""));
                }
                totalParticles += renderObbWireframe(world, worldCenter, kf.halfExtents(), worldRot, dust);
            }
        }

        if (log && !keyframes.isEmpty()) {
            Sword.print("[VolumeEditorMode] spawned " + totalParticles + " particles this tick");
        }
    }

    // ── OBB wireframe renderer ────────────────────────────────────────────────

    /**
     * Draws the 12 edges of an oriented bounding box using spaced dust particles.
     *
     * @param world       the world to spawn particles in
     * @param center      world-space OBB centre
     * @param halfExtents OBB half-extents along its local axes
     * @param rotation    OBB orientation as a unit quaternion
     * @param dust        the {@link Particle.DustOptions} colour/size to use
     * @return total number of particle spawn calls made
     */
    private static int renderObbWireframe(World world, Vector3f center, Vector3f halfExtents,
            Quaternionf rotation, Particle.DustOptions dust) {
        // Compute the three scaled local-axis vectors
        Vector3f ax = rotation.transform(new Vector3f(halfExtents.x, 0, 0), new Vector3f());
        Vector3f ay = rotation.transform(new Vector3f(0, halfExtents.y, 0), new Vector3f());
        Vector3f az = rotation.transform(new Vector3f(0, 0, halfExtents.z), new Vector3f());

        // 8 corners: bit pattern i → (±ax, ±ay, ±az)
        Vector3f[] corners = new Vector3f[8];
        for (int i = 0; i < 8; i++) {
            float sx = (i & 1) != 0 ? 1f : -1f;
            float sy = (i & 2) != 0 ? 1f : -1f;
            float sz = (i & 4) != 0 ? 1f : -1f;
            corners[i] = new Vector3f(center)
                .add(ax.x * sx, ax.y * sx, ax.z * sx)
                .add(ay.x * sy, ay.y * sy, ay.z * sy)
                .add(az.x * sz, az.y * sz, az.z * sz);
        }

        // 12 edges: pairs of corners that differ in exactly one bit
        int count = 0;
        count += drawEdge(world, corners[0], corners[1], dust);
        count += drawEdge(world, corners[2], corners[3], dust);
        count += drawEdge(world, corners[4], corners[5], dust);
        count += drawEdge(world, corners[6], corners[7], dust);
        count += drawEdge(world, corners[0], corners[2], dust);
        count += drawEdge(world, corners[1], corners[3], dust);
        count += drawEdge(world, corners[4], corners[6], dust);
        count += drawEdge(world, corners[5], corners[7], dust);
        count += drawEdge(world, corners[0], corners[4], dust);
        count += drawEdge(world, corners[1], corners[5], dust);
        count += drawEdge(world, corners[2], corners[6], dust);
        count += drawEdge(world, corners[3], corners[7], dust);
        return count;
    }

    // ── Sphere wireframe renderer ─────────────────────────────────────────────

    /**
     * Draws three orthogonal great circles (XZ, XY, YZ planes) to represent a sphere.
     *
     * @param world  the world to spawn particles in
     * @param center world-space sphere centre
     * @param radius sphere radius
     * @param dust   the {@link Particle.DustOptions} colour/size to use
     * @return total number of particle spawn calls made
     */
    private static int renderSphereWireframe(World world, Vector3f center, float radius,
            Particle.DustOptions dust) {
        int count = 0;
        for (int ring = 0; ring < 3; ring++) {
            for (int i = 0; i < SPHERE_SEGMENTS; i++) {
                float a0 = (float) (2 * Math.PI * i / SPHERE_SEGMENTS);
                float a1 = (float) (2 * Math.PI * (i + 1) / SPHERE_SEGMENTS);
                float cos0 = (float) Math.cos(a0);
                float sin0 = (float) Math.sin(a0);
                float cos1 = (float) Math.cos(a1);
                float sin1 = (float) Math.sin(a1);

                Vector3f p0;
                Vector3f p1;
                if (ring == 0) { // XZ plane
                    p0 = new Vector3f(center.x + radius * cos0, center.y, center.z + radius * sin0);
                    p1 = new Vector3f(center.x + radius * cos1, center.y, center.z + radius * sin1);
                } else if (ring == 1) { // XY plane
                    p0 = new Vector3f(center.x + radius * cos0, center.y + radius * sin0, center.z);
                    p1 = new Vector3f(center.x + radius * cos1, center.y + radius * sin1, center.z);
                } else { // YZ plane
                    p0 = new Vector3f(center.x, center.y + radius * cos0, center.z + radius * sin0);
                    p1 = new Vector3f(center.x, center.y + radius * cos1, center.z + radius * sin1);
                }
                count += drawEdge(world, p0, p1, dust);
            }
        }
        return count;
    }

    // ── Shared utilities ──────────────────────────────────────────────────────

    private static int drawEdge(World world, Vector3f a, Vector3f b, Particle.DustOptions dust) {
        float dx = b.x - a.x;
        float dy = b.y - a.y;
        float dz = b.z - a.z;
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1e-4f) return 0;

        int steps = Math.max(1, (int) (len / EDGE_SPACING));
        float sx = dx / steps;
        float sy = dy / steps;
        float sz = dz / steps;

        for (int i = 0; i <= steps; i++) {
            world.spawnParticle(Particle.DUST,
                a.x + sx * i, a.y + sy * i, a.z + sz * i,
                1, 0, 0, 0, 0, dust);
        }
        return steps + 1;
    }

    private static String fmtVec(Vector3f v) {
        return String.format("(%.2f,%.2f,%.2f)", v.x, v.y, v.z);
    }
}
