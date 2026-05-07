package btm.sword.scene.animation;

import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;

import btm.sword.Sword;
import btm.sword.entity.player.SwordPlayer;
import btm.sword.event.packet.MovementListener;
import btm.sword.input.trie.ActivationContext;
import btm.sword.runtime.scheduler.SwordScheduler;
import btm.sword.scene.camera.CameraController;
import btm.sword.scene.camera.CameraService;
import btm.sword.scene.camera.CameraSession;
import net.donnypz.displayentityutils.events.GroupSpawnedEvent;
import net.donnypz.displayentityutils.managers.DisplayAnimationManager;
import net.donnypz.displayentityutils.managers.DisplayGroupManager;
import net.donnypz.displayentityutils.managers.LoadMethod;
import net.donnypz.displayentityutils.utils.DisplayEntities.DisplayAnimator;
import net.donnypz.displayentityutils.utils.DisplayEntities.DisplayEntityGroup;
import net.donnypz.displayentityutils.utils.DisplayEntities.PacketDisplayEntityGroup;
import net.donnypz.displayentityutils.utils.DisplayEntities.SpawnedDisplayAnimation;


/**
 * Camera controller that plays a DEU/BDEngine animation as a cutscene for its owner.
 * <p>
 * On start, the animation's display-entity group is spawned as a packet group at the
 * player's current location and the configured animation is played for that player.
 * If {@code attachCamera} is {@code true}, the animation AND DEU's camera-path system
 * ({@link DisplayAnimator#playCamera}) are both started so the player's viewpoint follows
 * the animation's embedded camera track while the display entities animate.
 * The player's {@link ActivationContext} is set to {@link ActivationContext#CUTSCENE}
 * for the duration, blocking all combat inputs.
 * </p>
 *
 * <p>On stop, the animation and packet group are cleaned up and the activation context
 * is restored to {@link ActivationContext#NORMAL}.</p>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * AnimationDef def = AnimationRegistry.get("slash_test_default").orElseThrow();
 * new DEUAnimationController(def, true, true).start(player);
 * }</pre>
 */
public class DEUAnimationController extends CameraController {

    private final AnimationDef def;
    private final boolean attachCamera;
    private final boolean loop;

    private SwordPlayer cachedOwner;
    private PacketDisplayEntityGroup packetGroup;
    private DisplayAnimator animator;

    private ItemDisplay anchorEntity;
    private CameraSession anchorSession;


    /**
     * Creates a new DEU animation controller.
     *
     * @param def          the animation definition to play
     * @param attachCamera if {@code true}, the player's camera follows the animation's camera track
     * @param loop         if {@code true}, the animation loops; otherwise it plays once
     */
    public DEUAnimationController(AnimationDef def, boolean attachCamera, boolean loop) {
        this.def = def;
        this.attachCamera = attachCamera;
        this.loop = loop;
    }

    @Override
    protected void onStart() {
        cachedOwner = owner;
        Player player = cachedOwner.player();
        Location location = player.getLocation().clone();

        MovementListener.LOCKED_PLAYERS.add(player.getUniqueId());


        DisplayEntityGroup group = DisplayGroupManager.getGroup(LoadMethod.LOCAL, def.groupTag());
        if (group == null) {
            Sword.getInstance().getLogger().warning(
                "[DEUAnimationController] Group not found: " + def.groupTag() + " — stopping controller."
            );
            stop();
            return;
        }

        SpawnedDisplayAnimation animation = DisplayAnimationManager.getSpawnedDisplayAnimation(
            def.animTag(), LoadMethod.LOCAL
        );

        if (animation == null) {
            Sword.getInstance().getLogger().warning(
                "[DEUAnimationController] Animation not found: " + def.animTag() + " — stopping controller."
            );
            stop();
            return;
        }
        packetGroup = group.createPacketGroup(location, GroupSpawnedEvent.SpawnReason.DISPLAY_CONTROLLER);
        if (packetGroup == null) {
            Sword.getInstance().getLogger().warning(
                "[DEUAnimationController] Failed to create packet group for: " + def.groupTag()
                    + " — stopping controller."
            );
            stop();
            return;
        }

        packetGroup.showToPlayer(player, GroupSpawnedEvent.SpawnReason.CUSTOM);
        cachedOwner.enterSceneOverlay();

        DisplayAnimator.AnimationType animType = loop
            ? DisplayAnimator.AnimationType.LOOP
            : DisplayAnimator.AnimationType.LINEAR;

        // Always play the animation (display entity keyframes).
        animator = DisplayAnimator.play(player, packetGroup, animation, animType);

        // When there is no DEU camera track, pin the player's view to a fixed ArmorStand at their
        // eye position so mouse input cannot rotate what they see (no tick-teleport jitter).
        if (!attachCamera) {
            Location eyeLoc = player.getEyeLocation().clone();
            anchorEntity = (ItemDisplay) player.getWorld().spawnEntity(eyeLoc, EntityType.ITEM_DISPLAY);
            anchorEntity.setGravity(false);
            anchorEntity.setInvulnerable(true);
            anchorEntity.setSilent(true);
            anchorEntity.setPersistent(false);
            ItemDisplay entity = anchorEntity;
            SwordScheduler.runConsumerNextTick(
                p -> anchorSession = CameraService.attach(p, entity), player);
        }

        // If camera attachment is requested, also start the DEU camera track.
        if (attachCamera) {
            DisplayAnimator.playCamera(player, packetGroup, animation, animType);
        }

        cachedOwner.setActivationContext(ActivationContext.CUTSCENE);
    }

    @Override
    protected void onStop() {
        if (cachedOwner == null) return;
        Player player = cachedOwner.player();

        MovementListener.LOCKED_PLAYERS.remove(player.getUniqueId());

        if (anchorSession != null) {
            anchorSession.detach();
            anchorSession = null;
        }
        if (anchorEntity != null) {
            anchorEntity.remove();
            anchorEntity = null;
        }

        if (player.isOnline()) {
            if (attachCamera) {
                DisplayAnimator.stopCameraView(player);
            }
            if (animator != null) {
                animator.stop(player, packetGroup);
            }
            if (packetGroup != null) {
                packetGroup.hideFromPlayer(player);
            }
        }

        animator = null;
        if (packetGroup != null) {
            packetGroup.unregister();
            packetGroup = null;
        }

        cachedOwner.exitSceneOverlay();
        cachedOwner.setActivationContext(ActivationContext.NORMAL);
        cachedOwner = null;
    }

    @Override
    protected void onTick() {
        // No-op. MovementListener packet cancellation holds body position server-side.
        // Camera: fixed ArmorStand anchor (!attachCamera) or DEU's camera track (attachCamera).
    }
}
