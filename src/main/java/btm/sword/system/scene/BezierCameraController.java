package btm.sword.system.scene;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import btm.sword.system.control.TimeArbiter;

/**
 * Camera controller that drives the camera along a {@link CameraPath} (cubic Bézier curve).
 * <p>
 * Intended for cutscenes and loading transitions. On start, the player is placed in
 * SPECTATOR mode targeting an invisible {@link ArmorStand} that travels along the curve
 * from {@code t = 0} to {@code t = 1} over {@code durationTicks} frames.
 * </p>
 *
 * <h2>Lifecycle</h2>
 * <ol>
 *   <li>{@link #onStart()} — saves player state, spawns ArmorStand at path origin, enters spectator.</li>
 *   <li>{@link #onTick()} — advances {@code t} by {@code 1 / durationTicks} each 50 ms frame; teleports stand.</li>
 *   <li>{@link #onStop()} — cancels tick task, removes stand, restores game mode and location.</li>
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
        savedLocation = playerRef.getLocation().clone();
        savedGameMode = playerRef.getGameMode();

        World world = playerRef.getWorld();
        Location startLoc = path.evaluate(0.0, world);

        armorStand = (ArmorStand) world.spawnEntity(startLoc, EntityType.ARMOR_STAND);
        armorStand.setVisible(false);
        armorStand.setSmall(true);
        armorStand.setMarker(true);
        armorStand.setGravity(false);
        armorStand.setInvulnerable(true);
        armorStand.setSilent(true);

        playerRef.setGameMode(GameMode.SPECTATOR);
        playerRef.setSpectatorTarget(armorStand);

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
        if (armorStand != null) {
            armorStand.remove();
            armorStand = null;
        }
        if (playerRef != null) {
            playerRef.setSpectatorTarget(null);
            playerRef.setGameMode(savedGameMode);
            playerRef.teleport(savedLocation);
        }
    }
}
