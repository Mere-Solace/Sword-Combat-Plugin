package btm.sword.system.scene;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import btm.sword.config.Config;
import btm.sword.system.control.SwordScheduler;
import btm.sword.system.control.TimeArbiter;

/**
 * Camera controller for the main menu scene.
 * <p>
 * The camera is placed at a fixed world anchor point ({@code Config.Scene.CAMERA_ANCHOR})
 * and bobs/drifts gently using a sinusoidal Y-offset and slow yaw drift. An
 * {@link ItemDisplay} entity is used as the camera anchor and repositioned each tick.
 * {@link HudDisplayGroup} entities (title, subtitle, decorative items) are spawned
 * alongside the camera.
 * </p>
 * <p>
 * The player's camera is attached via {@link CameraService} ({@code ClientboundSetCameraPacket})
 * so the player remains in their original game mode. Falls back to spectator-mode
 * targeting if {@link CameraService#isAvailable()} returns {@code false}.
 * </p>
 *
 * <h2>Lifecycle</h2>
 * <ol>
 *   <li>{@link #onStart()} — spawns ItemDisplay and HUD, attaches camera via packet (1-tick delay).</li>
 *   <li>{@link #onTick()} — applies sinusoidal bob and yaw drift; updates HUD positions.</li>
 *   <li>{@link #onStop()} — cancels task, detaches camera, removes HUD and display entity.</li>
 * </ol>
 */
public class GentleDriftCameraController extends CameraController {

    private int tick = 0;
    private Player playerRef;
    private GameMode savedGameMode;
    private Location savedLocation;
    private Location anchor;
    private ItemDisplay itemDisplay;
    private HudDisplayGroup hudGroup;
    private CameraSession cameraSession;
    private TimeArbiter.TaskHandle tickTask;
    private Vector curDir;

    @Override
    protected void onStart() {
        playerRef = owner.player();

        World world = playerRef.getWorld();
        anchor = Config.Scene.CAMERA_ANCHOR.toLocation(world);
        curDir = anchor.getDirection();

        itemDisplay = (ItemDisplay) world.spawnEntity(anchor.clone(), EntityType.ITEM_DISPLAY);
        itemDisplay.setItemStack(ItemStack.of(Material.STONE_BUTTON));
        itemDisplay.setGravity(false);
        itemDisplay.setInvulnerable(true);
        itemDisplay.setSilent(true);

        if (CameraService.isAvailable()) {
            // Delay 1 tick so the entity is tracked client-side before the packet fires.
            ItemDisplay display = itemDisplay;
            SwordScheduler.runConsumerNextTick(
                p -> cameraSession = CameraService.attach(p, display), playerRef);
        } else {
            savedGameMode = playerRef.getGameMode();
            savedLocation = playerRef.getLocation().clone();
            playerRef.setGameMode(GameMode.SPECTATOR);
            playerRef.setSpectatorTarget(itemDisplay);
        }

        hudGroup = new HudDisplayGroup();
        hudGroup.spawn(anchor.clone());

        tickTask = TimeArbiter.runTimeIndependentBukkitTaskOnTimer(
            this::onTick, null,
            0, 50,
            GentleDriftCameraController.class, "onTick"
        );
    }

    @Override
    protected void onTick() {
        if (!isActive()) return;
        tick++;

        curDir.rotateAroundAxis(Config.Direction.UP(), Config.Scene.YAW_DRIFT_RATE);

        double yOffset = Math.sin(tick * Config.Scene.DRIFT_SPEED) * Config.Scene.DRIFT_AMPLITUDE;

        Location drifted = anchor.clone().add(0, yOffset, 0);

        TimeArbiter.teleportDisplay(itemDisplay, drifted, curDir, 5,
            GentleDriftCameraController.class, 88);
        itemDisplay.teleport(drifted);
        hudGroup.tick(drifted, tick);
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
        if (hudGroup != null) {
            hudGroup.remove();
            hudGroup = null;
        }
        if (itemDisplay != null) {
            itemDisplay.remove();
            itemDisplay = null;
        }
    }
}
