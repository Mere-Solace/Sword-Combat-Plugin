package btm.sword.config.section;

import static btm.sword.config.Config.register;

import org.bukkit.Color;
import org.bukkit.configuration.ConfigurationSection;

import btm.sword.combat.dev.VolumeEditorMode;
import btm.sword.config.Config;

/**
 * Debug and development configuration for logging and visualization.
 * <p>
 * Enables verbose logging and visual debugging tools. All options default to
 * {@code false} for production. Enable selectively for development/troubleshooting.
 * </p>
 *
 * <h2>Debug Tools</h2>
 * <ul>
 *   <li><b>Verbose Logging</b> - Per-system console/chat output (combat, movement, inventory,
 *       system, umbral, hostile, general debug)</li>
 *   <li><b>Visualization</b> - Particle-based hitbox and raytrace rendering</li>
 * </ul>
 *
 * <p><b>Warning:</b> Visualization features generate many particles and may impact performance.</p>
 */
public final class DebugConfig {

    private DebugConfig() {}

    // Logging — general
    public static boolean LOGGING_VERBOSE_DEBUG = false;
    static { register(
        "debug.logging_verbose_debug",
        LOGGING_VERBOSE_DEBUG, Boolean.class,
        v -> LOGGING_VERBOSE_DEBUG = v,
        ConfigurationSection::getBoolean); }

    // Logging — per system
    public static boolean LOGGING_VERBOSE_COMBAT = false;
    static { register(
        "debug.logging_verbose_combat",
        LOGGING_VERBOSE_COMBAT, Boolean.class,
        v -> LOGGING_VERBOSE_COMBAT = v,
        ConfigurationSection::getBoolean); }

    public static boolean LOGGING_VERBOSE_MOVEMENT = false;
    static { register(
        "debug.logging_verbose_movement",
        LOGGING_VERBOSE_MOVEMENT, Boolean.class,
        v -> LOGGING_VERBOSE_MOVEMENT = v,
        ConfigurationSection::getBoolean); }

    public static boolean LOGGING_VERBOSE_INVENTORY = false;
    static { register(
        "debug.logging_verbose_inventory",
        LOGGING_VERBOSE_INVENTORY, Boolean.class,
        v -> LOGGING_VERBOSE_INVENTORY = v,
        ConfigurationSection::getBoolean); }

    public static boolean LOGGING_VERBOSE_SYSTEM = false;
    static { register(
        "debug.logging_verbose_system",
        LOGGING_VERBOSE_SYSTEM, Boolean.class,
        v -> LOGGING_VERBOSE_SYSTEM = v,
        ConfigurationSection::getBoolean); }

    public static boolean LOGGING_VERBOSE_UMBRAL = false;
    static { register(
        "debug.logging_verbose_umbral",
        LOGGING_VERBOSE_UMBRAL, Boolean.class,
        v -> LOGGING_VERBOSE_UMBRAL = v,
        ConfigurationSection::getBoolean); }

    public static boolean LOGGING_VERBOSE_UMBRAL_STATES = false;
    static { register(
        "debug.logging_verbose_umbral_states",
        LOGGING_VERBOSE_UMBRAL_STATES, Boolean.class,
        v -> LOGGING_VERBOSE_UMBRAL_STATES = v,
        ConfigurationSection::getBoolean); }

    public static boolean LOGGING_VERBOSE_HOSTILE = false;
    static { register(
        "debug.logging_verbose_hostile",
        LOGGING_VERBOSE_HOSTILE, Boolean.class,
        v -> LOGGING_VERBOSE_HOSTILE = v,
        ConfigurationSection::getBoolean); }

    public static boolean LOGGING_VERBOSE_LISTENER = false;
    static { register(
        "debug.logging_verbose_listener",
        LOGGING_VERBOSE_LISTENER, Boolean.class,
        v -> LOGGING_VERBOSE_LISTENER = v,
        ConfigurationSection::getBoolean); }

