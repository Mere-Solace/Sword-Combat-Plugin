package btm.sword.config.section;

import static btm.sword.config.Config.register;

import org.bukkit.configuration.ConfigurationSection;

/**
 * World interaction configuration for block breaking and explosions.
 * <p>
 * Controls whether combat actions can modify the environment, including block
 * breaking permissions and explosion behavior (fire/block damage).
 * </p>
 *
 * <h2>Protection Integration</h2>
 * <ul>
 *   <li><b>WorldGuard</b> - Respects region protection flags when enabled</li>
 *   <li><b>Block Breaking</b> - Master toggle for all block modifications</li>
 *   <li><b>Explosions</b> - Separate controls for fire and block damage</li>
 * </ul>
 */
public final class WorldConfig {

    private WorldConfig() {}

    // Block interaction configuration
    public static boolean BLOCK_INTERACTION_ALLOW_BLOCK_BREAKING = false;
    static { register(
        "world.block_interaction_allow_block_breaking",
        BLOCK_INTERACTION_ALLOW_BLOCK_BREAKING, Boolean.class,
        v -> BLOCK_INTERACTION_ALLOW_BLOCK_BREAKING = v,
        ConfigurationSection::getBoolean); }

    public static boolean BLOCK_INTERACTION_ALLOW_BLOCK_PLACING = false;
    static { register(
        "world.block_interaction_allow_block_placing",
        BLOCK_INTERACTION_ALLOW_BLOCK_PLACING, Boolean.class,
        v -> BLOCK_INTERACTION_ALLOW_BLOCK_PLACING = v,
        ConfigurationSection::getBoolean); }

    public static boolean BLOCK_INTERACTION_RESPECT_WORLD_GUARD = true;
    static { register(
        "world.block_interaction_respect_world_guard",
        BLOCK_INTERACTION_RESPECT_WORLD_GUARD, Boolean.class,
        v -> BLOCK_INTERACTION_RESPECT_WORLD_GUARD = v,
        ConfigurationSection::getBoolean); }

    // Explosions configuration
    public static boolean EXPLOSIONS_SET_FIRE = false;
    static { register(
        "world.explosions_set_fire",
        EXPLOSIONS_SET_FIRE, Boolean.class,
        v -> EXPLOSIONS_SET_FIRE = v,
        ConfigurationSection::getBoolean); }

    public static boolean EXPLOSIONS_BREAK_BLOCKS = false;
    static { register(
        "world.explosions_break_blocks",
        EXPLOSIONS_BREAK_BLOCKS, Boolean.class,
        v -> EXPLOSIONS_BREAK_BLOCKS = v,
        ConfigurationSection::getBoolean); }
}
