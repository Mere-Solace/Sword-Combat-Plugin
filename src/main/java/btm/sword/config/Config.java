package btm.sword.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.util.Vector;

import btm.sword.combat.style.AttackType;
import btm.sword.config.section.AngleConfig;
import btm.sword.config.section.AnimationConfig;
import btm.sword.config.section.AttackCurveConfig;
import btm.sword.config.section.AudioConfig;
import btm.sword.config.section.ColorConfig;
import btm.sword.config.section.CombatConfig;
import btm.sword.config.section.CtfConfig;
import btm.sword.config.section.DebugConfig;
import btm.sword.config.section.DetectionConfig;
import btm.sword.config.section.DirectionConfig;
import btm.sword.config.section.DisplayConfig;
import btm.sword.config.section.EntityConfig;
import btm.sword.config.section.GrabConfig;
import btm.sword.config.section.HostileConfig;
import btm.sword.config.section.JoinSequenceConfig;
import btm.sword.config.section.MaterialConfig;
import btm.sword.config.section.MenuConfig;
import btm.sword.config.section.MenuGridConfig;
import btm.sword.config.section.MovementConfig;
import btm.sword.config.section.ParticleConfig;
import btm.sword.config.section.PhysicsConfig;
import btm.sword.config.section.RoguelikeConfig;
import btm.sword.config.section.SceneConfig;
import btm.sword.config.section.TimingConfig;
import btm.sword.config.section.UmbralBladeConfig;
import btm.sword.config.section.WorldConfig;
import btm.sword.util.sound.SwordSoundType;
import net.kyori.adventure.text.format.TextColor;

/**
 * Static configuration class for Sword: Combat Evolved.
 * <p>
 * Provides centralized, type-safe access to all configuration values.
 * Values are loaded from config.yaml by {@link ConfigManager} and can be
 * hot-reloaded at runtime using /sword reload.
 * </p>
 * <p>
 * Uses a self-registering ConfigEntry pattern where each field registers itself
 * in a static initializer block. ConfigManager loops through the ENTRIES list
 * for reload/save operations.
 * </p>
 */
public final class Config {

    private Config() {}

    // ==============================================================================
    // CONFIG ENTRY REGISTRATION SYSTEM
    // ==============================================================================

    /**
         * ConfigEntry represents a single configuration value with metadata for loading and saving.
         * <p>
         * Each entry contains:
         * <ul>
         *   <li><b>path</b> - YAML path (e.g., "angles.umbral_blade_idle_period")</li>
         *   <li><b>defaultValue</b> - Default value if not in config.yaml</li>
         *   <li><b>type</b> - Java class type for type safety</li>
         *   <li><b>assign</b> - Consumer lambda to update the static field</li>
         *   <li><b>loader</b> - Custom loader for type-specific YAML parsing</li>
         * </ul>
         * </p>
         *
         * @param <T> the Java type of the configuration value
         */
        public record ConfigEntry<T>(String path, T defaultValue, Class<T> type, Consumer<T> assign, Loader<T> loader) {
            /** Functional interface for custom YAML loading logic. */
            @FunctionalInterface
            public interface Loader<T> {
                /** Reads a value of type {@code T} from the given config section. */
                T load(ConfigurationSection section, String path, T defaultValue);
            }

        /**
         * Constructs a {@code ConfigEntry} and immediately self-registers it in {@link Config#ENTRIES}.
         *
         * @param path         YAML key (dot-separated, e.g. {@code "debug.skip_data_load"})
         * @param defaultValue value used when the key is absent from config.yaml
         * @param type         boxed type of the value
         * @param assign       consumer that writes the loaded value into the owning static field
         * @param loader       reads the raw value from a {@link ConfigurationSection}
         */
        public ConfigEntry {
        }
        }

    /**
     * Backing storage for every registered {@link ConfigEntry}.
     * <p>
     * <b>Owner:</b> {@link Config}. The field is {@code private} to prevent any external
     * caller from mutating the registry — the only path to add entries is through
     * {@link #register}. Read access is exposed via {@link #entries()} as an
     * unmodifiable view.
     * </p>
     */
    private static final List<ConfigEntry<?>> ENTRIES = new ArrayList<>();

    /**
     * Index of paths already registered, used to enforce path uniqueness in {@link #register}.
     * <p>
     * Maintains the invariant: every entry in {@link #ENTRIES} has a unique YAML path. Without
     * this check, two {@code register()} calls with the same path would silently coexist and
     * the second loader would overwrite the first on every load — a non-deterministic bug.
     * </p>
     */
    private static final Set<String> REGISTERED_PATHS = new HashSet<>();

    /**
     * Returns an unmodifiable snapshot of the registered entries.
     * <p>
     * Callers iterate this list to load, save, or display config values. The returned list
     * cannot be mutated — any attempt throws {@link UnsupportedOperationException}.
     * </p>
     *
     * @return an unmodifiable view of {@link #ENTRIES}
     */
    public static List<ConfigEntry<?>> entries() {
        return Collections.unmodifiableList(ENTRIES);
    }