    /** Log detailed animation state changes and transitions. */
    public static boolean LOGGING_VERBOSE_ANIMATION = false;
    static { register(
        "debug.logging_verbose_animation",
        LOGGING_VERBOSE_ANIMATION, Boolean.class,
        v -> LOGGING_VERBOSE_ANIMATION = v,
        ConfigurationSection::getBoolean); }

    /** Log input combo detection, trie traversal, and action dispatch. */
    public static boolean LOGGING_VERBOSE_INPUT = false;
    static { register(
        "debug.logging_verbose_input",
        LOGGING_VERBOSE_INPUT, Boolean.class,
        v -> LOGGING_VERBOSE_INPUT = v,
        ConfigurationSection::getBoolean); }

    /** Log skill slot resolution, ability cast routing, and
     * {@link btm.sword.action.skill.container.PlayerSkillContainer} state. */
    public static boolean LOGGING_VERBOSE_SKILL = false;
    static { register(
        "debug.logging_verbose_skill",
        LOGGING_VERBOSE_SKILL, Boolean.class,
        v -> LOGGING_VERBOSE_SKILL = v,
        ConfigurationSection::getBoolean); }

    /** Log ability execution lifecycle: activation, soulfire cost, cooldown, and charge sessions. */
    public static boolean LOGGING_VERBOSE_ABILITY = false;
    static { register(
        "debug.logging_verbose_ability",
        LOGGING_VERBOSE_ABILITY, Boolean.class,
        v -> LOGGING_VERBOSE_ABILITY = v,
        ConfigurationSection::getBoolean); }

    /** Log grab action lifecycle: cast, hold ticks, release, and target resolution. */
    public static boolean LOGGING_VERBOSE_GRAB = false;
    static { register(
        "debug.logging_verbose_grab",
        LOGGING_VERBOSE_GRAB, Boolean.class,
        v -> LOGGING_VERBOSE_GRAB = v,
        ConfigurationSection::getBoolean); }

    /** Log attack sweeps: Bezier curve steps, hitbox detections,
     * and {@link btm.sword.combat.hit.HitValuePacket} dispatch. */
    public static boolean LOGGING_VERBOSE_ATTACK = false;
    static { register(
        "debug.logging_verbose_attack",
        LOGGING_VERBOSE_ATTACK, Boolean.class,
        v -> LOGGING_VERBOSE_ATTACK = v,
        ConfigurationSection::getBoolean); }

    /** Log thrown-item lifecycle: spawn, flight ticks, collision, recall, and arbiter cleanup. */
    public static boolean LOGGING_VERBOSE_THROWING = false;
    static { register(
        "debug.logging_verbose_throwing",
        LOGGING_VERBOSE_THROWING, Boolean.class,
        v -> LOGGING_VERBOSE_THROWING = v,
        ConfigurationSection::getBoolean); }

    /** Log AttackDef volume simulation: launch, per-hit confirmation, and attack expiry. */
    public static boolean LOGGING_VERBOSE_ATTACK_VOLUME = false;
    static { register(
        "debug.logging_verbose_attack_volume",
        LOGGING_VERBOSE_ATTACK_VOLUME, Boolean.class,
        v -> LOGGING_VERBOSE_ATTACK_VOLUME = v,
        ConfigurationSection::getBoolean); }

    /** Log ParticleEffect → ParticleWrapper conversion: particle type, dustOptions type, and dispatch path. */
    public static boolean LOGGING_VERBOSE_PARTICLE_DISPLAY = false;
    static { register(
        "debug.logging_verbose_particle_display",
        LOGGING_VERBOSE_PARTICLE_DISPLAY, Boolean.class,
        v -> LOGGING_VERBOSE_PARTICLE_DISPLAY = v,
        ConfigurationSection::getBoolean); }

    // Visualization configuration
    public static boolean VISUALIZATION_SHOW_HITBOXES = false;
    static { register(
        "debug.visualization_show_hitboxes",
        VISUALIZATION_SHOW_HITBOXES, Boolean.class,
        v -> VISUALIZATION_SHOW_HITBOXES = v,
        ConfigurationSection::getBoolean); }

