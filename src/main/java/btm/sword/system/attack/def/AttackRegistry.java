package btm.sword.system.attack.def;

import java.io.File;
import java.util.concurrent.ConcurrentHashMap;

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
     * Existing entries with matching ids are replaced.
     *
     * @param attacksYml the YAML file to load from
     * @todo #319 — implement once AttackDefSerializer is built
     */
    public static void loadAll(File attacksYml) {
        // TODO: #319 — delegate to AttackDefSerializer once implemented
    }
}
