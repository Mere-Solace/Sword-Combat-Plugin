package btm.sword.system.attack.dev;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.BoundingBox;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import btm.sword.Sword;
import btm.sword.config.Config;
import btm.sword.system.attack.simulation.KeyframedTrajectory;
import btm.sword.system.attack.simulation.ObbVolume;
import btm.sword.system.attack.simulation.VolumeKeyframe;
import btm.sword.system.attack.simulation.VolumeShape;
import btm.sword.system.attack.simulation.VolumeTrajectory;
import btm.sword.system.control.PredicateRunnablePair;
import btm.sword.system.control.TimeArbiter;
import btm.sword.utility.Debug;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

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

    /** How many ticks to skip between grey (non-selected) keyframe renders. */
    private static final int GREY_RENDER_PERIOD = 3;

    private static Particle.DustOptions DUST_SELECTED =
        new Particle.DustOptions(Color.fromRGB(255, 170, 0), 0.7f);
    private static Particle.DustOptions DUST_DEFAULT =
        new Particle.DustOptions(Color.fromRGB(160, 160, 160), 0.2f);
    /** Bright cyan used for the live-playback hitbox outline. */
    private static Particle.DustOptions DUST_LIVE =
        new Particle.DustOptions(Color.fromRGB(100, 220, 255), 1.0f);

    /**
     * Rebuilds {@link #DUST_SELECTED}, {@link #DUST_DEFAULT}, and {@link #DUST_LIVE} from the
     * current {@link btm.sword.config.Config.Debug} wireframe color/size entries.
     * Called by config consumers on load and hot-reload.
     */
    public static void rebuildDust() {
        DUST_SELECTED = new Particle.DustOptions(
            Config.Debug.WIREFRAME_SELECTED_COLOR,
            (float) Config.Debug.WIREFRAME_SELECTED_SIZE);
        DUST_DEFAULT = new Particle.DustOptions(
            Config.Debug.WIREFRAME_DEFAULT_COLOR,
            (float) Config.Debug.WIREFRAME_DEFAULT_SIZE);
        DUST_LIVE = new Particle.DustOptions(
            Config.Debug.WIREFRAME_LIVE_COLOR,
            (float) Config.Debug.WIREFRAME_LIVE_SIZE);
    }

    /** Text display entities spawned per editing session, keyed by player UUID. */
    private static final Map<UUID, List<TextDisplay>> SESSION_LABELS = new ConcurrentHashMap<>();

    private VolumeEditorMode() {}

    /**
     * Starts a 50 ms main-thread loop that renders the live interpolated OBB of a wand
     * test attack as it plays out.
     *
     * <p>Only supports {@link KeyframedTrajectory} — silently no-ops for other trajectory
     * types. The OBB is rendered with a bright cyan outline at each tick.</p>
     *
     * <p>When {@code lockOriginOnFire} is {@code true}, the world transform is captured once
     * at call time and reused every tick — matching the simulation's frozen origin behaviour.
     * Otherwise the player's live position and yaw are sampled each tick.</p>
     *
     * <p>The loop self-cancels when the attack duration has elapsed or the player goes offline.
     * No cleanup is required by the caller.</p>
     *
     * @param player           the player who fired the wand attack
     * @param trajectory       the attack's trajectory (must be {@link KeyframedTrajectory})
     * @param startMs          wall-clock start time matching the {@link btm.sword.system.attack.simulation.SimulationAttack}
     * @param durationMs       total attack duration in milliseconds
     * @param lockOriginOnFire when {@code true}, origin is frozen at call time instead of
     *                         following the player each tick
     * @param orientWithPitch  whether the world transform should include pitch rotation
     */
    public static void startPlaybackVisualization(Player player, VolumeTrajectory trajectory,
            long startMs, long durationMs, boolean lockOriginOnFire, boolean orientWithPitch) {
        if (!(trajectory instanceof KeyframedTrajectory)) return;

        ObbVolume buffer = new ObbVolume();

        // Capture a frozen transform now if origin is locked; null means sample live each tick.
        final Matrix4f frozenTransform;
        if (lockOriginOnFire) {
            Location loc = player.getLocation();
            frozenTransform = buildWorldTransform(
                player.getBoundingBox(), loc.getYaw(), loc.getPitch(), orientWithPitch);
        } else {
            frozenTransform = null;
        }

        TimeArbiter.runTimeIndependentBukkitTaskOnTimer(
            () -> {
                if (!player.isOnline()) return;
                long now = System.currentTimeMillis();
                float t = Math.min(1.0f, (now - startMs) / (float) durationMs);

                World world = player.getWorld();
                Matrix4f worldTransform;
                if (frozenTransform != null) {
                    worldTransform = frozenTransform;
                } else {
                    Location loc = player.getLocation();
                    worldTransform = buildWorldTransform(
                        player.getBoundingBox(), loc.getYaw(), loc.getPitch(), orientWithPitch);
                }

                trajectory.sample(t, worldTransform, buffer);
                if (buffer.isSphere) {
                    renderSphereWireframe(world, buffer.center, buffer.halfExtents.x, DUST_LIVE);
                } else {
                    renderObbWireframe(world, buffer.center, buffer.halfExtents, buffer.rotation, DUST_LIVE);
                }
            },
            null,
            0, 50,
            VolumeEditorMode.class, "playback-" + player.getName(),
            new PredicateRunnablePair(
                () -> !player.isOnline() || System.currentTimeMillis() - startMs >= durationMs,
                () -> { }
            )
        );
    }

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
        Debug.attackVolume("[VolumeEditorMode] starting edit loop for " + session.getPlayer().getName()
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
        Debug.attackVolume("[VolumeEditorMode] starting view loop for " + session.getPlayer().getName()
            + " attack=" + session.getCurrentAttackName());
        startLoop(session, DevMode.VIEWING);
    }

    private static void startLoop(AttackDevSession session, DevMode expectedMode) {
        Player player = session.getPlayer();
        String modeLabel = expectedMode == DevMode.EDITING ? "Edit" : "View";
        notifyOn(player, modeLabel);

        // Spawn TextDisplay labels for each keyframe (EDITING mode only)
        if (expectedMode == DevMode.EDITING) {
            spawnLabels(session);
        }

        // Show a boss bar for the duration of the wireframe session
        BossBar.Color barColor = expectedMode == DevMode.EDITING ? BossBar.Color.YELLOW : BossBar.Color.GREEN;
        BossBar bossBar = BossBar.bossBar(
            Component.text("[Dev] " + modeLabel + " — " + session.getCurrentAttackName(),
                expectedMode == DevMode.EDITING ? NamedTextColor.GOLD : NamedTextColor.GREEN),
            1.0f,
            barColor,
            BossBar.Overlay.PROGRESS
        );
        player.showBossBar(bossBar);

        int[] tickCount = {0};
        TimeArbiter.runTimeIndependentBukkitTaskOnTimer(
            () -> {
                tickCount[0]++;
                if (tickCount[0] % 40 == 0) {
                    Debug.attackVolume("[VolumeEditorMode] tick #" + tickCount[0]
                        + " mode=" + session.getMode()
                        + " keyframes=" + session.getEditKeyframes().size()
                        + " online=" + player.isOnline());
                }
                if (session.getMode() != expectedMode || !player.isOnline()) return;
                renderSession(session, tickCount[0]);
                if (expectedMode == DevMode.EDITING) {
                    updateLabels(session);
                }
            },
            null,
            0, TICK_MS,
            VolumeEditorMode.class, "startLoop-" + expectedMode.name(),
            new PredicateRunnablePair(
                () -> session.getMode() != expectedMode || !player.isOnline(),
                () -> {
                    Debug.attackVolume("[VolumeEditorMode] render loop ended for " + player.getName());
                    removeLabels(session.getPlayer().getUniqueId());
                    if (player.isOnline()) {
                        player.hideBossBar(bossBar);
                        notifyOff(player, modeLabel);
                    }
                }
            )
        );
    }

    private static void notifyOn(Player player, String modeLabel) {
        player.sendActionBar(Component.text("[Dev] Wireframe " + modeLabel + " ON", NamedTextColor.GOLD));
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.6f, 2.0f);
    }

    private static void notifyOff(Player player, String modeLabel) {
        player.sendActionBar(Component.text("[Dev] Wireframe " + modeLabel + " OFF", NamedTextColor.GRAY));
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.6f, 0.5f);
    }

    private static void renderSession(AttackDevSession session, int tickCount) {
        Location loc = session.getPlayer().getLocation();
        World world = loc.getWorld();
        if (world == null) {
            Debug.attackVolume("[VolumeEditorMode] world is null for player " + session.getPlayer().getName());
            return;
        }

        BoundingBox bb = session.getPlayer().getBoundingBox();
        Matrix4f worldTransform = buildWorldTransform(
            bb, loc.getYaw(), loc.getPitch(), session.isEditOrientWithPitch());

        Quaternionf worldBaseRot = worldTransform.getNormalizedRotation(new Quaternionf());

        List<VolumeKeyframe> keyframes = session.getEditKeyframes();
        // In VIEWING mode currentKeyframeIndex is -1; no highlight
        int selectedIdx = session.getMode() == DevMode.EDITING ? session.getCurrentKeyframeIndex() : -1;

        boolean log = tickCount % 40 == 0;
        if (log) {
            Debug.attackVolume("[VolumeEditorMode] rendering " + keyframes.size() + " keyframes for "
                + session.getPlayer().getName()
                + " bbCenter=(" + String.format("%.1f,%.1f,%.1f",
                    bb.getCenterX(), bb.getCenterY(), bb.getCenterZ()) + ")"
                + " yaw=" + String.format("%.1f", loc.getYaw())
                + " world=" + world.getName());
        }

        int totalParticles = 0;
        for (int i = 0; i < keyframes.size(); i++) {
            VolumeKeyframe kf = keyframes.get(i);
            boolean isSelected = (i == selectedIdx);

            // Grey keyframes render at a reduced rate to keep particle count low
            if (!isSelected && tickCount % GREY_RENDER_PERIOD != 0) continue;

            // Transform local center to world space
            Vector3f worldCenter = worldTransform.transformPosition(
                new Vector3f(kf.localPosition()), new Vector3f());

            Particle.DustOptions dust = isSelected ? DUST_SELECTED : DUST_DEFAULT;

            if (kf.shape() == VolumeShape.SPHERE) {
                totalParticles += renderSphereWireframe(world, worldCenter, kf.halfExtents().x, dust);
            } else {
                // OBB
                Quaternionf worldRot = new Quaternionf(worldBaseRot).mul(kf.rotation());
                if (log) {
                    Debug.attackVolume("[VolumeEditorMode]   kf[" + i + "] local=" + fmtVec(kf.localPosition())
                        + " → world=" + fmtVec(worldCenter)
                        + " half=" + fmtVec(kf.halfExtents())
                        + (isSelected ? " [SELECTED]" : ""));
                }
                totalParticles += renderObbWireframe(world, worldCenter, kf.halfExtents(), worldRot, dust);
            }
        }

        if (log && !keyframes.isEmpty()) {
            Debug.attackVolume("[VolumeEditorMode] spawned " + totalParticles + " particles this tick");
        }
    }

    // ── Transform helpers ─────────────────────────────────────────────────────

    /**
     * Builds the local-to-world transform for a player, matching the origin used by
     * {@link btm.sword.system.attack.simulation.VolumeSimulation} exactly.
     *
     * <p>When {@code orientWithPitch} is {@code true}, a pitch rotation is applied after
     * the yaw rotation so OBBs tilt with the player's view angle.</p>
     *
     * @param bb               the player's current bounding box
     * @param yaw              the player's current yaw in degrees
     * @param pitch            the player's current pitch in degrees
     * @param orientWithPitch  whether to apply pitch rotation
     * @return a Matrix4f: translate to BB centre → rotate by negated yaw → optionally rotate by pitch
     */
    private static Matrix4f buildWorldTransform(BoundingBox bb, float yaw, float pitch,
            boolean orientWithPitch) {
        Matrix4f m = new Matrix4f()
            .translate((float) bb.getCenterX(), (float) bb.getCenterY(), (float) bb.getCenterZ())
            .rotateY(-(float) Math.toRadians(yaw));
        if (orientWithPitch) {
            m.rotateX((float) Math.toRadians(pitch));
        }
        return m;
    }

    // ── TextDisplay label management ─────────────────────────────────────────

    private static void spawnLabels(AttackDevSession session) {
        Player player = session.getPlayer();
        World world = player.getWorld();

        List<VolumeKeyframe> keyframes = session.getEditKeyframes();
        List<TextDisplay> labels = new ArrayList<>(keyframes.size());
        Location spawnLoc = player.getLocation();

        for (int i = 0; i < keyframes.size(); i++) {
            final int idx = i;
            TextDisplay td = world.spawn(spawnLoc, TextDisplay.class, display -> {
                display.text(Component.text("#" + idx, NamedTextColor.GRAY, TextDecoration.BOLD));
                display.setSeeThrough(true);
                display.setBillboard(Display.Billboard.CENTER);
            });
            labels.add(td);
        }

        SESSION_LABELS.put(player.getUniqueId(), labels);
    }

    private static void updateLabels(AttackDevSession session) {
        Player player = session.getPlayer();
        List<TextDisplay> labels = SESSION_LABELS.get(player.getUniqueId());
        if (labels == null) return;

        List<VolumeKeyframe> keyframes = session.getEditKeyframes();

        // Reconcile: if keyframe count changed (add/remove), rebuild labels
        if (labels.size() != keyframes.size()) {
            removeLabels(player.getUniqueId());
            spawnLabels(session);
            return;
        }

        Location loc = player.getLocation();
        BoundingBox bb = player.getBoundingBox();
        Matrix4f worldTransform = buildWorldTransform(
            bb, loc.getYaw(), loc.getPitch(), session.isEditOrientWithPitch());

        int cursor = session.getCurrentKeyframeIndex();
        java.util.LinkedHashSet<Integer> sel = session.getSelectedKeyframeIndices();

        int last = getLastInSelection(session);
        for (int i = 0; i < keyframes.size(); i++) {
            TextDisplay td = labels.get(i);
            if (!td.isValid()) continue;

            VolumeKeyframe kf = keyframes.get(i);
            Vector3f worldCenter = worldTransform.transformPosition(
                new Vector3f(kf.localPosition()), new Vector3f());
            float labelY = worldCenter.y + kf.halfExtents().y + 0.3f;
            td.teleport(new Location(loc.getWorld(), worldCenter.x, labelY, worldCenter.z));

            boolean isPrimary = (i == cursor);
            boolean isLast = (i == last);

            // When a selection is active: only show label on first and last selected index
            boolean show = isPrimary || isLast;

            td.setVisibleByDefault(show);
            if (show) {
                player.showEntity(Sword.getInstance(), td);
            }

            NamedTextColor color = show ? NamedTextColor.GOLD
                : NamedTextColor.GRAY;
            td.text(Component.text("#" + i, color, TextDecoration.BOLD));
        }
    }

    private static void removeLabels(UUID playerUuid) {
        List<TextDisplay> labels = SESSION_LABELS.remove(playerUuid);
        if (labels != null) {
            labels.forEach(td -> { if (td.isValid()) td.remove(); });
        }
    }

    private static int getLastInSelection(AttackDevSession session) {
        return session.getSelectedKeyframeIndices().stream()
            .mapToInt(Integer::intValue).max().orElse(-1);
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
