package btm.sword.combat.hit;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import btm.sword.Sword;
import btm.sword.combat.attack.Blockability;

/**
 * Static registry of named {@link HitPacketPreset}s loaded from
 * {@code plugins/sword/hit-packets.yaml}.
 *
 * <p>Presets are keyed by {@link HitPacketPreset#id()} and may be registered, retrieved,
 * removed, and persisted at runtime. {@link #bootstrap(Sword)} copies the bundled default
 * resource into the plugin data folder on first run and loads all entries.</p>
 */
public final class HitPacketRegistry {

    private static final ConcurrentHashMap<String, HitPacketPreset> PRESETS = new ConcurrentHashMap<>();
    private static File storageFile;

    private HitPacketRegistry() {}

    /**
     * Initializes the registry from {@code plugins/sword/hit-packets.yaml}. If the file
     * does not exist, the bundled default resource is copied into place.
     *
     * @param plugin the Sword plugin instance
     */
    public static void bootstrap(Sword plugin) {
        storageFile = new File(plugin.getDataFolder(), "hit-packets.yaml");
        if (!storageFile.exists()) {
            plugin.getDataFolder().mkdirs();
            try (InputStream defaultYaml = plugin.getResource("hit-packets.yaml")) {
                if (defaultYaml != null) {
                    Files.copy(defaultYaml, storageFile.toPath());
                    plugin.getLogger().info("Created default hit-packets.yaml");
                } else {
                    storageFile.createNewFile();
                }
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to create hit-packets.yaml", e);
            }
        }
        loadAll(storageFile);
    }

    /**
     * Loads all presets from the given YAML file, replacing the current registry contents.
     *
     * @param file the YAML file to read
     */
    public static void loadAll(File file) {
        PRESETS.clear();
        if (!file.exists()) return;

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yaml.getConfigurationSection("hit-packets");
        if (root == null) return;

        for (String id : root.getKeys(false)) {
            ConfigurationSection s = root.getConfigurationSection(id);
            if (s == null) continue;
            try {
                PRESETS.put(id, readPreset(id, s));
            } catch (Exception e) {
                Sword.getInstance().getLogger().log(Level.WARNING,
                    "Failed to load hit-packet preset '" + id + "'", e);
            }
        }
    }

    /**
     * Writes all currently registered presets to the registry's storage file.
     */
    public static void saveAll() {
        if (storageFile == null) return;
        save(storageFile);
    }

    /**
     * Writes all currently registered presets to the given YAML file.
     *
     * @param file destination YAML file
     */
    public static void save(File file) {
        YamlConfiguration yaml = new YamlConfiguration();
        for (HitPacketPreset preset : PRESETS.values()) {
            String path = "hit-packets." + preset.id();
            yaml.set(path + ".display-name", preset.displayName());
            yaml.set(path + ".shard-damage", preset.shardDamage());
            yaml.set(path + ".toughness-damage", (double) preset.toughnessDamage());
            yaml.set(path + ".soulfire-loss", (double) preset.soulfireLoss());
            yaml.set(path + ".reaped-soulfire", (double) preset.reapedSoulfire());
            yaml.set(path + ".invulnerable-ticks", preset.invulnerableTicks());
            yaml.set(path + ".blockability", preset.blockability().name());
            yaml.set(path + ".bypass-power", (double) preset.bypassPower());
        }
        try {
            yaml.save(file);
        } catch (IOException e) {
            Sword.getInstance().getLogger().log(Level.SEVERE,
                "Failed to save hit-packets.yaml", e);
        }
    }

    /**
     * Registers or replaces a preset in the registry. Does not persist to disk;
     * call {@link #saveAll()} afterwards to write changes.
     *
     * @param preset the preset to register
     */
    public static void register(HitPacketPreset preset) {
        PRESETS.put(preset.id(), preset);
    }

    /**
     * Removes the preset with the given id. Does not persist to disk;
     * call {@link #saveAll()} afterwards to write changes.
     *
     * @param id the preset id to remove
     * @return {@code true} if a preset was removed
     */
    public static boolean remove(String id) {
        return PRESETS.remove(id) != null;
    }

    /**
     * @param id the preset id to look up
     * @return the matching preset, or {@code null} if not registered
     */
    public static HitPacketPreset get(String id) {
        return PRESETS.get(id);
    }

    /** @return an unmodifiable snapshot of all registered presets */
    public static Collection<HitPacketPreset> all() {
        return PRESETS.values();
    }

    /** @return all registered presets sorted by id */
    public static List<HitPacketPreset> allSorted() {
        List<HitPacketPreset> out = new ArrayList<>(PRESETS.values());
        out.sort(Comparator.comparing(HitPacketPreset::id));
        return out;
    }

    private static HitPacketPreset readPreset(String id, ConfigurationSection s) {
        String displayName = s.getString("display-name", id);
        int shard = s.getInt("shard-damage", 0);
        float tough = (float) s.getDouble("toughness-damage", 0.0);
        float soulLoss = (float) s.getDouble("soulfire-loss", 0.0);
        float reaped = (float) s.getDouble("reaped-soulfire", 0.0);
        int invulTicks = s.getInt("invulnerable-ticks", 10);
        Blockability block = Blockability.BLOCKABLE;
        String blockStr = s.getString("blockability");
        if (blockStr != null) {
            try {
                block = Blockability.valueOf(blockStr.toUpperCase());
            } catch (IllegalArgumentException ignored) { }
        }
        float bypass = (float) s.getDouble("bypass-power", 0.0);
        return new HitPacketPreset(id, displayName, shard, tough, soulLoss, reaped, invulTicks, block, bypass);
    }

    /** Used by test code to preseed presets without touching disk. */
    static Map<String, HitPacketPreset> snapshot() {
        return new LinkedHashMap<>(PRESETS);
    }
}
