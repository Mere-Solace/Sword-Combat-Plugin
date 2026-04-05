package btm.sword.system.attack.def;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
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
     * Returns an unmodifiable view of all registered attack definitions, keyed by id.
     *
     * @return all registered attacks
     */
    public static Collection<AttackDef> getAll() {
        return Collections.unmodifiableCollection(REGISTRY.values());
    }

    /**
     * Removes the attack with the given id from the registry.
     *
     * @param id the attack id to remove
     */
    public static void unregister(String id) {
        REGISTRY.remove(id);
    }

    /**
     * Loads all {@code .yml} files from {@code dir} as attack definitions, registering
     * each one. Files that fail to parse are skipped with a warning. Idempotent —
     * safe to call multiple times (later calls overwrite earlier ones with the same id).
     *
     * @param dir the directory to scan (must exist and be a directory)
     */
    public static void loadDirectory(File dir) {
        if (!dir.isDirectory()) return;
        File[] files = dir.listFiles((d, name) -> name.endsWith(".yml"));
        if (files == null) return;
        for (File file : files) {
            loadAll(file);
        }
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