    /**
     * Register a configuration entry.
     * <p>
     * Called from static initializer blocks in section classes. Enforces:
     * <ul>
     *   <li><b>Path uniqueness</b> — throws {@link IllegalStateException} if {@code path}
     *       has already been registered. Catches accidental duplicates at startup rather
     *       than letting them silently override each other.</li>
     * </ul>
     *
     * @throws IllegalStateException if {@code path} is already registered
     */
    public static <T> void register(
            String path, T defaultValue, Class<T> type, Consumer<T> assign, ConfigEntry.Loader<T> loader) {
        if (!REGISTERED_PATHS.add(path)) {
            throw new IllegalStateException("Duplicate config path registered: " + path);
        }
        ENTRIES.add(new ConfigEntry<>(path, defaultValue, type, assign, loader));
    }

    /**
     * Authoritative list of every config section class.
     * <p>
     * Adding a new section requires exactly one new entry here. Order is irrelevant for
     * correctness but kept alphabetical for review discipline. Class literals do <b>not</b>
     * trigger static initialization on their own — {@link #forceInitializeAll()} drives that.
     * </p>
     */
    private static final List<Class<?>> SECTIONS = List.of(
        AngleConfig.class,
        AnimationConfig.class,
        AttackCurveConfig.class,
        AudioConfig.class,
        ColorConfig.class,
        CombatConfig.class,
        CtfConfig.class,
        DebugConfig.class,
        DetectionConfig.class,
        DirectionConfig.class,
        DisplayConfig.class,
        EntityConfig.class,
        GrabConfig.class,
        HostileConfig.class,
        JoinSequenceConfig.class,
        MaterialConfig.class,
        MenuConfig.class,
        MenuGridConfig.class,
        MovementConfig.class,
        ParticleConfig.class,
        PhysicsConfig.class,
        RoguelikeConfig.class,
        SceneConfig.class,
        TimingConfig.class,
        UmbralBladeConfig.class,
        WorldConfig.class
    );

    /**
     * Forces every section listed in {@link #SECTIONS} to initialize.
     * <p>
     * Each section's static initializer block calls {@link #register} to populate
     * {@link #ENTRIES}. Without this drive call, sections referenced lazily would never
     * register and would be invisible to {@code ConfigManager} and the in-game editor.
     * </p>
     * <p>
     * <b>Idempotent:</b> the JVM guarantees a class's static initializer runs at most
     * once, so repeated calls are safe and produce no duplicate entries.
     * </p>
     * <p>
     * Call once during plugin startup before {@code ConfigManager.loadConfig()}.
     * </p>
     */
    public static void forceInitializeAll() {
        for (Class<?> section : SECTIONS) {
            try {
                Class.forName(section.getName(), true, section.getClassLoader());
            } catch (ClassNotFoundException ignored) {
                // unreachable — class literal proves the class is on the classpath
            }
        }
    }

    // ==============================================================================
    // HELPER METHODS FOR COMMON TYPES
    // ==============================================================================

    /**
     * Loader for List<String> configuration values.
     */
    public static List<String> loadStringList(ConfigurationSection section, String path, List<String> defaultValue) {
        return section.contains(path) ? section.getStringList(path) : defaultValue;
    }

