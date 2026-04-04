package btm.sword.system.attack.def;

import java.io.File;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import btm.sword.utility.Debug;

/**
 * Global registry of all named {@link AttackDef}s.
 *
 * <p>Attacks are registered at plugin startup and on {@code /sword reload}.
 * All lookups are O(1) via a {@link ConcurrentHashMap}.</p>
 */
public final class AttackRegistry {

    private static final ConcurrentHashMap<String, AttackDef> REGISTRY = new ConcurrentHashMap<>();

    private AttackRegistry() {}

    /**
     * Registers an attack definition, replacing any existing entry with the same id.
     *
     * @param def the attack definition to register
     */
    public static void register(AttackDef def) {
        REGISTRY.put(def.getId(), def);
    }

    /**
     * Returns the {@link AttackDef} for the given id.
     *
     * @param id the unique attack identifier
     * @return the registered attack definition
     * @throws IllegalArgumentException if no attack is registered under {@code id}
     */
    public static AttackDef get(String id) {
        AttackDef def = REGISTRY.get(id);
        if (def == null) throw new IllegalArgumentException("No AttackDef registered for id: " + id);
        return def;
    }

    /**
     * Bulk-loads attack definitions from {@code attacks.yml} and registers them all.
     * Existing entries with matching ids are replaced. Malformed entries are skipped
     * with a warning rather than aborting the whole load.
     *
     * @param attacksYml the YAML file to load from
     */
    public static void loadAll(File attacksYml) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(attacksYml);
        ConfigurationSection attacks = yaml.getConfigurationSection("attacks");
        if (attacks == null) return;
        for (String id : attacks.getKeys(false)) {
            ConfigurationSection section = attacks.getConfigurationSection(id);
            if (section == null) continue;
            try {
                register(AttackDefSerializer.load(section, id));
            } catch (Exception e) {
                Debug.system("AttackRegistry: failed to load attack '" + id + "': " + e.getMessage());
            }
        }
    }
}
