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
import btm.sword.utility.Prefab;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;


/**
 * Dev tool actions for recording sweep attack paths with the volume attack wand.
 *
 * <h2>Controls</h2>
 * <ul>
 *   <li><b>SHIFT+LEFT</b> — {@link #toggleRecording}: starts or stops the recording session.
 *       Starting the session also launches the continuous sampling loop.</li>
 * </ul>
 *
 * <p>While the session is active, holding right-click (blocking with a shield) samples the
 * player's look direction as a {@link RecordedSample} — a world-space tip position plus
 * an absolute timestamp. Samples accumulate in {@link AttackDevSession#getRecordingBuffer()}.
 * At save time the world-space positions are converted to the attacker's local frame using
 * the bounding-box origin and yaw captured when recording started.</p>
 *
 * <p>The player is locked to their starting position (XZ) while recording — any drift is
 * teleported back via {@code PlayerMoveEvent} in {@link btm.sword.listeners.PlayerListener}.
 * A forward-facing particle arrow at the origin shows the recording's reference direction.</p>
 *
 * <p>The sampling loop auto-cancels when the session leaves {@link DevMode#RECORDING}
 * (via a {@link PredicateRunnablePair} stop condition) and immediately triggers the
 * 5-second particle persistence phase.</p>
 */
public final class SweepRecordingAction {

    /** Distance from the player's eye at which the tip sample is placed, in blocks. */
    private static final float TIP_DISTANCE = 1.5f;

    /** Sampling and re-render interval in milliseconds (10 Hz). */
    private static final int SAMPLE_PERIOD_MS = 100;

    /** Number of persistence-phase iterations (each 100 ms → 5 seconds total). */
    private static final int PERSISTENCE_ITERATIONS = 50;

    /** Length of the north-arrow rendered at the recording origin, in blocks. */
    private static final float ARROW_LENGTH = 1.5f;

    /** Dust colour for the origin crosshair (red). */
    private static final Particle.DustOptions ORIGIN_DUST =
        new Particle.DustOptions(Color.fromRGB(220, 30, 30), 0.7f);

    /** Dust colour for the north-reference arrow (red). */
    private static final Particle.DustOptions ARROW_DUST =
        new Particle.DustOptions(Color.fromRGB(220, 30, 30), 0.7f);

    private SweepRecordingAction() {}

    // ── SHIFT+LEFT ────────────────────────────────────────────────────────────

