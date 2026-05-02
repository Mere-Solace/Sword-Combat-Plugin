package btm.sword.join;

import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import btm.sword.config.Config;
import btm.sword.entity.player.SwordPlayer;
import btm.sword.playerdata.PlayerData;
import btm.sword.playerdata.PlayerDataManager;
import btm.sword.runtime.scheduler.SwordScheduler;
import btm.sword.scene.SceneManager;
import btm.sword.scene.animation.AnimationDef;
import btm.sword.scene.animation.AnimationRegistry;
import btm.sword.scene.animation.DEUAnimationController;
import btm.sword.scene.camera.CameraSystem;

/**
 * Orchestrates the first-time player intro cutscene.
 *
 * <p>Sequence:</p>
 * <p><b>Phase 1</b> — Player teleports to the opening location; the opening animation plays
 * (looping, camera attached) for {@link Config.JoinSequence#PHASE1_DURATION_MS} ms.</p>
 * <p><b>Phase 2</b> — A lightning-effect strikes the opening location; the animation plays
 * again for {@link Config.JoinSequence#PHASE2_DURATION_MS} ms.</p>
 * <p><b>Phase 3</b> — The animation stops; a fake player NPC (real skin and equipment) spawns
 * at the opening location and the static camera locks onto it for
 * {@link Config.JoinSequence#PHASE3_DURATION_MS} ms.</p>
 * <p><b>Complete</b> — The scene exits, the NPC despawns, the player lands at the opening
 * location, and {@link PlayerData#setJoinSequenceCompleted} is called.</p>
 *
 * <p>All durations and coordinates are hot-reloadable via {@code join_sequence} config keys.</p>
 */
public final class InitialJoinCutscene {

    private InitialJoinCutscene() {}

    /**
     * Starts the intro cutscene for the given player.
     *
     * <p>The player is immediately teleported from their staging slot to the opening location.
     * All subsequent steps are scheduled on the main thread.</p>
     *
     * @param sp the player entering the game for the first time
     */
    public static void play(SwordPlayer sp) {
        Player player = sp.player();
        Location openingLoc = openingLocation();
        if (openingLoc == null) {
            complete(sp);
            return;
        }

        player.teleport(openingLoc);

        AnimationDef def = resolveAnimation(sp);

        // Phase 1: play opening animation
        startAnimation(sp, def);

        SwordScheduler.after(Config.JoinSequence.PHASE1_DURATION_MS, TimeUnit.MILLISECONDS, () -> {
            if (!player.isOnline()) return;
            CameraSystem.stopController(sp);

            // Phase 2: lightning strike + second animation pass
            World world = openingLoc.getWorld();
            if (world != null) {
                world.strikeLightningEffect(openingLoc);
            }
            startAnimation(sp, def);

        }).andThen(Config.JoinSequence.PHASE2_DURATION_MS, TimeUnit.MILLISECONDS, () -> {
            if (!player.isOnline()) return;
            CameraSystem.stopController(sp);

            // Phase 3: fake player NPC + static camera
            // Player is at openingLoc — NPC spawns there; on scene exit
            // MenuSceneController.onStop teleports the player back to openingLoc.
            SceneManager.enterStaticMenuScene(sp, openingLoc);

        }).andThen(Config.JoinSequence.PHASE3_DURATION_MS, TimeUnit.MILLISECONDS, () -> {
            if (!player.isOnline()) return;
            CameraSystem.stopController(sp);
            complete(sp);
            player.teleport(openingLoc);
        });
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /**
     * Marks the join sequence as completed for this player.
     *
     * @param sp the player to mark complete
     */
    private static void complete(SwordPlayer sp) {
        PlayerData data = PlayerDataManager.getPlayerData(sp.player().getUniqueId());
        if (data != null) {
            data.setJoinSequenceCompleted(true);
        }
    }

    /**
     * Starts the opening animation on the player with camera attached and looping enabled
     * so it plays continuously until explicitly stopped. No-op if {@code def} is null.
     *
     * @param sp  the player
     * @param def the animation definition, or null to skip
     */
    private static void startAnimation(SwordPlayer sp, AnimationDef def) {
        if (def == null) return;
        new DEUAnimationController(def, true, true).start(sp);
    }

    /**
     * Resolves the opening animation definition from the registry.
     * Sends the player a warning message if the configured key is not registered.
     *
     * @param sp the player (used for warning delivery only)
     * @return the animation def, or null if not found
     */
    private static AnimationDef resolveAnimation(SwordPlayer sp) {
        String key = Config.JoinSequence.OPENING_ANIMATION_KEY;
        AnimationDef def = AnimationRegistry.get(key).orElse(null);
        if (def == null) {
            sp.player().sendMessage("[JoinCutscene] Opening animation not found: " + key);
        }
        return def;
    }

    /**
     * Builds the opening-sequence {@link Location} from config.
     * Falls back to the first loaded world if the configured world name is not found.
     * Returns null if no world is available.
     *
     * @return the opening location, or null
     */
    static Location openingLocation() {
        World world = Bukkit.getWorld(Config.JoinSequence.WORLD);
        if (world == null && !Bukkit.getWorlds().isEmpty()) {
            world = Bukkit.getWorlds().getFirst();
        }
        if (world == null) return null;
        return new Location(
            world,
            Config.JoinSequence.OPENING_X,
            Config.JoinSequence.OPENING_Y,
            Config.JoinSequence.OPENING_Z,
            Config.JoinSequence.OPENING_YAW,
            0.0f
        );
    }
}
