package btm.sword.scene.animation;

import btm.sword.Sword;
import btm.sword.scene.camera.CameraController;
import btm.sword.util.misc.Debug;
import net.donnypz.displayentityutils.events.GroupSpawnedEvent;
import net.donnypz.displayentityutils.managers.DisplayAnimationManager;
import net.donnypz.displayentityutils.managers.DisplayGroupManager;
import net.donnypz.displayentityutils.managers.LoadMethod;
import net.donnypz.displayentityutils.utils.DisplayEntities.DisplayAnimator;
import net.donnypz.displayentityutils.utils.DisplayEntities.DisplayEntityGroup;
import net.donnypz.displayentityutils.utils.DisplayEntities.GroupSpawnSettings;
import net.donnypz.displayentityutils.utils.DisplayEntities.SpawnedDisplayAnimation;
import net.donnypz.displayentityutils.utils.DisplayEntities.SpawnedDisplayEntityGroup;

/**
 * Plays a DEU/BDEngine animation as world entities at the owner's location.
 * <p>
 * Unlike {@link DEUAnimationController}, this controller does not attach a camera,
 * lock the player's movement, or modify the activation context in any way.
 * The animation group is spawned as real server-side entities, visible to all players.
 * </p>
 *
 * <p>The standard Stop button ({@link btm.sword.scene.camera.CameraSystem#stopController}) still works because
 * this controller extends {@link CameraController} — ownership is tracked normally.</p>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * AnimationDef def = AnimationRegistry.get("my_anim").orElseThrow();
 * new WorldAnimationController(def, true).start(player);
 * }</pre>
 */
public class WorldAnimationController extends CameraController {

    private final AnimationDef def;
    private final boolean loop;

    private SpawnedDisplayEntityGroup spawnedGroup;
    private DisplayAnimator animator;

    /**
     * Creates a new world animation controller.
     *
     * @param def  the animation definition to play
     * @param loop if {@code true}, the animation loops; otherwise it plays once
     */
    public WorldAnimationController(AnimationDef def, boolean loop) {
        this.def = def;
        this.loop = loop;
    }

    @Override
    protected void onStart() {
        DisplayEntityGroup group = DisplayGroupManager.getGroup(LoadMethod.LOCAL, def.groupTag());
        if (group == null) {
            Sword.getInstance().getLogger().warning(
                "[WorldAnimationController] Group not found: " + def.groupTag() + " — stopping controller."
            );
            stop();
            return;
        }

        SpawnedDisplayAnimation animation = DisplayAnimationManager.getSpawnedDisplayAnimation(
            def.animTag(), LoadMethod.LOCAL
        );
        if (animation == null) {
            Sword.getInstance().getLogger().warning(
                "[WorldAnimationController] Animation not found: " + def.animTag() + " — stopping controller."
            );
            stop();
            return;
        }
        spawnedGroup = group.spawn(owner.player().getLocation(), GroupSpawnedEvent.SpawnReason.CUSTOM);

        if (spawnedGroup == null) {
            Debug.error("Spawned Group is null");
            return;
        }

        DisplayAnimator.AnimationType animType = loop
            ? DisplayAnimator.AnimationType.LOOP
            : DisplayAnimator.AnimationType.LINEAR;

        animator = DisplayAnimator.play(spawnedGroup, animation, animType);
    }

    @Override
    protected void onStop() {
        if (animator != null) {
            animator.stop(spawnedGroup);
            animator = null;
        }
        if (spawnedGroup != null) {
            spawnedGroup.unregister(true, true);
            spawnedGroup = null;
        }
    }

    @Override
    protected void onTick() {
        // No-op — DEU manages the animation loop internally.
    }
}
