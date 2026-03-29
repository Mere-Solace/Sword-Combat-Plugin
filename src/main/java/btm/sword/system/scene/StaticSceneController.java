package btm.sword.system.scene;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;

import btm.sword.Sword;
import btm.sword.listeners.packet.MovementListener;
import btm.sword.system.control.SwordScheduler;
import btm.sword.system.control.TimeArbiter;
import btm.sword.system.entity.impl.SwordPlayer;
import btm.sword.system.input.ActivationContext;

/**
 * Camera controller that freezes the player and attaches the camera to a fixed world location.
 * <p>
 * This is the camera and input-lock layer for static preview scenes (menus, character select).
 * It does not manage the visual subject (fake player NPC, DEU clone, etc.) — that is the
 * caller's responsibility. The camera {@link Location} (with baked yaw/pitch) is provided by
 * the caller; facing toward the subject is the caller's responsibility.
 * </p>
 * <p>
 * An {@link ItemDisplay} entity (matching DEU's camera entity type) is spawned at
 * {@code cameraLocation} and the player's camera is attached to it via {@link CameraService}
 * ({@code ClientboundSetCameraPacket}). Because the entity never moves, mouse input cannot rotate
 * the player's viewport — the view is perfectly fixed with zero jitter.
 * The camera attachment and any open inventory are closed together in a single deferred tick so
 * there is no transition window where the player sees their first-person arm overlay.
 * {@link MovementListener#LOCKED_PLAYERS} blocks body movement via packet cancellation.
 * {@link ActivationContext#CUTSCENE} suppresses all combat inputs and enables SHIFT-to-exit via
 * the existing {@code CutsceneInputHandler}.
 * </p>
 * <p>
 * If {@link CameraService} is unavailable (ProtocolLib not loaded), the controller logs a warning
 * and stops immediately. There is no spectator-mode fallback for this use case.
 * </p>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * Location playerLoc = swordPlayer.player().getLocation();
 * // 3 blocks behind the player, 1 block up, facing toward them
 * Location camLoc = playerLoc.clone()
 *     .subtract(playerLoc.getDirection().multiply(3))
 *     .add(0, 1, 0);
 * camLoc.setDirection(playerLoc.toVector().subtract(camLoc.toVector()));
 * new StaticSceneController(camLoc).start(swordPlayer);
 * // Do NOT call closeInventory() before starting — the controller closes it when the camera
 * // attaches so there is no arm-visible gap.
 * }</pre>
 */
public class StaticSceneController extends CameraController {

    private final Location cameraLocation;

    private SwordPlayer cachedOwner;
    private ItemDisplay cameraEntity;
    private CameraSession cameraSession;
    private TimeArbiter.TaskHandle tickTask;

    /**
     * Creates a static scene controller.
     *
     * @param cameraLocation world location with yaw/pitch set; the camera entity spawns here
     */
    public StaticSceneController(Location cameraLocation) {
        this.cameraLocation = cameraLocation.clone();
    }

    @Override
    protected void onStart() {
        cachedOwner = owner;
        Player player = cachedOwner.player();

        if (!CameraService.isAvailable()) {
            Sword.getInstance().getLogger().warning(
                "[StaticSceneController] CameraService unavailable — cannot freeze view.");
            stop();
            return;
        }

        MovementListener.LOCKED_PLAYERS.add(player.getUniqueId());
        cachedOwner.enterSceneOverlay();

        World world = cameraLocation.getWorld();
        cameraEntity = (ItemDisplay) world.spawnEntity(cameraLocation, EntityType.ITEM_DISPLAY);
        cameraEntity.setGravity(false);
        cameraEntity.setInvulnerable(true);
        cameraEntity.setSilent(true);
        cameraEntity.setPersistent(false);

        // Close inventory and attach camera in the same deferred tick so there is no frame where
        // the player sees their first-person arm overlay (the gap between inventory close and
        // camera attach would otherwise expose a 50 ms arm-visible window).
        ItemDisplay entity = cameraEntity;
        SwordScheduler.runConsumerNextTick(p -> {
            p.closeInventory();
            cameraSession = CameraService.attach(p, entity);
        }, player);

        cachedOwner.setActivationContext(ActivationContext.CUTSCENE);

        tickTask = TimeArbiter.runTimeIndependentBukkitTaskOnTimer(
            this::onTick, null, 0, 50,
            StaticSceneController.class, "onTick"
        );
    }

    @Override
    protected void onTick() {
        if (!isActive()) return;
        // Camera is stationary — no per-tick update needed.
        // MovementListener packet cancellation holds the player's body position server-side.
    }

    @Override
    protected void onStop() {
        if (cachedOwner == null) return;

        MovementListener.LOCKED_PLAYERS.remove(cachedOwner.player().getUniqueId());

        if (tickTask != null)      { tickTask.cancel();       tickTask      = null; }
        if (cameraSession != null) { cameraSession.detach();  cameraSession = null; }
        if (cameraEntity != null)  { cameraEntity.remove();   cameraEntity  = null; }

        cachedOwner.exitSceneOverlay();
        cachedOwner.setActivationContext(ActivationContext.NORMAL);
        cachedOwner = null;
    }
}
