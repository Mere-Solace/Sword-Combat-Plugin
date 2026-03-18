package btm.sword.system.scene;

import btm.sword.utility.Prefab;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;

import btm.sword.config.Config;
import btm.sword.system.control.TimeArbiter;

import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

/**
 * Camera controller for the main menu scene.
 * <p>
 * The camera is placed at a fixed world anchor point ({@code Config.Scene.CAMERA_ANCHOR})
 * and bobs/drifts gently using a sinusoidal Y-offset and slow yaw drift, rather than
 * traveling along a path. {@link HudDisplayGroup} entities are spawned alongside the
 * camera and repositioned each tick.
 * </p>
 *
 * <h2>Lifecycle</h2>
 * <ol>
 *   <li>{@link #onStart()} — saves state, spawns ArmorStand and HUD, enters spectator.</li>
 *   <li>{@link #onTick()} — applies sinusoidal bob and yaw drift; updates HUD positions.</li>
 *   <li>{@link #onStop()} — cancels task, removes HUD, removes stand, restores player.</li>
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
    private TimeArbiter.TaskHandle tickTask;
    private Vector curDir;

    @Override
    protected void onStart() {
        playerRef = owner.player();
        savedLocation = playerRef.getLocation().clone();
        savedGameMode = playerRef.getGameMode();

        World world = playerRef.getWorld();
        anchor = Config.Scene.CAMERA_ANCHOR.toLocation(world);
        curDir = anchor.getDirection();

        itemDisplay = (ItemDisplay) world.spawnEntity(anchor.clone(), EntityType.ITEM_DISPLAY);
        itemDisplay.setItemStack(ItemStack.of(Material.STONE_BUTTON));
        itemDisplay.setGravity(false);
        itemDisplay.setInvulnerable(true);
        itemDisplay.setSilent(true);

        playerRef.setGameMode(GameMode.SPECTATOR);
        playerRef.setSpectatorTarget(itemDisplay);

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
        if (hudGroup != null) {
            hudGroup.remove();
            hudGroup = null;
        }
        if (itemDisplay != null) {
            itemDisplay.remove();
            itemDisplay = null;
        }
        if (playerRef != null) {
            playerRef.setSpectatorTarget(null);
            playerRef.setGameMode(savedGameMode);
            playerRef.teleport(savedLocation);
        }
    }
}
