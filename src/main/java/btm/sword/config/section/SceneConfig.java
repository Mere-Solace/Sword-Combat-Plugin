package btm.sword.config.section;

import static btm.sword.config.Config.register;

import org.bukkit.configuration.ConfigurationSection;

/**
 * Scene configuration for static menu preview scenes.
 * <p>
 * {@code safe_anchor} defines the off-screen world position where the real player body
 * is hidden during a menu scene. {@code camera_distance} and {@code camera_height}
 * control the offset from the NPC to the fixed camera position.
 * </p>
 */
public final class SceneConfig {

    private SceneConfig() {}

    /** Name of the world containing the safe anchor. */
    public static String SAFE_ANCHOR_WORLD = "world";

    /** X coordinate of the safe anchor (off-screen location for real player body). */
    public static double SAFE_ANCHOR_X = 0.0;

    /** Y coordinate of the safe anchor. Should be far above or below normal play area. */
    public static double SAFE_ANCHOR_Y = 500.0;

    /** Z coordinate of the safe anchor. */
    public static double SAFE_ANCHOR_Z = 0.0;

    /** Distance in blocks from the NPC to the camera position (along the NPC's facing direction). */
    public static double CAMERA_DISTANCE = 3.0;

    /** Height offset in blocks from the NPC's feet to the camera position. */
    public static double CAMERA_HEIGHT = 1.0;

    /**
     * Distance in blocks in front of the player to spawn the fake NPC when using the
     * dev-menu test button. Does not affect the production scene pipeline.
     */
    public static double FAKE_PLAYER_DISTANCE = 10.0;

    static {
        register("scene.safe_anchor_world", SAFE_ANCHOR_WORLD, String.class,
            v -> SAFE_ANCHOR_WORLD = v, ConfigurationSection::getString);
        register("scene.safe_anchor_x", SAFE_ANCHOR_X, Double.class,
            v -> SAFE_ANCHOR_X = v, ConfigurationSection::getDouble);
        register("scene.safe_anchor_y", SAFE_ANCHOR_Y, Double.class,
            v -> SAFE_ANCHOR_Y = v, ConfigurationSection::getDouble);
        register("scene.safe_anchor_z", SAFE_ANCHOR_Z, Double.class,
            v -> SAFE_ANCHOR_Z = v, ConfigurationSection::getDouble);
        register("scene.camera_distance", CAMERA_DISTANCE, Double.class,
            v -> CAMERA_DISTANCE = v, ConfigurationSection::getDouble);
        register("scene.camera_height", CAMERA_HEIGHT, Double.class,
            v -> CAMERA_HEIGHT = v, ConfigurationSection::getDouble);
        register("scene.fake_player_distance", FAKE_PLAYER_DISTANCE, Double.class,
            v -> FAKE_PLAYER_DISTANCE = v, ConfigurationSection::getDouble);
    }
}
