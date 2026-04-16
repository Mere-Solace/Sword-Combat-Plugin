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
import btm.sword.system.attack.def.AttackDef;
import btm.sword.system.attack.def.AttackDefSerializer;
import btm.sword.system.attack.def.AttackRegistry;
import btm.sword.system.attack.simulation.VolumeKeyframe;
import btm.sword.system.attack.simulation.VolumeShape;
import btm.sword.system.control.PredicateRunnablePair;
import btm.sword.system.control.TimeArbiter;
import btm.sword.system.entity.impl.Combatant;
import btm.sword.system.entity.impl.SwordPlayer;
import btm.sword.system.inventory.menu.dev.AttackEditorMenu;
import btm.sword.utility.Debug;
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

    /** Distance from the player's eye at which the tip sample is placed, in blocks. */
    private static final float TIP_DISTANCE = 1.5f;

    /** Length of the north-arrow rendered at the recording origin, in blocks. */
    private static final float ARROW_LENGTH = 1.5f;

    /** Render tick period for the recording visualization loop, in milliseconds (10 Hz). */
    private static final int RENDER_PERIOD_MS = 100;

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
            int required = session.getPlacementMode().requiredPoints();
            session.stopRecording();
            Debug.attackVolume("RECORDING STOPPED player=" + player.player().getName()
                + " points=" + placed);
            if (placed >= required) {
                saveDraft(session, player);
            } else {
                player.player().sendActionBar(Component.text(
                    "[Dev] Not enough points — need " + required
                        + " for " + session.getPlacementMode().label(),
                    NamedTextColor.RED));
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
     * The point is computed at {@link #TIP_DISTANCE} blocks from the player's eye along
     * the current look direction. A dark-blue dust particle marks the placement.
     * No-op if the session is not in {@link DevMode#RECORDING}.
     *
     * @param executor the combatant holding the volume-attack wand
     */
    public static void placePoint(Combatant executor) {
        if (!(executor instanceof SwordPlayer player)) return;
        AttackDevSession session = AttackDevSession.get(player.player().getUniqueId());
        if (session == null || session.getMode() != DevMode.RECORDING) return;

        Vector3f tip = computeWorldTip(player);
        session.addPendingPoint(tip);

        // Immediate placement marker — color matches what the render loop assigns this index
        int placedIndex = session.getPendingPoints().size() - 1;
        Particle.DustOptions dust = dustForIndex(placedIndex, 0, session.getPlacementMode());
        player.player().getWorld().spawnParticle(
            Particle.DUST, tip.x, tip.y, tip.z, 1, 0, 0, 0, 0, dust);

        Debug.attackVolume("PLACED POINT player=" + player.player().getName()
            + " count=" + session.getPendingPoints().size()
            + " pos=[" + String.format("%.2f,%.2f,%.2f", tip.x, tip.y, tip.z) + "]");
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
        Debug.attackVolume("CYCLE MODE player=" + player.player().getName()
            + " mode=" + session.getPlacementMode().name());
    }

    // ── Saving ────────────────────────────────────────────────────────────────

    /**
     * Converts the session's pending points into a VOLUME {@link AttackDef} and writes it
     * to {@code plugins/sword/attacks/<name>.yml} via {@link AttackDefSerializer}.
     *
     * <p>Each pending point is converted to local space using the reference origin captured
     * at recording start, then wrapped in a {@link VolumeKeyframe} using the session's
     * {@link AttackDevSession#getCurrentPlacementSize()}. T-values are distributed evenly
     * across {@code [0, 1]}.</p>
     *
     * <p><b>TODO #368:</b> Replace this placeholder mapping with a {@code ControlPointTrajectory}
     * once that type is available.</p>
     */
    private static void saveDraft(AttackDevSession session, SwordPlayer player) {
        List<Vector3f> points = session.getPendingPoints();
        int n = points.size();

        File attacksDir = new File(Sword.getInstance().getDataFolder(), "attacks");
        attacksDir.mkdirs();
        String name = nextAvailableName("sweep_draft", attacksDir);

        Vector3f refOrigin = session.getRecordingRefOrigin();
        Vector3f size = session.getCurrentPlacementSize();

        List<VolumeKeyframe> keyframes = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            float t = n == 1 ? 0f : (float) i / (n - 1);
            Vector3f world = points.get(i);
            Vector3f local = new Vector3f(
                world.x - refOrigin.x,
                world.y - refOrigin.y,
                world.z - refOrigin.z);
            keyframes.add(new VolumeKeyframe(
                t, local, new Vector3f(size), new Quaternionf(), VolumeShape.SPHERE, null, false));
        }

        int durationMs = 600;
        HitValuePacket placeholder = new HitValuePacket(() -> 0f, () -> 10, () -> 0, () -> 0f, () -> 0f);

        AttackDef draft = new AttackDef.Builder(name)
            .duration(durationMs)
            .onHit(placeholder)
            .keyframes(keyframes)
            .build();

        File file = new File(attacksDir, name + ".yml");
        AttackDefSerializer.save(file, draft);
        AttackRegistry.register(draft);

        Debug.attackVolume("SAVED draft '" + name + "' → " + file.getPath()
            + " keyframes=" + n + " mode=" + session.getPlacementMode().name());
        player.player().sendMessage(Component.text(
            "[Dev] Saved to attacks/" + name + ".yml — opening editor.", NamedTextColor.AQUA));

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
                renderOriginArrow(player.player().getWorld(), session);
                renderPlacedPoints(player.player().getWorld(), session.getPendingPoints(),
                    session.getPlacementMode());
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
     * Spawns {@link Particle#DUST} at each placed world position using mode-appropriate colors.
     *
     * <p>For {@link PlacementMode#BEZIER_CURVE}: fixed role colors by index
     * (start→green, c1→muted green, c2→muted red, end→red).</p>
     *
     * <p>For all other modes: age-based gradient where the newest point is dark blue,
     * the two preceding points step toward mid-gray, and anything older is flat mid-gray.</p>
     */
    private static void renderPlacedPoints(World world, List<Vector3f> positions, PlacementMode mode) {
        int n = positions.size();
        for (int i = 0; i < n; i++) {
            int age = n - 1 - i;
            Particle.DustOptions dust = dustForIndex(i, age, mode);
            Vector3f p = positions.get(i);
            world.spawnParticle(Particle.DUST, p.x, p.y, p.z, 1, 0, 0, 0, 0, dust);
        }
    }

    private static Particle.DustOptions dustForIndex(int index, int age, PlacementMode mode) {
        if (mode == PlacementMode.BEZIER_CURVE) {
            return switch (index) {
                case 0 -> DUST_BEZIER_START;
                case 1 -> DUST_BEZIER_C1;
                case 2 -> DUST_BEZIER_C2;
                default -> DUST_BEZIER_END;
            };
        }
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
    private static void renderOriginArrow(World world, AttackDevSession session) {
        Location origin = session.getLockedOrigin();
        if (origin == null) return;

        float ox = (float) origin.getX();
        float oy = (float) origin.getY() + 0.9f;
        float oz = (float) origin.getZ();

        world.spawnParticle(Particle.DUST, ox + 0.3f, oy, oz, 1, 0, 0, 0, 0, ORIGIN_DUST);
        world.spawnParticle(Particle.DUST, ox - 0.3f, oy, oz, 1, 0, 0, 0, 0, ORIGIN_DUST);
        world.spawnParticle(Particle.DUST, ox, oy, oz + 0.3f, 1, 0, 0, 0, 0, ORIGIN_DUST);
        world.spawnParticle(Particle.DUST, ox, oy, oz - 0.3f, 1, 0, 0, 0, 0, ORIGIN_DUST);

        for (float t = 0.2f; t <= ARROW_LENGTH; t += 0.2f) {
            world.spawnParticle(Particle.DUST, ox, oy, oz - t, 1, 0, 0, 0, 0, ORIGIN_DUST);
        }
    }

    /**
     * Returns the world-space tip position: player eye offset by {@link #TIP_DISTANCE}
     * blocks along the current look direction.
     */
    static Vector3f computeWorldTip(SwordPlayer player) {
        Location eye = player.player().getEyeLocation();
        Vector dir = player.player().getLocation().getDirection();
        return new Vector3f(
            (float) (eye.getX() + dir.getX() * TIP_DISTANCE),
            (float) (eye.getY() + dir.getY() * TIP_DISTANCE),
            (float) (eye.getZ() + dir.getZ() * TIP_DISTANCE));
    }
}
