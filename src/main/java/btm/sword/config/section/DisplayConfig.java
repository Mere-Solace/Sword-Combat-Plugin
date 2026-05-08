package btm.sword.config.section;

import static btm.sword.config.Config.loadEnum;
import static btm.sword.config.Config.register;

import org.bukkit.configuration.ConfigurationSection;

/**
 * Visual display configuration for particles, status indicators, and effects.
 * <p>
 * Controls particle effects, status display positioning, item display behavior,
 * and billboard modes. Distances in <b>blocks</b>, intervals in <b>ticks</b>,
 * brightness 0-15 (Minecraft light level).
 * </p>
 *
 * <h2>Key Subsystems</h2>
 * <ul>
 *   <li><b>Status Display</b> - Overhead health/stats text displays</li>
 *   <li><b>Item Display</b> - Floating item entities, billboard modes</li>
 *   <li><b>Particles</b> - Global particle toggles and density</li>
 * </ul>
 *
 * @see org.bukkit.entity.Display.Billboard Billboard rotation modes
 */
public final class DisplayConfig {

    private DisplayConfig() {}

    public static int DEFAULT_TELEPORT_DURATION = 2;
    static { register(
        "display.default_teleport_duration",
        DEFAULT_TELEPORT_DURATION, Integer.class,
        v -> DEFAULT_TELEPORT_DURATION = v,
        ConfigurationSection::getInt
    ); }

    // Status display configuration
    public static boolean STATUS_DISPLAY_ENABLED = true;
    static { register(
        "display.status_display_enabled",
        STATUS_DISPLAY_ENABLED, Boolean.class,
        v -> STATUS_DISPLAY_ENABLED = v,
        ConfigurationSection::getBoolean
    ); }

    public static double STATUS_DISPLAY_HEIGHT_OFFSET = 2.0;
    static { register(
        "display.status_display_height_offset",
        STATUS_DISPLAY_HEIGHT_OFFSET, Double.class,
        v -> STATUS_DISPLAY_HEIGHT_OFFSET = v,
        ConfigurationSection::getDouble
    ); }

    public static int STATUS_DISPLAY_UPDATE_INTERVAL = 5;
    static { register(
        "display.status_display_update_interval",
        STATUS_DISPLAY_UPDATE_INTERVAL, Integer.class,
        v -> STATUS_DISPLAY_UPDATE_INTERVAL = v,
        ConfigurationSection::getInt
    ); }

    public static int STATUS_DISPLAY_BLOCK_BRIGHTNESS = 15; // 0-15 (light level)
    static { register(
        "display.status_display_block_brightness",
        STATUS_DISPLAY_BLOCK_BRIGHTNESS, Integer.class,
        v -> STATUS_DISPLAY_BLOCK_BRIGHTNESS = v,
        ConfigurationSection::getInt
    ); }

    public static int STATUS_DISPLAY_SKY_BRIGHTNESS = 15; // 0-15 (light level)
    static { register(
        "display.status_display_sky_brightness",
        STATUS_DISPLAY_SKY_BRIGHTNESS, Integer.class,
        v -> STATUS_DISPLAY_SKY_BRIGHTNESS = v,
        ConfigurationSection::getInt
    ); }

    // Item display follow configuration
    public static int ITEM_DISPLAY_FOLLOW_UPDATE_INTERVAL = 100;
    static { register(
        "display.item_display_follow_update_interval",
        ITEM_DISPLAY_FOLLOW_UPDATE_INTERVAL, Integer.class,
        v -> ITEM_DISPLAY_FOLLOW_UPDATE_INTERVAL = v,
        ConfigurationSection::getInt
    ); }

    public static int ITEM_DISPLAY_FOLLOW_PARTICLE_INTERVAL = 4;
    static { register(
        "display.item_display_follow_particle_interval",
        ITEM_DISPLAY_FOLLOW_PARTICLE_INTERVAL, Integer.class,
        v -> ITEM_DISPLAY_FOLLOW_PARTICLE_INTERVAL = v,
        ConfigurationSection::getInt
    ); }

    public static org.bukkit.entity.Display.Billboard ITEM_DISPLAY_FOLLOW_BILLBOARD_MODE = org.bukkit.entity.Display.Billboard.FIXED;
    static { register(
        "display.item_display_follow_billboard_mode",
        ITEM_DISPLAY_FOLLOW_BILLBOARD_MODE, org.bukkit.entity.Display.Billboard.class,
        v -> ITEM_DISPLAY_FOLLOW_BILLBOARD_MODE = v,
        (s, p, d) -> loadEnum(s, p, d, org.bukkit.entity.Display.Billboard.class)
    ); }

    /** Number of display steps per attack animation. */
    public static int ATTACK_DISPLAY_STEPS = 10;
    static { register(
        "display.attack_display_steps",
        ATTACK_DISPLAY_STEPS, Integer.class,
        v -> ATTACK_DISPLAY_STEPS = v,
        ConfigurationSection::getInt
    ); }

    // Particles configuration
    public static boolean PARTICLES_ENABLED = true;
    static { register(
        "display.particles_enabled",
        PARTICLES_ENABLED, Boolean.class,
        v -> PARTICLES_ENABLED = v,
        ConfigurationSection::getBoolean
    ); }

    public static int PARTICLES_DENSITY = 10;
    static { register(
        "display.particles_density",
        PARTICLES_DENSITY, Integer.class,
        v -> PARTICLES_DENSITY = v,
        ConfigurationSection::getInt
    ); }
}
