package btm.sword.system.attack.dev;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import btm.sword.system.attack.simulation.ControlPoint;
import btm.sword.system.attack.simulation.ControlPointSequence;
import btm.sword.system.attack.simulation.KeyframeType;
import btm.sword.system.attack.simulation.KeyframedSequence;
import btm.sword.system.attack.simulation.ObbVolume;
import btm.sword.system.attack.simulation.VolumeKeyframe;
import btm.sword.system.attack.simulation.VolumeSequence;
import btm.sword.system.attack.simulation.VolumeShape;
import btm.sword.system.control.PredicateRunnablePair;
import btm.sword.system.control.TimeArbiter;
import btm.sword.system.item.KeyRegistry;
import btm.sword.utility.Debug;
import btm.sword.utility.Prefab;
import btm.sword.utility.display.DrawUtil;
import btm.sword.utility.display.ObbWireframe;
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

    /** How many ticks to skip between grey (non-selected) keyframe renders. */
    private static final int GREY_RENDER_PERIOD = 3;

    private static Particle.DustOptions DUST_SELECTED =
        new Particle.DustOptions(Color.fromRGB(255, 170, 0), 0.7f);
    private static Particle.DustOptions DUST_DEFAULT =
        new Particle.DustOptions(Color.fromRGB(160, 160, 160), 0.2f);
    /** Bright cyan used for the live-playback hitbox outline. */
    private static Particle.DustOptions DUST_LIVE =
        new Particle.DustOptions(Color.fromRGB(100, 220, 255), 1.0f);

    /** Dark blue used for CTRL_POINT control-point handles in EDITING mode. */
    private static final Particle.DustOptions DUST_CTRL_POINT =
        new Particle.DustOptions(Color.fromRGB(30, 80, 220), 1.5f);

    /** Yellow used for the ghost interpolated trajectory path in EDITING mode and playback. */
    private static final Particle.DustOptions DUST_GHOST_PATH =
        new Particle.DustOptions(Color.fromRGB(255, 230, 60), 0.7f);

    // ── Per-KeyframeType wireframe colours ────────────────────────────────────
    private static final Particle.DustOptions DUST_TYPE_BEZIER_START =
        new Particle.DustOptions(Color.fromRGB(30, 200, 60), 1.0f);
    private static final Particle.DustOptions DUST_TYPE_BEZIER_C1 =
        new Particle.DustOptions(Color.fromRGB(80, 150, 100), 1.0f);
    private static final Particle.DustOptions DUST_TYPE_BEZIER_C2 =
        new Particle.DustOptions(Color.fromRGB(150, 100, 80), 1.0f);
    private static final Particle.DustOptions DUST_TYPE_BEZIER_END =
        new Particle.DustOptions(Color.fromRGB(200, 50, 50), 1.0f);
    private static final Particle.DustOptions DUST_TYPE_LINE =
        new Particle.DustOptions(Color.fromRGB(100, 190, 255), 1.0f);
    /** Dark blue wireframe for RAYCAST tip volumes. */
    private static final Particle.DustOptions DUST_TYPE_RAYCAST_TIP =
        new Particle.DustOptions(Color.fromRGB(30, 80, 220), 1.0f);
    /** Orange marker for RAYCAST ray origin points. */
    private static final Particle.DustOptions DUST_TYPE_RAYCAST_ORIGIN =
        new Particle.DustOptions(Color.fromRGB(255, 130, 0), 1.5f);
    /** Yellow wireframe for ORIGIN_RAY tip volumes and ray line. */
    private static final Particle.DustOptions DUST_TYPE_ORIGIN_RAY =
        new Particle.DustOptions(Color.fromRGB(255, 210, 40), 1.2f);

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

    /** Returns the wireframe dust colour for a given {@link KeyframeType}. */
    private static Particle.DustOptions dustForType(KeyframeType type) {
        return switch (type) {
            case BEZIER_START -> DUST_TYPE_BEZIER_START;
            case BEZIER_C1 -> DUST_TYPE_BEZIER_C1;
            case BEZIER_C2 -> DUST_TYPE_BEZIER_C2;
            case BEZIER_END -> DUST_TYPE_BEZIER_END;
            case LINE -> DUST_TYPE_LINE;
            case RAYCAST -> DUST_TYPE_RAYCAST_TIP;
            case ORIGIN_RAY -> DUST_TYPE_ORIGIN_RAY;
            default -> DUST_DEFAULT;
        };
    }

    /**
     * Starts a 50 ms main-thread loop that renders the live interpolated OBB of a wand
     * test attack as it plays out.
     *
     * <p>Only supports {@link KeyframedSequence} — silently no-ops for other trajectory
     * types. The OBB is rendered with a bright cyan outline at each tick.</p>
     *
     * <p>When {@code lockOriginOnFire} is {@code true}, the world transform is captured once
     * at call time and reused every tick — matching the simulation's frozen origin behaviour.
     * Otherwise, the player's live position and yaw are sampled each tick.</p>
     *
     * <p>The attack ray is drawn from the keyframe's stored ray origin (or the attacker's
     * bounding-box centre when none is stored) to the current volume centre. This matches
     * the capsule line-of-attack detection built by
     * {@link btm.sword.system.attack.simulation.VolumeSimulation}.</p>
     *
     * <p>The loop self-cancels when the attack duration has elapsed or the player goes offline.
     * No cleanup is required by the caller.</p>
     *
     * @param player           the player who fired the wand attack
     * @param sequence       the attack's trajectory (must be {@link KeyframedSequence})
     * @param startMs          wall-clock start time matching the {@link btm.sword.system.attack.simulation.SimulationAttack}
     * @param durationMs       total attack duration in milliseconds
     * @param lockOriginOnFire when {@code true}, origin is frozen at call time instead of
     *                         following the player each tick
     * @param orientWithPitch  whether the world transform should include pitch rotation
     */
    public static void startPlaybackVisualization(Player player, VolumeSequence sequence,
            long startMs, long durationMs, boolean lockOriginOnFire, boolean orientWithPitch) {
        if (!(sequence instanceof KeyframedSequence) && !(sequence instanceof ControlPointSequence)) return;

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

                // Live position wireframe
                sequence.sample(t, worldTransform, buffer);
                if (buffer.isSphere) {
                    ObbWireframe.renderSphere(world, buffer.center, buffer.halfExtents.x, DUST_LIVE);
                } else {
                    ObbWireframe.renderObb(world, buffer.center, buffer.halfExtents, buffer.rotation, DUST_LIVE);
                }

                // Attack ray: only drawn when the sampled keyframe is RAYCAST or ORIGIN_RAY
                // (buffer.rayOrigin is non-null only for those types).
                if (buffer.rayOrigin != null && Config.Debug.VISUALIZATION_SHOW_HITBOXES) {
                    ObbWireframe.drawEdge(world, buffer.rayOrigin, new Vector3f(buffer.center), DUST_GHOST_PATH);
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

    /**
     * Starts the per-session visualization loop for a CTRL_POINT EDITING session.
     * Renders control-point handles as dark blue dust with {@code [CP#n]} labels,
     * and a ghost interpolated path as light blue dust every 3rd tick.
     *
     * <p>Called from {@link AttackDevSession#startEditingCtrlPoint} — not from plugin startup.</p>
     *
     * @param session the session that just entered CTRL_POINT EDITING mode
     */
    public static void startCtrlPointForSession(AttackDevSession session) {
        Player player = session.getPlayer();
        notifyOn(player, "Edit");

        spawnCtrlPointLabels(session);

        BossBar bossBar = BossBar.bossBar(
            Component.text("[Dev] Edit (CP) — " + session.getCurrentAttackName(), NamedTextColor.GOLD),
            1.0f, BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS);
        player.showBossBar(bossBar);

        int[] tickCount = {0};
        TimeArbiter.runTimeIndependentBukkitTaskOnTimer(
            () -> {
                tickCount[0]++;
                if (session.getMode() != DevMode.EDITING || !player.isOnline()) return;
                renderCtrlPointSession(session, tickCount[0]);
                updateCtrlPointLabels(session);
            },
            null,
            0, TICK_MS,
            VolumeEditorMode.class, "ctrlPointLoop",
            new PredicateRunnablePair(
                () -> session.getMode() != DevMode.EDITING || !player.isOnline(),
                () -> {
                    removeLabels(session.getPlayer().getUniqueId());
                    if (player.isOnline()) {
                        player.hideBossBar(bossBar);
                        notifyOff(player, "Edit");
                    }
                }
            )
        );
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
        Set<Integer> rangeSelection = session.getSelectedKeyframeIndices();

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
            boolean isSelected = (i == selectedIdx) || rangeSelection.contains(i);

            // Grey keyframes render at a reduced rate to keep particle count low
            if (!isSelected && tickCount % GREY_RENDER_PERIOD != 0) continue;

            // Transform local center to world space
            Vector3f worldCenter = worldTransform.transformPosition(
                new Vector3f(kf.localPosition()), new Vector3f());

            Particle.DustOptions dust = isSelected ? DUST_SELECTED : dustForType(kf.keyframeType());

            if (kf.shape() == VolumeShape.SPHERE) {
                totalParticles += ObbWireframe.renderSphere(world, worldCenter, kf.halfExtents().x, dust);
            } else {
                // OBB
                Quaternionf worldRot = new Quaternionf(worldBaseRot).mul(kf.rotation());
                if (log) {
                    Debug.attackVolume("[VolumeEditorMode]   kf[" + i + "] local=" + fmtVec(kf.localPosition())
                        + " → world=" + fmtVec(worldCenter)
                        + " half=" + fmtVec(kf.halfExtents())
                        + (isSelected ? " [SELECTED]" : ""));
                }
                totalParticles += ObbWireframe.renderObb(world, worldCenter, kf.halfExtents(), worldRot, dust);
            }

            // RAYCAST: orange dot at ray origin + secant from origin to tip
            if (kf.keyframeType() == KeyframeType.RAYCAST && kf.localRayOrigin() != null) {
                Vector3f worldOrigin = worldTransform.transformPosition(
                    new Vector3f(kf.localRayOrigin()), new Vector3f());
                Location originLoc = new Location(world, worldOrigin.x, worldOrigin.y, worldOrigin.z);
                Location tipLoc = new Location(world, worldCenter.x, worldCenter.y, worldCenter.z);
                Prefab.Particles.CREATE_DUST.apply(DUST_TYPE_RAYCAST_ORIGIN).display(originLoc);
                DrawUtil.secant(List.of(Prefab.Particles.CREATE_DUST.apply(DUST_TYPE_RAYCAST_ORIGIN)),
                    originLoc, tipLoc, 0.2);
            }

            // ORIGIN_RAY: yellow secant from stored ray origin to tip
            if (kf.keyframeType() == KeyframeType.ORIGIN_RAY && kf.localRayOrigin() != null) {
                Vector3f worldOrigin = worldTransform.transformPosition(
                    new Vector3f(kf.localRayOrigin()), new Vector3f());
                Location originLoc = new Location(world, worldOrigin.x, worldOrigin.y, worldOrigin.z);
                Location tipLoc = new Location(world, worldCenter.x, worldCenter.y, worldCenter.z);
                Prefab.Particles.CREATE_DUST.apply(DUST_TYPE_ORIGIN_RAY).display(originLoc);
                DrawUtil.secant(List.of(Prefab.Particles.CREATE_DUST.apply(DUST_TYPE_ORIGIN_RAY)),
                    originLoc, tipLoc, 0.2);
            }
        }

        if (log && !keyframes.isEmpty()) {
            Debug.attackVolume("[VolumeEditorMode] spawned " + totalParticles + " particles this tick");
        }

        // Ghost rays from recorded origin to each RAYCAST/ORIGIN_RAY keyframe — only while holding the wand.
        boolean holdingWand = KeyRegistry.hasKey(
            session.getPlayer().getInventory().getItemInMainHand(),
            KeyRegistry.TEST_VOLUME_ATTACK_KEY);
        if (holdingWand && keyframes.size() >= 2 && tickCount % GREY_RENDER_PERIOD == 0
                && Config.Debug.VISUALIZATION_SHOW_HITBOXES) {
            for (VolumeKeyframe kf : keyframes) {
                if (kf.keyframeType() != KeyframeType.RAYCAST && kf.keyframeType() != KeyframeType.ORIGIN_RAY) {
                    continue;
                }
                if (kf.localRayOrigin() == null) continue;
                Vector3f worldCenter = worldTransform.transformPosition(
                    new Vector3f(kf.localPosition()), new Vector3f());
                Vector3f rayStart = worldTransform.transformPosition(
                    new Vector3f(kf.localRayOrigin()), new Vector3f());
                ObbWireframe.drawEdge(world, rayStart, worldCenter, DUST_GHOST_PATH);
            }
        }
    }

    private static void renderCtrlPointSession(AttackDevSession session, int tickCount) {
        Location loc = session.getPlayer().getLocation();
        World world = loc.getWorld();
        if (world == null) return;

        BoundingBox bb = session.getPlayer().getBoundingBox();
        Matrix4f worldTransform = buildWorldTransform(
            bb, loc.getYaw(), loc.getPitch(), session.isEditOrientWithPitch());
        Quaternionf worldBaseRot = worldTransform.getNormalizedRotation(new Quaternionf());

        List<ControlPoint> points = session.getEditControlPoints();
        if (points == null || points.isEmpty()) return;

        // Render each control point handle as a dark blue OBB
        if (Config.Debug.VISUALIZATION_SHOW_HITBOXES) {
            for (ControlPoint cp : points) {
                Vector3f worldCenter = worldTransform.transformPosition(
                    new Vector3f(cp.position()), new Vector3f());
                ObbWireframe.renderObb(world, worldCenter, cp.halfExtents(), worldBaseRot, DUST_CTRL_POINT);
            }
        }

        // Ghost interpolated path at 10 t-values, rendered every 3rd tick
        if (Config.Debug.VISUALIZATION_SHOW_HITBOXES && tickCount % GREY_RENDER_PERIOD == 0) {
            ControlPointSequence traj = new ControlPointSequence(points, session.getEditControlMode());
            ObbVolume buffer = new ObbVolume();
            for (int i = 0; i <= 9; i++) {
                float t = i / 9.0f;
                traj.sample(t, worldTransform, buffer);
                ObbWireframe.renderObb(world, buffer.center, buffer.halfExtents, buffer.rotation, DUST_GHOST_PATH);
            }
        }
    }

    private static void spawnCtrlPointLabels(AttackDevSession session) {
        Player player = session.getPlayer();
        World world = player.getWorld();
        List<ControlPoint> points = session.getEditControlPoints();
        if (points == null) return;

        List<TextDisplay> labels = new ArrayList<>(points.size());
        Location spawnLoc = player.getLocation();
        for (int i = 0; i < points.size(); i++) {
            final int idx = i;
            TextDisplay td = world.spawn(spawnLoc, TextDisplay.class, display -> {
                display.text(Component.text("[CP#" + idx + "]", NamedTextColor.GOLD, TextDecoration.BOLD));
                display.setSeeThrough(true);
                display.setBillboard(Display.Billboard.CENTER);
            });
            labels.add(td);
        }
        SESSION_LABELS.put(player.getUniqueId(), labels);
    }

    private static void updateCtrlPointLabels(AttackDevSession session) {
        Player player = session.getPlayer();
        List<TextDisplay> labels = SESSION_LABELS.get(player.getUniqueId());
        if (labels == null) return;

        List<ControlPoint> points = session.getEditControlPoints();
        if (points == null || labels.size() != points.size()) {
            removeLabels(player.getUniqueId());
            spawnCtrlPointLabels(session);
            return;
        }

        Location loc = player.getLocation();
        BoundingBox bb = player.getBoundingBox();
        Matrix4f worldTransform = buildWorldTransform(
            bb, loc.getYaw(), loc.getPitch(), session.isEditOrientWithPitch());

        for (int i = 0; i < points.size(); i++) {
            TextDisplay td = labels.get(i);
            if (!td.isValid()) continue;
            ControlPoint cp = points.get(i);
            Vector3f worldCenter = worldTransform.transformPosition(
                new Vector3f(cp.position()), new Vector3f());
            float labelY = worldCenter.y + cp.halfExtents().y + 0.3f;
            td.teleport(new Location(loc.getWorld(), worldCenter.x, labelY, worldCenter.z));
            td.setVisibleByDefault(true);
            player.showEntity(Sword.getInstance(), td);
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

            NamedTextColor color = show ? NamedTextColor.GOLD : NamedTextColor.GRAY;
            String typeLabel = kf.keyframeType() != KeyframeType.STANDARD
                ? " " + kf.keyframeType().name() : "";
            td.text(Component.text("#" + i + typeLabel, color, TextDecoration.BOLD));
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

    private static String fmtVec(Vector3f v) {
        return String.format("(%.2f,%.2f,%.2f)", v.x, v.y, v.z);
    }
}
