package btm.sword.system.attack.dev;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import btm.sword.Sword;
import btm.sword.system.attack.HitValuePacket;
import btm.sword.system.attack.simulation.KeyframeType;
import btm.sword.system.attack.simulation.VolumeKeyframe;
import btm.sword.system.attack.simulation.VolumeShape;
import btm.sword.system.control.PredicateRunnablePair;
import btm.sword.system.control.TimeArbiter;
import btm.sword.system.entity.impl.Combatant;
import btm.sword.system.entity.impl.SwordPlayer;
import btm.sword.system.inventory.menu.dev.AttackEditorMenu;
import btm.sword.utility.Debug;
import btm.sword.utility.Prefab;
import btm.sword.utility.display.DrawUtil;
import btm.sword.utility.display.ParticleWrapper;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Dev tool actions for manually placing attack path points with the volume attack wand.
 *
 * <h2>Controls (while holding the wand)</h2>
 * <ul>
 *   <li><b>SHIFT+LEFT</b> — {@link #toggleRecording}: starts or stops the recording session.</li>
 *   <li><b>RIGHT</b> — {@link #placePoint}: deposits a world-space tip point at the player's
 *       look direction during an active recording.</li>
 *   <li><b>DROP+SWAP</b> — {@link #cyclePlacementMode}: cycles through
 *       {@link PlacementMode} values and clears any pending points.</li>
 * </ul>
 *
 * <p>Right-click places a dark-blue dust marker at the tip position. The current
 * {@link PlacementMode} determines how many points are required to form a complete
 * trajectory. When recording is stopped (SHIFT+LEFT) with enough points, the session
 * is saved to {@code plugins/sword/attacks/<name>.yml} and the editor opens.</p>
 */
public final class SweepRecordingAction {

    /** Length of the north-arrow rendered at the recording origin, in blocks. */
    private static final float ARROW_LENGTH = 1.5f;

    /** Render tick period for the recording visualization loop, in milliseconds (20 Hz). */
    private static final int RENDER_PERIOD_MS = 50;

    /** Dust colour for the origin crosshair and north arrow (red). */
    private static final Particle.DustOptions ORIGIN_DUST =
        new Particle.DustOptions(Color.fromRGB(220, 30, 30), 0.7f);

    // ── Age-based gradient: newest (age 0) = dark blue → mid-gray (age 3+) ────
    private static final Particle.DustOptions DUST_AGE_0 =
        new Particle.DustOptions(Color.fromRGB(30, 80, 220), 1.5f);
    private static final Particle.DustOptions DUST_AGE_1 =
        new Particle.DustOptions(Color.fromRGB(73, 107, 193), 1.5f);
    private static final Particle.DustOptions DUST_AGE_2 =
        new Particle.DustOptions(Color.fromRGB(117, 133, 167), 1.5f);
    private static final Particle.DustOptions DUST_AGE_OLD =
        new Particle.DustOptions(Color.fromRGB(160, 160, 160), 1.5f);

    /** Yellow dust used to draw the live raycast ray cursor in RAYCAST mode. */
    private static final Particle.DustOptions DUST_RAY_CURSOR =
        new Particle.DustOptions(Color.fromRGB(255, 230, 60), 0.7f);

    /** Orange dust used to mark the ray origin point of a placed RAYCAST point. */
    private static final Particle.DustOptions DUST_RAYCAST_ORIGIN =
        new Particle.DustOptions(Color.fromRGB(255, 130, 0), 1.5f);

    // ── BEZIER_CURVE role colors: start, c1, c2, end ─────────────────────────
    private static final Particle.DustOptions DUST_BEZIER_START =
        new Particle.DustOptions(Color.fromRGB(30, 200, 60), 1.5f);
    private static final Particle.DustOptions DUST_BEZIER_C1 =
        new Particle.DustOptions(Color.fromRGB(80, 150, 100), 1.5f);
    private static final Particle.DustOptions DUST_BEZIER_C2 =
        new Particle.DustOptions(Color.fromRGB(150, 100, 80), 1.5f);
    private static final Particle.DustOptions DUST_BEZIER_END =
        new Particle.DustOptions(Color.fromRGB(200, 50, 50), 1.5f);

    private SweepRecordingAction() {}

    // ── SHIFT+LEFT ────────────────────────────────────────────────────────────

    /**
     * Toggles the recording session for the given player.
     * <ul>
     *   <li>IDLE → RECORDING: starts the session, shows a start message,
     *       and launches the visualization render loop.</li>
     *   <li>RECORDING → IDLE: stops the session. If enough points have been placed for the
     *       current {@link PlacementMode}, saves the draft and opens the editor.
     *       Otherwise shows an error with the required count.</li>
     * </ul>
     *
     * @param executor the combatant holding the volume-attack wand
     */
    public static void toggleRecording(Combatant executor) {
        if (!(executor instanceof SwordPlayer player)) return;
        AttackDevSession session = AttackDevSession.getOrCreate(player.player());

        if (session.getMode() == DevMode.RECORDING) {
            int placed = session.getPendingPoints().size();
            session.stopRecording();
            Debug.attackVolume("RECORDING STOPPED player=" + player.player().getName()
                + " points=" + placed);
            if (placed >= 1) {
                saveDraft(session, player);
            } else {
                player.player().sendActionBar(Component.text(
                    "[Dev] No points placed — nothing to save.", NamedTextColor.RED));
            }
        } else if (session.getMode() == DevMode.IDLE) {
            session.startRecording("sweep_draft");
            Debug.attackVolume("RECORDING STARTED player=" + player.player().getName());
            player.player().sendMessage(Component.text(
                "[Dev] Recording started. Right-click to place points. DROP+SWAP to cycle mode.",
                NamedTextColor.GREEN));
            launchRenderLoop(player, session);
        }
    }

    // ── RIGHT ─────────────────────────────────────────────────────────────────

    /**
     * Places a world-space tip point during an active recording session.
     * The tip is computed at {@link AttackDevSession#getRayOffset()} blocks from the player's
     * eye along the current look direction (or at the raycast hit for
     * {@link PlacementMode#RAYCAST}). A dust particle marks the placement.
     * No-op if the session is not in {@link DevMode#RECORDING}.
     *
     * @param executor the combatant holding the volume-attack wand
     */
    public static void placePoint(Combatant executor) {
        if (!(executor instanceof SwordPlayer player)) return;
        AttackDevSession session = AttackDevSession.get(player.player().getUniqueId());
        if (session == null || session.getMode() != DevMode.RECORDING) return;

        PlacementMode mode = session.getPlacementMode();
        Location tip;
        if (mode == PlacementMode.RAYCAST) {
            Location rayOrigin = player.locFromEyeDir(session.getRayOffset());
            tip = computeRaycastTip(player, session.getRaycastMaxDistance());
            session.addPendingPoint(new PlacedPoint(tip, mode, rayOrigin));
            Prefab.Particles.CREATE_DUST.apply(dustForNewPoint(session.getPendingPoints())).display(tip);
            Prefab.Particles.CREATE_DUST.apply(DUST_RAYCAST_ORIGIN).display(rayOrigin);
        } else {
            tip = computeWorldTip(player, session.getRayOffset());
            session.addPendingPoint(tip);
            // Immediate placement marker — color matches what the render loop assigns this point
            Prefab.Particles.CREATE_DUST.apply(dustForNewPoint(session.getPendingPoints())).display(tip);
        }
    }

    // ── DROP+SWAP ─────────────────────────────────────────────────────────────

    /**
     * Cycles to the next {@link PlacementMode} and clears any pending points.
     * Shows an actionbar message naming the newly active mode.
     * No-op if the session is not in {@link DevMode#RECORDING}.
     *
     * @param executor the combatant holding the volume-attack wand
     */
    public static void cyclePlacementMode(Combatant executor) {
        if (!(executor instanceof SwordPlayer player)) return;
        AttackDevSession session = AttackDevSession.get(player.player().getUniqueId());
        if (session == null || session.getMode() != DevMode.RECORDING) return;

        session.cyclePlacementMode();
        player.player().sendActionBar(Component.text(
            "[Dev] Mode: " + session.getPlacementMode().label(), NamedTextColor.YELLOW));
        if (session.getPlacementMode() == PlacementMode.LINE_SEGMENT) {
            player.player().sendMessage(Component.text(
                "[Dev] Tip: stand still and wait ~0.5s before placing each Line Segment point.",
                NamedTextColor.GRAY));
        }
        Debug.attackVolume("CYCLE MODE player=" + player.player().getName()
            + " mode=" + session.getPlacementMode().name());
    }

    // ── Saving ────────────────────────────────────────────────────────────────

    /**
     * Converts the session's pending points into a {@link KeyframedTrajectory} edit session.
     *
     * <p>No YAML is written here — the file is only persisted when the user explicitly saves
     * via {@link btm.sword.system.attack.dev.SaveConfirmDialog}. Each pending point is
     * converted to local space using the reference origin captured at recording start, then
     * wrapped in a {@link VolumeKeyframe}. T-values are distributed evenly across
     * {@code [0, 1]}. Points placed in {@link PlacementMode#LINE_SEGMENT} set
     * {@code linearToNext=true} on their keyframe.</p>
     */
    private static void saveDraft(AttackDevSession session, SwordPlayer player) {
        List<PlacedPoint> points = session.getPendingPoints();
        int n = points.size();

        File attacksDir = new File(Sword.getInstance().getDataFolder(), "attacks");
        String name = nextAvailableName("sweep_draft", attacksDir);

        Vector3f refOrigin = session.getRecordingRefOrigin();
        Vector3f size = session.getCurrentPlacementSize();

        // Pre-compute inverse yaw rotation to convert world-space offsets to local space.
        // The world transform during playback applies rotateY(-yaw), so local positions must
        // be stored as rotateY(+yaw) applied to the world offset.
        float refYawRad = (float) Math.toRadians(session.getRecordingRefYaw());
        float cosY = (float) Math.cos(refYawRad);
        float sinY = (float) Math.sin(refYawRad);

        List<VolumeKeyframe> keyframes = new ArrayList<>(n);
        int bezierCount = 0;
        for (int i = 0; i < n; i++) {
            float t = n == 1 ? 0f : (float) i / (n - 1);
            Location world = points.get(i).location();
            float wx = (float) world.getX() - refOrigin.x;
            float wy = (float) world.getY() - refOrigin.y;
            float wz = (float) world.getZ() - refOrigin.z;
            // Apply rotateY(refYaw): x' = cos*wx + sin*wz,  z' = -sin*wx + cos*wz
            Vector3f local = new Vector3f(cosY * wx + sinY * wz, wy, -sinY * wx + cosY * wz);
            PlacementMode pointMode = points.get(i).mode();
            boolean linearToNext = pointMode == PlacementMode.LINE_SEGMENT;
            KeyframeType kfType;
            if (pointMode == PlacementMode.BEZIER_CURVE) {
                kfType = switch (bezierCount % 4) {
                    case 0 -> KeyframeType.BEZIER_START;
                    case 1 -> KeyframeType.BEZIER_C1;
                    case 2 -> KeyframeType.BEZIER_C2;
                    default -> KeyframeType.BEZIER_END;
                };
                bezierCount++;
            } else if (pointMode == PlacementMode.LINE_SEGMENT) {
                kfType = KeyframeType.LINE;
            } else if (pointMode == PlacementMode.ORIGIN_RAY) {
                kfType = KeyframeType.ORIGIN_RAY;
            } else if (pointMode == PlacementMode.RAYCAST) {
                kfType = KeyframeType.RAYCAST;
            } else {
                kfType = KeyframeType.STANDARD;
            }
            float rayOffset = kfType == KeyframeType.ORIGIN_RAY ? session.getRayOffset() : 0f;
            Vector3f localRayOrigin = null;
            if (kfType == KeyframeType.RAYCAST && points.get(i).rayOrigin() != null) {
                Location worldRayOrigin = points.get(i).rayOrigin();
                float rox = (float) worldRayOrigin.getX() - refOrigin.x;
                float roy = (float) worldRayOrigin.getY() - refOrigin.y;
                float roz = (float) worldRayOrigin.getZ() - refOrigin.z;
                localRayOrigin = new Vector3f(cosY * rox + sinY * roz, roy, -sinY * rox + cosY * roz);
            }
            keyframes.add(new VolumeKeyframe(
                t, local, new Vector3f(size), new Quaternionf(), VolumeShape.SPHERE, null, false, linearToNext, kfType, rayOffset, localRayOrigin));
        }

        int durationMs = session.getEditDurationMs();
        HitValuePacket placeholder = new HitValuePacket(() -> 0f, () -> 10, () -> 0, () -> 0f, () -> 0f);

        Debug.attackVolume("DRAFT ready '" + name + "' keyframes=" + n
            + " mode=" + session.getPlacementMode().name());
        player.player().sendMessage(Component.text(
            "[Dev] " + n + " point" + (n == 1 ? "" : "s") + " recorded — opening editor. Save when done.",
            NamedTextColor.AQUA));

        session.startEditing(name, keyframes, durationMs, placeholder);
        new AttackEditorMenu(player).open();
    }

    // ── Utilities ─────────────────────────────────────────────────────────────

    /**
     * Returns a name not already occupied in the {@link AttackRegistry} or on the filesystem.
     * If {@code base} is free, returns it unchanged. Otherwise appends {@code _1}, {@code _2}, …
     *
     * @param base the desired base name (e.g. {@code "sweep_draft"})
     * @param dir  the attacks directory to check for existing YAML files
     * @return the first available name
     */
    public static String nextAvailableName(String base, File dir) {
        if (!new File(dir, base + ".yml").exists()) {
            return base;
        }
        int n = 1;
        while (true) {
            String candidate = base + "_" + n;
            if (!new File(dir, candidate + ".yml").exists()) {
                return candidate;
            }
            n++;
        }
    }

    // ── Private rendering ─────────────────────────────────────────────────────

    /**
     * Launches a 10 Hz render loop that draws the origin crosshair/arrow and all placed
     * pending points. Auto-cancels when the session leaves RECORDING or the player goes offline.
     */
    private static void launchRenderLoop(SwordPlayer player, AttackDevSession session) {
        TimeArbiter.runTimeIndependentBukkitTaskOnTimer(
            () -> {
                renderOriginArrow(session);
                renderPlacedPoints(session.getPendingPoints());
                PlacementMode mode = session.getPlacementMode();
                if (mode == PlacementMode.RAYCAST || mode == PlacementMode.ORIGIN_RAY) {
                    renderRayCursorForSession(player, session);
                }
            },
            null,
            0, RENDER_PERIOD_MS,
            SweepRecordingAction.class, "launchRenderLoop",
            new PredicateRunnablePair(
                () -> session.getMode() != DevMode.RECORDING || !player.player().isOnline(),
                null)
        );
    }

    /**
     * Spawns {@link Particle#DUST} at each placed world position using per-point colors.
     *
     * <p>For {@link PlacementMode#BEZIER_CURVE} points: role color based on
     * {@code bezierPointIndex % 4} across all bezier points, cycling every 4
     * (start→green, c1→muted green, c2→muted red, end→red).</p>
     *
     * <p>For all other modes: age-based gradient where the newest point is dark blue,
     * the two preceding points step toward mid-gray, and anything older is flat mid-gray.</p>
     */
    private static void renderPlacedPoints(List<PlacedPoint> points) {
        int n = points.size();
        int bezierIndex = 0;
        for (int i = 0; i < n; i++) {
            PlacedPoint point = points.get(i);
            Particle.DustOptions dust;
            if (point.mode() == PlacementMode.BEZIER_CURVE) {
                dust = bezierRoleDust(bezierIndex % 4);
                bezierIndex++;
            } else {
                dust = ageDust(n - 1 - i);
            }
            Prefab.Particles.CREATE_DUST.apply(dust).display(point.location());
            if (point.mode() == PlacementMode.RAYCAST && point.rayOrigin() != null) {
                ParticleWrapper originDust = Prefab.Particles.CREATE_DUST.apply(DUST_RAYCAST_ORIGIN);
                originDust.display(point.rayOrigin());
                DrawUtil.secant(List.of(originDust), point.rayOrigin(), point.location(), 0.25);
            }
        }
    }

    /**
     * Returns the dust color for the most recently added point — used for the instant
     * placement marker so it matches what the render loop will assign to that point.
     */
    private static Particle.DustOptions dustForNewPoint(List<PlacedPoint> points) {
        if (points.isEmpty()) return DUST_AGE_0;
        PlacedPoint newest = points.getLast();
        if (newest.mode() == PlacementMode.BEZIER_CURVE) {
            long bezierCount = points.stream().filter(p -> p.mode() == PlacementMode.BEZIER_CURVE).count();
            return bezierRoleDust((int) ((bezierCount - 1) % 4));
        }
        return DUST_AGE_0;
    }

    private static Particle.DustOptions bezierRoleDust(int roleIndex) {
        return switch (roleIndex) {
            case 0 -> DUST_BEZIER_START;
            case 1 -> DUST_BEZIER_C1;
            case 2 -> DUST_BEZIER_C2;
            default -> DUST_BEZIER_END;
        };
    }

    private static Particle.DustOptions ageDust(int age) {
        return switch (age) {
            case 0 -> DUST_AGE_0;
            case 1 -> DUST_AGE_1;
            case 2 -> DUST_AGE_2;
            default -> DUST_AGE_OLD;
        };
    }

    /**
     * Renders a crosshair at the locked origin and a fixed north-pointing arrow (−Z axis).
     * Visible throughout the recording session.
     */
    private static void renderOriginArrow(AttackDevSession session) {
        Location origin = session.getLockedOrigin();
        if (origin == null) return;

        ParticleWrapper dust = Prefab.Particles.CREATE_DUST.apply(ORIGIN_DUST);
        Location center = origin.clone().add(0, 0.9, 0);
        dust.display(center.clone().add(0.3, 0, 0));
        dust.display(center.clone().add(-0.3, 0, 0));
        dust.display(center.clone().add(0, 0, 0.3));
        dust.display(center.clone().add(0, 0, -0.3));
        DrawUtil.line(List.of(dust), center, new Vector(0, 0, -1), ARROW_LENGTH, 0.2);
    }

    /**
     * Renders a yellow ray cursor each tick.
     *
     * <ul>
     *   <li>{@link PlacementMode#RAYCAST} — draws from the offset eye origin to the
     *       raycast hit (or max distance) in the look direction.</li>
     *   <li>{@link PlacementMode#ORIGIN_RAY} — draws from the height-adjusted eye to the
     *       tip point.</li>
     * </ul>
     */
    static void renderRayCursorForSession(SwordPlayer player, AttackDevSession session) {
        World world = player.world();

        Location start, end;

        if (session.getPlacementMode() == PlacementMode.ORIGIN_RAY) {
            // Anchor the visual ray at the locked recording origin so it doesn't wobble
            // as the player looks around. The tip tracks the current look direction.
            Location lockedOrigin = session.getLockedOrigin();
            if (lockedOrigin == null) return;
            Vector tip = player.locFromEyeDir(1).toVector();
            Vector toLookPos = tip.clone().subtract(lockedOrigin.toVector());
            start = lockedOrigin.clone().add(toLookPos.normalize().multiply(session.getRayOffset()));
            end = tip.toLocation(world);
        } else {
            // RAYCAST — ray start is eye + lookDir * rayOffset, end is raycast hit or max distance.
            start = player.locFromEyeDir(session.getRayOffset());
            end = player.locFromEyeDir(session.getRaycastMaxDistance());
        }

        DrawUtil.secant(List.of(Prefab.Particles.CREATE_DUST.apply(DUST_RAY_CURSOR)),
            start, end, 0.25);
    }

    /**
     * Returns the world-space tip position at {@code distance} blocks from the player's eye
     * along the current look direction.
     */
    static Location computeWorldTip(SwordPlayer player, float distance) {
        return player.locFromEyeDir(distance);
    }

    /**
     * returns the point
     */
    static Location computeRaycastTip(SwordPlayer player, float maxDistance) {
        return player.locFromEyeDir(maxDistance);
    }
}
