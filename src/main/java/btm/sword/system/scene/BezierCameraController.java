package btm.sword.system.scene;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import btm.sword.control.TimeArbiter;

/**
 * Camera controller that drives the camera along a {@link CameraPath} (cubic Bézier curve).
 * <p>
 * Intended for cutscenes and loading transitions. On start, an invisible
 * {@link ArmorStand} is spawned at the path origin and the player's camera is
 * attached to it via {@link CameraService} ({@code ClientboundSetCameraPacket}).
 * The player remains in their original game mode — no spectator mode is used.
 * </p>
 * <p>
 * If {@link CameraService#isAvailable()} returns {@code false} (reflection failed),
 * the controller falls back to spectator-mode targeting.
 * </p>
 *
 * <h2>Lifecycle</h2>
 * <ol>
 *   <li>{@link #onStart()} — spawns ArmorStand, attaches camera via packet (1-tick delay),
 *       starts tick loop.</li>
 *   <li>{@link #onTick()} — advances {@code t} by {@code 1 / durationTicks}; teleports stand.</li>
 *   <li>{@link #onStop()} — cancels tick task, detaches camera, removes stand.</li>
 * </ol>
 */
public class BezierCameraController extends CameraController {

    private final CameraPath path;
    private final int durationTicks;

    private double t = 0.0;
    private Player playerRef;
    private GameMode savedGameMode;
    private Location savedLocation;
    private ArmorStand armorStand;
    private CameraSession cameraSession;
    private TimeArbiter.TaskHandle tickTask;

    /**
     * Constructs a controller that will drive the camera along {@code path} over the given duration.
     *
     * @param path          the Bézier world-space path to follow
     * @param durationTicks number of 50 ms ticks the traversal should take
     */
    public BezierCameraController(CameraPath path, int durationTicks) {
        this.path = path;
        this.durationTicks = durationTicks;
    }

    @Override
    protected void onStart() {
        playerRef = owner.player();

        World world = playerRef.getWorld();
        Location startLoc = path.evaluate(0.0, world);

        armorStand = (ArmorStand) world.spawnEntity(startLoc, EntityType.ARMOR_STAND);
        armorStand.setVisible(false);
        armorStand.setSmall(true);
        armorStand.setMarker(true);
        armorStand.setGravity(false);
        armorStand.setInvulnerable(true);
        armorStand.setSilent(true);

        if (CameraService.isAvailable()) {
            // Delay 1 tick so the entity is tracked client-side before the packet fires.
            ArmorStand stand = armorStand;
            btm.sword.control.SwordScheduler.runConsumerNextTick(
                p -> cameraSession = CameraService.attach(p, stand), playerRef);
        } else {
            savedGameMode = playerRef.getGameMode();
            savedLocation = playerRef.getLocation().clone();
            playerRef.setGameMode(GameMode.SPECTATOR);
            playerRef.setSpectatorTarget(armorStand);
        }

        tickTask = TimeArbiter.runTimeIndependentBukkitTaskOnTimer(
            this::onTick, null,
            0, 50,
            BezierCameraController.class, "onTick"
        );
    }

    @Override
    protected void onTick() {
        if (!isActive()) return;

        t += 1.0 / durationTicks;
        if (t >= 1.0) {
            armorStand.teleport(path.evaluate(1.0, armorStand.getWorld()));
            stop();
            return;
        }
        armorStand.teleport(path.evaluate(t, armorStand.getWorld()));
    }

    @Override
    protected void onStop() {
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
        if (cameraSession != null) {
            cameraSession.detach();
            cameraSession = null;
        } else if (playerRef != null && savedGameMode != null) {
            playerRef.setSpectatorTarget(null);
            playerRef.setGameMode(savedGameMode);
            if (savedLocation != null) playerRef.teleport(savedLocation);
        }
        if (armorStand != null) {
            armorStand.remove();
            armorStand = null;
        }
    }
}