    public static boolean VISUALIZATION_SHOW_RAYTRACES = false;
    static { register(
        "debug.visualization_show_raytraces",
        VISUALIZATION_SHOW_RAYTRACES, Boolean.class,
        v -> VISUALIZATION_SHOW_RAYTRACES = v,
        ConfigurationSection::getBoolean); }

    // Wireframe particle colors and sizes for the attack volume editor/playback visualization
    /** Particle color for the currently selected keyframe wireframe (orange by default). */
    public static Color WIREFRAME_SELECTED_COLOR = Color.fromRGB(255, 170, 0);
    static { register(
        "debug.wireframe_selected_color",
        WIREFRAME_SELECTED_COLOR, Color.class,
        v -> { WIREFRAME_SELECTED_COLOR = v;
            VolumeEditorMode.rebuildDust(); },
        Config::loadColor); }

    /** Dust particle size for the selected keyframe wireframe. */
    public static double WIREFRAME_SELECTED_SIZE = 0.7;
    static { register(
        "debug.wireframe_selected_size",
        WIREFRAME_SELECTED_SIZE, Double.class,
        v -> { WIREFRAME_SELECTED_SIZE = v;
            VolumeEditorMode.rebuildDust(); },
        ConfigurationSection::getDouble); }

    /** Particle color for unselected keyframe wireframes (yellow by default). */
    public static Color WIREFRAME_DEFAULT_COLOR = Color.fromRGB(255, 230, 60);
    static { register(
        "debug.wireframe_default_color",
        WIREFRAME_DEFAULT_COLOR, Color.class,
        v -> { WIREFRAME_DEFAULT_COLOR = v;
            VolumeEditorMode.rebuildDust(); },
        Config::loadColor); }

    /** Dust particle size for unselected keyframe wireframes. */
    public static double WIREFRAME_DEFAULT_SIZE = 0.7;
    static { register(
        "debug.wireframe_default_size",
        WIREFRAME_DEFAULT_SIZE, Double.class,
        v -> { WIREFRAME_DEFAULT_SIZE = v;
            VolumeEditorMode.rebuildDust(); },
        ConfigurationSection::getDouble); }

    /** Particle color for the live simulation playback wireframe (cyan by default). */
    public static Color WIREFRAME_LIVE_COLOR = Color.fromRGB(100, 220, 255);
    static { register(
        "debug.wireframe_live_color",
        WIREFRAME_LIVE_COLOR, Color.class,
        v -> { WIREFRAME_LIVE_COLOR = v;
            VolumeEditorMode.rebuildDust(); },
        Config::loadColor); }

    /** Dust particle size for the live simulation playback wireframe. */
    public static double WIREFRAME_LIVE_SIZE = 1.0;
    static { register(
        "debug.wireframe_live_size",
        WIREFRAME_LIVE_SIZE, Double.class,
        v -> { WIREFRAME_LIVE_SIZE = v;
            VolumeEditorMode.rebuildDust(); },
        ConfigurationSection::getDouble); }

    /**
     * When {@code true}, {@link btm.sword.playerdata.PlayerDataManager#register} skips
     * the database load and always creates a fresh {@link btm.sword.playerdata.PlayerData},
     * simulating a first-time join. Persisted to config.yaml.
     * Toggle via the Dev Toggles menu or {@code /sword dev skipload}.
     */
    public static boolean SKIP_DATA_LOAD = false;
    static { register(
        "debug.skip_data_load",
        SKIP_DATA_LOAD, Boolean.class,
        v -> SKIP_DATA_LOAD = v,
        ConfigurationSection::getBoolean); }

    /**
     * When {@code true}, all save paths in {@link btm.sword.playerdata.PlayerDataManager}
     * ({@code saveAsync}, {@code flushAll}, {@code shutdown}) are no-ops — data is never written
     * to the database. The store connection is still closed cleanly on shutdown.
     * Persisted to config.yaml. Toggle via the Dev Toggles menu or {@code /sword dev skipsave}.
     */
    public static boolean SKIP_DATA_SAVE = false;
    static { register(
        "debug.skip_data_save",
        SKIP_DATA_SAVE, Boolean.class,
        v -> SKIP_DATA_SAVE = v,
        ConfigurationSection::getBoolean); }
}
