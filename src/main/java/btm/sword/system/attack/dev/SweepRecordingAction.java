package btm.sword.system.attack.dev;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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
import btm.sword.system.control.PredicateRunnablePair;
import btm.sword.system.control.TimeArbiter;
import btm.sword.system.entity.impl.Combatant;
import btm.sword.system.entity.impl.SwordPlayer;
import btm.sword.system.inventory.menu.dev.AttackEditorMenu;
import btm.sword.utility.Debug;
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
 * player's look direction as a {@link RecordedSample} — a local-space tip position plus
 * an absolute timestamp. Samples accumulate in {@link AttackDevSession#getRecordingBuffer()};
 * matching world-space coordinates for particle visualization are stored in
 * {@link AttackDevSession#getWorldPositions()}.</p>
 *
 * <p>The sampling loop auto-cancels when the session leaves {@link DevMode#RECORDING}
 * (via a {@link PredicateRunnablePair} stop condition) and immediately triggers the
 * 5-second particle persistence phase. A live ray preview
 * ({@link Particle#TRIAL_SPAWNER_DETECTION}, 1–2 blocks ahead) is rendered each tick
 * regardless of blocking state.</p>
 */
public final class SweepRecordingAction {

    /** Distance from the player's eye at which the tip sample is placed, in blocks. */
    private static final float TIP_DISTANCE = 1.5f;

    /** Sampling and re-render interval in milliseconds (10 Hz). */
    private static final int SAMPLE_PERIOD_MS = 100;

    /** Number of persistence-phase iterations (each 100 ms → 5 seconds total). */
    private static final int PERSISTENCE_ITERATIONS = 50;

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
                saveDraft(session.getCurrentAttackName(), samples, player);
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
                        new RecordedSample(computeLocalTip(player), System.currentTimeMillis()));

                    Location eye = player.player().getEyeLocation();
                    Vector dir = player.player().getLocation().getDirection();
                    session.getWorldPositions().add(new Vector3f(
                        (float) (eye.getX() + dir.getX() * TIP_DISTANCE),
                        (float) (eye.getY() + dir.getY() * TIP_DISTANCE),
                        (float) (eye.getZ() + dir.getZ() * TIP_DISTANCE)));
                }

                // Live look-ray preview (1 → 2 blocks) and all captured positions
                renderRay(player);
                renderCapturedPoints(player.player().getWorld(), session.getWorldPositions());
            },
            null,
            0, SAMPLE_PERIOD_MS,
            SweepRecordingAction.class, "launchSamplingLoop",
            // Stop when recording ends or the player goes offline; trigger persistence on stop
            new PredicateRunnablePair(
                () -> session.getMode() != DevMode.RECORDING || !player.player().isOnline(),
                () -> {
                    if (player.player().isOnline()) {
                        launchPersistencePhase(player, List.copyOf(session.getWorldPositions()));
                    }
                })
        );
    }

    /**
     * Converts the recorded samples into a VOLUME {@link AttackDef} and writes it to
     * {@code plugins/sword/attacks/<name>.yml} via {@link AttackDefSerializer}.
     *
     * <p>Timestamps are normalized so the first sample becomes {@code t=0.0} and the last
     * becomes {@code t=1.0}. Each keyframe uses a default half-extents of 0.4 × 0.4 × 0.4
     * and an identity rotation — these are intended to be hand-tuned after recording.</p>
     */
    private static void saveDraft(String name, List<RecordedSample> samples, SwordPlayer player) {
        long firstMs = samples.getFirst().capturedAtMs();
        long lastMs = samples.getLast().capturedAtMs();
        long spanMs = lastMs - firstMs;

        List<VolumeKeyframe> keyframes = new ArrayList<>(samples.size());
        Vector3f defaultHalfExtents = new Vector3f(0.4f, 0.4f, 0.4f);
        for (RecordedSample s : samples) {
            float t = spanMs == 0 ? 0f : (float) (s.capturedAtMs() - firstMs) / spanMs;
            keyframes.add(new VolumeKeyframe(t, new Vector3f(s.localPosition()), defaultHalfExtents, new Quaternionf()));
        }

        // Placeholder hit values — intended to be edited in the saved YAML
        HitValuePacket placeholder = new HitValuePacket(() -> 0f, () -> 10, () -> 0, () -> 0f, () -> 0f);

        AttackDef draft = new AttackDef.Builder(name)
            .duration((int) spanMs)
            .onHit(placeholder)
            .keyframes(keyframes)
            .build();

        File attacksDir = new File(Sword.getInstance().getDataFolder(), "attacks");
        attacksDir.mkdirs();
        File file = new File(attacksDir, name + ".yml");
        AttackDefSerializer.save(file, draft);
        AttackRegistry.register(draft);

        Debug.attackVolume("SAVED draft '" + name + "' → " + file.getPath()
            + " keyframes=" + keyframes.size() + " span=" + spanMs + "ms");
        player.player().sendMessage(Component.text(
            "[Dev] Saved to attacks/" + name + ".yml — opening editor.", NamedTextColor.AQUA));

        // Start editing the newly recorded attack immediately
        AttackDevSession session = AttackDevSession.getOrCreate(player.player());
        session.startEditing(name, keyframes, (int) spanMs, placeholder);
        new AttackEditorMenu(player).open();
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

    /** Spawns {@link Particle#CRIT} at each previously captured world position. */
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
     * Converts the player's current look direction into a local-space tip position.
     *
     * <p>Yaw is stripped so the result is expressed in the player's facing frame
     * (+Z forward, +X right, +Y up), placed {@link #TIP_DISTANCE} blocks from the origin.</p>
     *
     * <p>VolumeSimulation applies {@code rotateY(-yawRad)} for local→world, so
     * {@code rotateY(+yawRad)} is the inverse that recovers local space.</p>
     */
    private static Vector3f computeLocalTip(SwordPlayer player) {
        float yawRad = (float) Math.toRadians(player.player().getLocation().getYaw());
        float pitchRad = (float) Math.toRadians(player.player().getLocation().getPitch());

        // World-space look direction (matches Bukkit's getDirection() formula)
        float dx = -(float) (Math.cos(pitchRad) * Math.sin(yawRad));
        float dy = -(float) Math.sin(pitchRad);
        float dz = (float) (Math.cos(pitchRad) * Math.cos(yawRad));

        // Remove player yaw to arrive at local space (+Z = forward along player facing)
        Vector3f localDir = new Vector3f(dx, dy, dz)
            .rotate(new Quaternionf().rotateY(yawRad), new Vector3f());
        return localDir.mul(TIP_DISTANCE);
    }
}