    /**
     * Toggles the recording session for the given player.
     * <ul>
     *   <li>IDLE → RECORDING: starts the session, shows a start message,
     *       and launches the continuous sampling loop.</li>
     *   <li>RECORDING → IDLE: stops the session and shows a summary message.
     *       The sampling loop detects the mode change on its next tick,
     *       triggers the 5-second persistence phase, and self-cancels.</li>
     * </ul>
     *
     * @param executor the combatant holding the volume-attack wand
     */
    public static void toggleRecording(Combatant executor) {
        if (!(executor instanceof SwordPlayer player)) return;
        AttackDevSession session = AttackDevSession.getOrCreate(player.player());

        if (session.getMode() == DevMode.RECORDING) {
            List<RecordedSample> samples = session.stopRecording();
            int count = samples.size();
            Debug.attackVolume("RECORDING STOPPED player=" + player.player().getName()
                + " points=" + count);
            player.player().sendMessage(Component.text(
                "[Dev] Recording stopped — " + count + " point(s) captured.", NamedTextColor.YELLOW));
            if (count >= 2) {
                saveDraft(session, samples, player);
            }
        } else if (session.getMode() == DevMode.IDLE) {
            session.startRecording("sweep_draft");
            Debug.attackVolume("RECORDING STARTED player=" + player.player().getName());
            player.player().sendMessage(Component.text(
                "[Dev] Recording started. Hold right-click to sample.", NamedTextColor.GREEN));
            launchSamplingLoop(player, session);
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private static void launchSamplingLoop(SwordPlayer player, AttackDevSession session) {
        player.setBlocking(false);
        TimeArbiter.runTimeIndependentBukkitTaskOnTimer(
            () -> {
                // Capture a sample whenever the player is right-click-holding (blocking)
                if (player.player().isBlocking()) {
                    player.setBlocking(false);
                    session.getRecordingBuffer().add(
                        new RecordedSample(computeWorldTip(player), System.currentTimeMillis()));
                }

                // Live look-ray preview, all captured world positions, and origin arrow
                renderRay(player);
                List<Vector3f> positions = session.getRecordingBuffer().stream()
                    .map(RecordedSample::worldPosition)
                    .toList();
                renderCapturedPoints(player.player().getWorld(), positions);
                renderOriginArrow(player.player().getWorld(), session);
            },
            null,
            0, SAMPLE_PERIOD_MS,
            SweepRecordingAction.class, "launchSamplingLoop",
            // Stop when recording ends or the player goes offline; trigger persistence on stop
            new PredicateRunnablePair(
                () -> session.getMode() != DevMode.RECORDING || !player.player().isOnline(),
                () -> {
                    if (player.player().isOnline()) {
                        List<Vector3f> persistPositions = session.getRecordingBuffer().stream()
                            .map(RecordedSample::worldPosition)
                            .toList();
                        launchPersistencePhase(player, persistPositions);
                    }
                })
        );
    }

    /**
     * Converts the recorded world-space samples into a VOLUME {@link AttackDef} and writes it
     * to {@code plugins/sword/attacks/<name>.yml} via {@link AttackDefSerializer}.
     *
     * <p>The save name is resolved with {@link #nextAvailableName} to avoid overwriting existing
     * attacks. Each world-space tip is converted to the attacker's local frame using the
     * bounding-box centre and yaw captured at the start of the recording.</p>
     */
    private static void saveDraft(AttackDevSession session, List<RecordedSample> samples,
            SwordPlayer player) {
        File attacksDir = new File(Sword.getInstance().getDataFolder(), "attacks");
        attacksDir.mkdirs();

        String name = nextAvailableName("sweep_draft", attacksDir);

        long firstMs = samples.getFirst().capturedAtMs();
        long lastMs = samples.getLast().capturedAtMs();
        long spanMs = lastMs - firstMs;

        // Reference frame is always world-aligned (+Z = forward, matching the arrow).
        // No yaw rotation applied — local space == world-offset space.
        Vector3f refOrigin = session.getRecordingRefOrigin();

        List<VolumeKeyframe> keyframes = new ArrayList<>(samples.size());
        Vector3f defaultHalfExtents = new Vector3f(0.4f, 0.4f, 0.4f);
        for (RecordedSample s : samples) {
            float t = spanMs == 0 ? 0f : (float) (s.capturedAtMs() - firstMs) / spanMs;
            Vector3f localPos = new Vector3f(
                s.worldPosition().x - refOrigin.x,
                s.worldPosition().y - refOrigin.y,
                s.worldPosition().z - refOrigin.z);
            keyframes.add(new VolumeKeyframe(t, localPos, defaultHalfExtents, new Quaternionf(), VolumeShape.SPHERE, null, false));
        }

        // Placeholder hit values — intended to be edited in the saved YAML
        HitValuePacket placeholder = new HitValuePacket(() -> 0f, () -> 10, () -> 0, () -> 0f, () -> 0f);

        AttackDef draft = new AttackDef.Builder(name)
            .duration((int) spanMs)
            .onHit(placeholder)
            .keyframes(keyframes)
            .build();

        File file = new File(attacksDir, name + ".yml");
        AttackDefSerializer.save(file, draft);
        AttackRegistry.register(draft);

        Debug.attackVolume("SAVED draft '" + name + "' → " + file.getPath()
            + " keyframes=" + keyframes.size() + " span=" + spanMs + "ms");
        player.player().sendMessage(Component.text(
            "[Dev] Saved to attacks/" + name + ".yml — opening editor.", NamedTextColor.AQUA));

        // Start editing the newly recorded attack immediately
        session.startEditing(name, keyframes, (int) spanMs, placeholder);
        new AttackEditorMenu(player).open();
    }

    /**
     * Returns a name that is not already occupied in either the {@link AttackRegistry} or
     * on the filesystem. If {@code base} is free, returns it unchanged. Otherwise appends
     * {@code _1}, {@code _2}, … until a free slot is found.
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

    private static void launchPersistencePhase(SwordPlayer player, List<Vector3f> worldPositions) {
        if (worldPositions.isEmpty()) return;
        World world = player.player().getWorld();
        TimeArbiter.runFixedIterationTaskTimer(
            () -> renderCapturedPoints(world, worldPositions),
            null,
            0, SAMPLE_PERIOD_MS, PERSISTENCE_ITERATIONS,
            SweepRecordingAction.class, "launchPersistencePhase",
            null
        );
    }

    /** Spawns {@link Particle#CRIT} at each world position. */
    private static void renderCapturedPoints(World world, List<Vector3f> positions) {
        for (Vector3f p : positions) {
            world.spawnParticle(Particle.CRIT, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
    }

    /** Draws a bright {@link Particle#TRIAL_SPAWNER_DETECTION} ray from 1 to 2 blocks ahead. */
    private static void renderRay(SwordPlayer player) {
        Location eye = player.player().getEyeLocation();
        Vector dir = player.player().getLocation().getDirection();
        World world = player.player().getWorld();
        for (float t = 1.0f; t <= 2.0f; t += 0.25f) {
            world.spawnParticle(Particle.TRIAL_SPAWNER_DETECTION,
                eye.getX() + dir.getX() * t,
                eye.getY() + dir.getY() * t,
                eye.getZ() + dir.getZ() * t,
                1, 0, 0, 0, 0);
        }
    }

    /**
     * Renders a crosshair at the locked origin and a fixed north-pointing arrow (-Z axis).
     * Visible throughout the recording session.
     */
    private static void renderOriginArrow(World world, AttackDevSession session) {
        Location origin = session.getLockedOrigin();
        if (origin == null) return;

        float ox = (float) origin.getX();
        float oy = (float) origin.getY() + 0.9f; // approx eye-level mid-body
        float oz = (float) origin.getZ();

        // Crosshair: small ± X and ± Z ticks at origin
        world.spawnParticle(Particle.DUST, ox + 0.3f, oy, oz, 1, 0, 0, 0, 0, ORIGIN_DUST);
        world.spawnParticle(Particle.DUST, ox - 0.3f, oy, oz, 1, 0, 0, 0, 0, ORIGIN_DUST);
        world.spawnParticle(Particle.DUST, ox, oy, oz + 0.3f, 1, 0, 0, 0, 0, ORIGIN_DUST);
        world.spawnParticle(Particle.DUST, ox, oy, oz - 0.3f, 1, 0, 0, 0, 0, ORIGIN_DUST);

        // North arrow: fixed world-space north direction (+Z in Minecraft)
        for (float t = 0.2f; t <= ARROW_LENGTH; t += 0.2f) {
            world.spawnParticle(Particle.DUST,
                ox, oy, oz - t,
                1, 0, 0, 0, 0, ARROW_DUST);
        }
    }

    /**
     * Returns the world-space tip position: player eye position offset by {@link #TIP_DISTANCE}
     * blocks along the current look direction.
     */
    private static Vector3f computeWorldTip(SwordPlayer player) {
        Location eye = player.player().getEyeLocation();
        Vector dir = player.player().getLocation().getDirection();
        return new Vector3f(
            (float) (eye.getX() + dir.getX() * TIP_DISTANCE),
            (float) (eye.getY() + dir.getY() * TIP_DISTANCE),
            (float) (eye.getZ() + dir.getZ() * TIP_DISTANCE));
    }

    /**
     * Applies a dash-speed potion effect to the combatant for as long as they continue
     * blocking (right-click holding). Stops automatically when blocking ends.
     *
     * @param c the combatant holding right-click
     */
    public static void holdingRight(Combatant c) {
        TimeArbiter.runTimeIndependentBukkitTaskOnTimer(
            null,
            () -> Prefab.PotionEffects.DASH_SPEED.apply(c),
            50, 200,
            SweepRecordingAction.class, "holdingRight",
            new PredicateRunnablePair(
                () -> c instanceof SwordPlayer sp && !sp.player().isBlocking(),
                () -> Debug.attackVolume("Stopped Recording")
            )
        );
    }
}
