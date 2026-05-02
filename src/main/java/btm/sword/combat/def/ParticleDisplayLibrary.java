package btm.sword.combat.def;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.bukkit.configuration.file.YamlConfiguration;

import btm.sword.Sword;
import btm.sword.combat.visuals.ParticleDisplay;

/**
 * Static registry of named {@link ParticleDisplay} presets persisted in
 * {@code plugins/sword/particles.yml}.
 *
 * <p>Presets are keyed by a user-supplied name (e.g. {@code "sweep_trail"}).
 * The file is loaded at plugin startup via {@link #load(File)} and can be
 * reloaded at any time via the same method. Saves are explicit via
 * {@link #save(File)} — triggered by the in-game editor when the player
 * adds or removes a preset.</p>
 *
 * <p>YAML format:</p>
 * <pre>
 * presets:
 *   sweep_trail:
 *     shape: LINE
 *     anchor: {kind: OWNING}
 *     ...
 *   crit_flash:
 *     shape: POINT
 *     ...
 * </pre>
 */
public final class ParticleDisplayLibrary {

    private static final Map<String, ParticleDisplay> PRESETS = new LinkedHashMap<>();

    private ParticleDisplayLibrary() {}

    /**
     * Loads (or reloads) all presets from {@code particles.yml}. Clears the existing
     * registry first so stale entries are not retained across reloads.
     *
     * @param file the {@code particles.yml} file to read; created with defaults if absent
     */
    public static void load(File file) {
        PRESETS.clear();
        if (!file.exists()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        var section = yaml.getConfigurationSection("presets");
        if (section == null) return;
        for (String key : section.getKeys(false)) {
            var raw = section.get(key);
            if (raw instanceof Map<?, ?> map) {
                try {
                    PRESETS.put(key, ParticleDisplaySerializer.load(map));
                } catch (Exception e) {
                    Sword.getInstance().getLogger().warning(
                        "[ParticleDisplayLibrary] Failed to load preset '" + key + "': " + e.getMessage());
                }
            }
        }
    }

    /**
     * Writes the current registry to {@code particles.yml}.
     *
     * @param file the target file
     * @throws IOException if the file cannot be written
     */
    public static void save(File file) throws IOException {
        YamlConfiguration yaml = new YamlConfiguration();
        Map<String, Object> presets = new LinkedHashMap<>();
        for (Map.Entry<String, ParticleDisplay> entry : PRESETS.entrySet()) {
            presets.put(entry.getKey(), ParticleDisplaySerializer.serialize(entry.getValue()));
        }
        yaml.set("presets", presets);
        yaml.save(file);
    }

    /**
     * Adds or replaces the preset with the given name and immediately saves to disk.
     *
     * @param name    the preset name (will be used as the YAML key)
     * @param display the display to store; a deep copy is made via {@link ParticleDisplay#copy()}
     * @param file    the {@code particles.yml} file to save to
     */
    public static void register(String name, ParticleDisplay display, File file) {
        PRESETS.put(name, display.copy());
        try {
            save(file);
        } catch (IOException e) {
            Sword.getInstance().getLogger().warning(
                "[ParticleDisplayLibrary] Save failed after register: " + e.getMessage());
        }
    }

    /**
     * Removes the preset with the given name and immediately saves to disk.
     *
     * @param name the preset name to remove
     * @param file the {@code particles.yml} file to save to
     */
    public static void remove(String name, File file) {
        if (PRESETS.remove(name) != null) {
            try {
                save(file);
            } catch (IOException e) {
                Sword.getInstance().getLogger().warning(
                    "[ParticleDisplayLibrary] Save failed after remove: " + e.getMessage());
            }
        }
    }

    /**
     * Returns an unmodifiable ordered view of all named presets.
     *
     * @return map of name → display (insertion order preserved)
     */
    public static Map<String, ParticleDisplay> getAll() {
        return Collections.unmodifiableMap(PRESETS);
    }

    /** Returns {@code true} if a preset with the given name exists. */
    public static boolean contains(String name) {
        return PRESETS.containsKey(name);
    }
}
