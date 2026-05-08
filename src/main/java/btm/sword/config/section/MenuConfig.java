package btm.sword.config.section;

import static btm.sword.config.Config.register;

import org.bukkit.Material;

import btm.sword.config.Config;

/**
 * Material icons used for section header buttons throughout the inventory menu system.
 * Each entry corresponds to one collapsible section in the dev/character menus.
 */
public final class MenuConfig {

    private MenuConfig() {}

    /** Material icon for the Umbral section button. */
    public static Material UMBRAL_ICON = Material.HEAVY_CORE;
    static { register("menu.section_icons.umbral", UMBRAL_ICON, Material.class,
        v -> UMBRAL_ICON = v, Config::loadMaterial); }

    /** Material icon for the Umbral Blade section button. */
    public static Material UMBRAL_BLADE_ICON = Material.HEAVY_CORE;
    static { register("menu.section_icons.umbralblade", UMBRAL_BLADE_ICON, Material.class,
        v -> UMBRAL_BLADE_ICON = v, Config::loadMaterial); }

    /** Material icon for the Hostile section button. */
    public static Material HOSTILE_ICON = Material.REDSTONE;
    static { register("menu.section_icons.hostile", HOSTILE_ICON, Material.class,
        v -> HOSTILE_ICON = v, Config::loadMaterial); }

    /** Material icon for the Grab section button. */
    public static Material GRAB_ICON = Material.RABBIT_FOOT;
    static { register("menu.section_icons.grab", GRAB_ICON, Material.class,
        v -> GRAB_ICON = v, Config::loadMaterial); }

    /** Material icon for the Debug section button. */
    public static Material DEBUG_ICON = Material.ENDER_EYE;
    static { register("menu.section_icons.debug", DEBUG_ICON, Material.class,
        v -> DEBUG_ICON = v, Config::loadMaterial); }

    /** Material icon for the World section button. */
    public static Material WORLD_ICON = Material.WATER_BUCKET;
    static { register("menu.section_icons.world", WORLD_ICON, Material.class,
        v -> WORLD_ICON = v, Config::loadMaterial); }

    /** Material icon for the Movement section button. */
    public static Material MOVEMENT_ICON = Material.FEATHER;
    static { register("menu.section_icons.movement", MOVEMENT_ICON, Material.class,
        v -> MOVEMENT_ICON = v, Config::loadMaterial); }

    /** Material icon for the Entity section button. */
    public static Material ENTITY_ICON = Material.YELLOW_DYE;
    static { register("menu.section_icons.entity", ENTITY_ICON, Material.class,
        v -> ENTITY_ICON = v, Config::loadMaterial); }

    /** Material icon for the Audio section button. */
    public static Material AUDIO_ICON = Material.NAUTILUS_SHELL;
    static { register("menu.section_icons.audio", AUDIO_ICON, Material.class,
        v -> AUDIO_ICON = v, Config::loadMaterial); }

    /** Material icon for the Detection section button. */
    public static Material DETECTION_ICON = Material.SPYGLASS;
    static { register("menu.section_icons.detection", DETECTION_ICON, Material.class,
        v -> DETECTION_ICON = v, Config::loadMaterial); }

    /** Material icon for the Display section button. */
    public static Material DISPLAY_ICON = Material.CYAN_DYE;
    static { register("menu.section_icons.display", DISPLAY_ICON, Material.class,
        v -> DISPLAY_ICON = v, Config::loadMaterial); }

    /** Material icon for the Timing section button. */
    public static Material TIMING_ICON = Material.CLOCK;
    static { register("menu.section_icons.timing", TIMING_ICON, Material.class,
        v -> TIMING_ICON = v, Config::loadMaterial); }

    /** Material icon for the Combat section button. */
    public static Material COMBAT_ICON = Material.STONE_SWORD;
    static { register("menu.section_icons.combat", COMBAT_ICON, Material.class,
        v -> COMBAT_ICON = v, Config::loadMaterial); }

    /** Material icon for the Physics section button. */
    public static Material PHYSICS_ICON = Material.COMPARATOR;
    static { register("menu.section_icons.physics", PHYSICS_ICON, Material.class,
        v -> PHYSICS_ICON = v, Config::loadMaterial); }

    /** Material icon for the Angle section button. */
    public static Material ANGLE_ICON = Material.REPEATER;
    static { register("menu.section_icons.angle", ANGLE_ICON, Material.class,
        v -> ANGLE_ICON = v, Config::loadMaterial); }

    /** Material icon for the Angles section button. */
    public static Material ANGLES_ICON = Material.COMPASS;
    static { register("menu.section_icons.angles", ANGLES_ICON, Material.class,
        v -> ANGLES_ICON = v, Config::loadMaterial); }

    /** Material icon for the Color section button. */
    public static Material COLOR_ICON = Material.ORANGE_DYE;
    static { register("menu.section_icons.color", COLOR_ICON, Material.class,
        v -> COLOR_ICON = v, Config::loadMaterial); }

    /** Material icon for the Attack Curves section button. */
    public static Material ATTACK_CURVES_ICON = Material.BLAZE_ROD;
    static { register("menu.section_icons.attack_curves", ATTACK_CURVES_ICON, Material.class,
        v -> ATTACK_CURVES_ICON = v, Config::loadMaterial); }

    /** Material icon for the Materials section button. */
    public static Material MATERIALS_ICON = Material.CHEST;
    static { register("menu.section_icons.materials", MATERIALS_ICON, Material.class,
        v -> MATERIALS_ICON = v, Config::loadMaterial); }

    /** Material icon for the Particles section button. */
    public static Material PARTICLES_ICON = Material.FIREWORK_STAR;
    static { register("menu.section_icons.particles", PARTICLES_ICON, Material.class,
        v -> PARTICLES_ICON = v, Config::loadMaterial); }

    public static Material SECTION_ICON = Material.RECOVERY_COMPASS;
    static { register("animation.section_icon", SECTION_ICON, Material.class,
        v -> SECTION_ICON = v, Config::loadMaterial); }
}
