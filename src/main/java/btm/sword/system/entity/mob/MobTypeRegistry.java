package btm.sword.system.entity.mob;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nullable;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.java.JavaPlugin;

import btm.sword.Sword;

/**
 * Registry for all {@link MobTypeDefinition} entries loaded from {@code mob_types.yml}.
 * <p>
 * Call {@link #initialize(JavaPlugin)} once during {@code Sword.onEnable()} after
 * {@link btm.sword.config.ConfigManager} and
 * {@link btm.sword.system.scene.animation.AnimationRegistry} are ready.
 * Hot-reloads can call {@link #reload()} at any time (e.g. from {@code /sword reload}).
 * </p>
 *
 * <h2>YAML format</h2>
 * <pre>
 * pillager:
 *   entity_type: PILLAGER           # vanilla EntityType name
 *   display:
 *     group: "witha"                # DEU group tag to spawn as the display rig
 *     animation_prefix: "witha"     # prefix for all animation tags; full tag = prefix_idle, etc.
 *                                   # defaults to the group name if omitted
 *   stats: {}                       # optional overrides on top of global CombatProfile defaults
 *   abilities: []                   # reserved for future ability system integration
 * </pre>
 */
public class MobTypeRegistry {

    private static final String FILE_NAME = "mob_types.yml";
    private static Map<String, MobTypeDefinition> byId = Collections.emptyMap();
    private static Map<EntityType, MobTypeDefinition> byEntityType = Collections.emptyMap();
    private static JavaPlugin plugin;

    private MobTypeRegistry() {}

    /**
     * Initialises the registry from {@code mob_types.yml}, copying the bundled default
     * if the file does not yet exist in the plugin data folder.
     *
     * @param javaPlugin the owning plugin instance
     */
    public static void initialize(JavaPlugin javaPlugin) {
        plugin = javaPlugin;
        saveDefaultIfAbsent();
        reload();
    }

    /**
     * Re-parses {@code mob_types.yml} without restarting the server.
     * Safe to call at any time after {@link #initialize}.
     */
    public static void reload() {
        File file = new File(plugin.getDataFolder(), FILE_NAME);
        if (!file.exists()) {
            Sword.getInstance().getLogger().warning(
                "[MobTypeRegistry] mob_types.yml not found — no mob types loaded."
            );
            byId = Collections.emptyMap();
            byEntityType = Collections.emptyMap();
            return;
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);

        Map<String, MobTypeDefinition> loadedById = new HashMap<>();
        Map<EntityType, MobTypeDefinition> loadedByType = new EnumMap<>(EntityType.class);

        for (String id : yaml.getKeys(false)) {
            ConfigurationSection section = yaml.getConfigurationSection(id);
            if (section == null) continue;

            EntityType entityType = parseEntityType(id, section.getString("entity_type"));
            if (entityType == null) continue;

            String displayGroup = null;
            AnimationSlots animationSlots = AnimationSlots.EMPTY;
            ConfigurationSection displaySection = section.getConfigurationSection("display");
            if (displaySection != null) {
                displayGroup = displaySection.getString("group");
                // animation_prefix defaults to the group name when omitted.
                String prefix = displaySection.getString("animation_prefix", displayGroup);
                if (prefix != null && !prefix.isEmpty()) {
                    ConfigurationSection lengths = displaySection.getConfigurationSection("animation_lengths");
                    animationSlots = new AnimationSlots(
                        slot(prefix + "_idle",   lengths, "idle"),
                        slot(prefix + "_walk",   lengths, "walk"),
                        slot(prefix + "_fall",   lengths, "fall"),
                        slot(prefix + "_attack", lengths, "attack"),
                        slot(prefix + "_die",    lengths, "die")
                    );
                }
            }

            Map<String, Float> statOverrides = new HashMap<>();
            ConfigurationSection statsSection = section.getConfigurationSection("stats");
            if (statsSection != null) {
                for (String key : statsSection.getKeys(false)) {
                    statOverrides.put(key.toLowerCase(), (float) statsSection.getDouble(key));
                }
            }

            List<String> abilityIds = section.getStringList("abilities");

            MobTypeDefinition def = new MobTypeDefinition(
                id, entityType, displayGroup, animationSlots,
                Collections.unmodifiableMap(statOverrides),
                Collections.unmodifiableList(abilityIds)
            );
            loadedById.put(id, def);
            loadedByType.put(entityType, def);
        }

        byId = Collections.unmodifiableMap(loadedById);
        byEntityType = Collections.unmodifiableMap(loadedByType);
        Sword.getInstance().getLogger().info(
            "[MobTypeRegistry] Loaded " + loadedById.size() + " mob type(s)."
        );
    }

    /**
     * Returns the {@link MobTypeDefinition} for the given registry id.
     *
     * @param id the mob type id (e.g. {@code "pillager"})
     * @return the definition, or empty if not registered
     */
    public static Optional<MobTypeDefinition> get(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    /**
     * Returns the {@link MobTypeDefinition} for the given entity type, or {@code null}
     * if no definition has been registered for that type.
     *
     * @param type the vanilla entity type
     * @return the definition, or {@code null} if not found
     */
    public static @Nullable MobTypeDefinition getByEntityType(EntityType type) {
        return byEntityType.get(type);
    }

    // ------------------------------------------------------------------

    private static @Nullable EntityType parseEntityType(String id, @Nullable String raw) {
        if (raw == null) {
            Sword.getInstance().getLogger().warning(
                "[MobTypeRegistry] Mob type '" + id + "' is missing 'entity_type' — skipping."
            );
            return null;
        }
        try {
            return EntityType.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException e) {
            Sword.getInstance().getLogger().warning(
                "[MobTypeRegistry] Unknown entity_type '" + raw + "' in mob type '" + id + "' — skipping."
            );
            return null;
        }
    }

    /** Builds an {@link AnimationSlot} from a tag and an optional length section. */
    private static AnimationSlot slot(@Nullable String tag, @Nullable ConfigurationSection lengths, String key) {
        if (tag == null || tag.isEmpty()) return AnimationSlot.NONE;
        int ticks = (lengths != null) ? lengths.getInt(key, 0) : 0;
        return new AnimationSlot(tag, ticks);
    }

    private static void saveDefaultIfAbsent() {
        File file = new File(plugin.getDataFolder(), FILE_NAME);
        if (file.exists()) return;
        try (InputStream in = plugin.getResource(FILE_NAME)) {
            if (in == null) {
                Sword.getInstance().getLogger().warning(
                    "[MobTypeRegistry] No bundled mob_types.yml found in jar."
                );
                return;
            }
            file.getParentFile().mkdirs();
            Files.copy(in, file.toPath());
        } catch (IOException e) {
            Sword.getInstance().getLogger().warning(
                "[MobTypeRegistry] Failed to save default mob_types.yml: " + e.getMessage()
            );
        }
    }
}