    /** Loader for {@link net.kyori.adventure.text.format.TextColor} configuration values. */
    public static TextColor loadTextColor(ConfigurationSection section, String path, TextColor defaultValue) {
        if (!section.contains(path)) return defaultValue;
        String value = section.getString(path);
        if (value == null || value.isEmpty()) return defaultValue;
        try {
            return TextColor.fromHexString(value);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /** Loader for {@link org.bukkit.Color} configuration values from hex strings. */
    public static org.bukkit.Color loadColor(ConfigurationSection section, String path, org.bukkit.Color defaultValue) {
        if (!section.contains(path)) return defaultValue;
        String value = section.getString(path);
        if (value == null || value.isEmpty()) return defaultValue;

        try {
            if (value.startsWith("#")) value = value.substring(1);

            int rgb = Integer.parseInt(value, 16);
            int r = (rgb >> 16) & 0xFF;
            int g = (rgb >> 8) & 0xFF;
            int b = rgb & 0xFF;

            return org.bukkit.Color.fromRGB(r, g, b);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * Loader for List<EntityType> configuration values.
     */
    public static List<EntityType> loadEntityTypeList(
            ConfigurationSection section, String path, List<EntityType> defaultValue) {
        if (!section.contains(path)) return defaultValue;
        List<String> names = section.getStringList(path);
        return names.stream()
            .map(name -> {
                try {
                    return EntityType.valueOf(name.toUpperCase());
                } catch (IllegalArgumentException e) {
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    /**
     * Loader for Enum configuration values.
     */
    public static <E extends Enum<E>> E loadEnum(
            ConfigurationSection section, String path, E defaultValue, Class<E> enumClass) {
        if (!section.contains(path)) return defaultValue;
        String value = section.getString(path);
        if (value == null) return defaultValue;
        try {
            return Enum.valueOf(enumClass, value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return defaultValue;
        }
    }

    /** Loader for {@link Float} configuration values. */
    public static Float loadFloat(ConfigurationSection section, String path, Float defaultValue) {
        return (float) section.getDouble(path, defaultValue);
    }

    /**
     * Loader for {@link Vector} config values.
     *
     * <p>Expects the config path to point to a section with {@code x}, {@code y}, and {@code z}
     * sub-keys. Missing sub-keys fall back to the corresponding component of {@code defaultValue}.
     */
    public static Vector loadVector(ConfigurationSection section, String path, Vector defaultValue) {
        if (!section.contains(path)) return defaultValue.clone();
        ConfigurationSection vec = section.getConfigurationSection(path);
        if (vec == null) return defaultValue.clone();
        return new Vector(
            vec.getDouble("x", defaultValue.getX()),
            vec.getDouble("y", defaultValue.getY()),
            vec.getDouble("z", defaultValue.getZ())
        );
    }

    /**
     * Loader for {@link Location} config values.
     *
     * <p>Expects the config path to point to a section with the following sub-keys:
     * <ul>
     *   <li>{@code world} — int index into {@link org.bukkit.World.Environment}:<br>
     *       {@code 0} = overworld ({@link org.bukkit.World.Environment#NORMAL})<br>
     *       {@code 1} = nether ({@link org.bukkit.World.Environment#NETHER})<br>
     *       {@code 2} = end ({@link org.bukkit.World.Environment#THE_END})<br>
     *       The first loaded world matching that environment is used.</li>
     *   <li>{@code x}, {@code y}, {@code z} — doubles, default 0.</li>
     *   <li>{@code yaw}, {@code pitch} — doubles, default 0 (cast to float).</li>
     * </ul>
     *
     * <p>If the section is missing, the world index is out of range, or no loaded world matches
     * the requested environment, a clone of {@code defaultValue} is returned.
     *
     * <p><b>Caveat:</b> resolution depends on worlds being loaded at config-load time. If the
     * target world is not yet loaded the default is returned.
     */
    public static Location loadLocation(ConfigurationSection section, String path, Location defaultValue) {
        if (!section.contains(path)) return defaultValue.clone();
        ConfigurationSection loc = section.getConfigurationSection(path);
        if (loc == null) return defaultValue.clone();

        int worldIndex = loc.getInt("world", 0);
        org.bukkit.World.Environment env = switch (worldIndex) {
            case 0 -> org.bukkit.World.Environment.NORMAL;
            case 1 -> org.bukkit.World.Environment.NETHER;
            case 2 -> org.bukkit.World.Environment.THE_END;
            default -> null;
        };
        if (env == null) return defaultValue.clone();

        org.bukkit.World world = Bukkit.getWorlds().stream()
            .filter(w -> w.getEnvironment() == env)
            .findFirst()
            .orElse(null);
        if (world == null) return defaultValue.clone();

        return new Location(
            world,
            loc.getDouble("x", 0),
            loc.getDouble("y", 0),
            loc.getDouble("z", 0),
            (float) loc.getDouble("yaw", 0),
            (float) loc.getDouble("pitch", 0)
        );
    }

    /**
     * Loader for SoundType enum values.
     */
    public static SwordSoundType loadSoundType(ConfigurationSection section, String path, SwordSoundType defaultValue) {
        if (!section.contains(path)) return defaultValue;
        String value = section.getString(path);
        if (value == null) return defaultValue;
        try {
            return SwordSoundType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return defaultValue;
        }
    }

    /**
     * Loader for {@link Material} config values. Reads a material name string and resolves it
     * to the matching {@link Material} enum constant, falling back to {@code defaultValue} on
     * missing path, null, or unrecognised name.
     */
    public static Material loadMaterial(ConfigurationSection section, String path, Material defaultValue) {
        return loadEnum(section, path, defaultValue, Material.class);
    }

    /**
     * Loader for {@link Particle} config values.
     */
    public static Particle loadParticle(ConfigurationSection section, String path, Particle defaultValue) {
        return loadEnum(section, path, defaultValue, Particle.class);
    }

    /** Loader for {@link btm.sword.combat.style.AttackType} configuration values. */
    public static AttackType loadAttackType(ConfigurationSection section, String path, AttackType defaultValue) {
        if (!section.contains(path)) return defaultValue;
        String value = section.getString(path);
        if (value == null) return defaultValue;
        try {
            return AttackType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return defaultValue;
        }
    }
}
