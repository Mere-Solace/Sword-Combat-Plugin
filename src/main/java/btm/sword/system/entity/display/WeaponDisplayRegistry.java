package btm.sword.system.entity.display;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.EnumMap;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import btm.sword.Sword;

/**
 * Registry for per-material {@link WeaponDisplayTransform} entries loaded from
 * {@code weapon_display.yml}.
 *
 * <p>Call {@link #initialize(JavaPlugin)} once during {@code Sword.onEnable()}.
 * Hot-reloads call {@link #reload()}; the dev menu editor calls {@link #set} and
 * {@link #save()} to persist in-session tweaks back to disk.</p>
 *
 * <h2>YAML format</h2>
 * <pre>
 * materials:
 *   IRON_SWORD:
 *     offset_right:   0.3
 *     offset_up:      1.4
 *     offset_forward: 0.1
 *     rot_x:  0.0
 *     rot_y: 45.0
 *     rot_z:  0.0
 *     scale:  1.0
 * </pre>
 */
public class WeaponDisplayRegistry {

    private static final String FILE_NAME = "weapon_display.yml";
    private static Map<Material, WeaponDisplayTransform> transforms = new EnumMap<>(Material.class);
    private static JavaPlugin plugin;

    private WeaponDisplayRegistry() {}

    /**
     * Initialises the registry from {@code weapon_display.yml}, copying the bundled default
     * if the file does not yet exist.
     *
     * @param javaPlugin the owning plugin instance
     */
    public static void initialize(JavaPlugin javaPlugin) {
        plugin = javaPlugin;
        saveDefaultIfAbsent();
        reload();
    }

    /**
     * Re-parses {@code weapon_display.yml} without restarting the server.
     */
    public static void reload() {
        File file = new File(plugin.getDataFolder(), FILE_NAME);
        if (!file.exists()) {
            transforms = new EnumMap<>(Material.class);
            return;
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        Map<Material, WeaponDisplayTransform> loaded = new EnumMap<>(Material.class);

        ConfigurationSection materialsSection = yaml.getConfigurationSection("materials");
        if (materialsSection != null) {
            for (String matName : materialsSection.getKeys(false)) {
                Material mat;
                try {
                    mat = Material.valueOf(matName.toUpperCase());
                } catch (IllegalArgumentException e) {
                    Sword.getInstance().getLogger().warning(
                        "[WeaponDisplayRegistry] Unknown material '" + matName + "' — skipping."
                    );
                    continue;
                }
                ConfigurationSection s = materialsSection.getConfigurationSection(matName);
                if (s == null) continue;

                loaded.put(mat, new WeaponDisplayTransform(
                    (float) s.getDouble("offset_right",   0),
                    (float) s.getDouble("offset_up",      0),
                    (float) s.getDouble("offset_forward", 0),
                    (float) s.getDouble("rot_x",  0),
                    (float) s.getDouble("rot_y",  0),
                    (float) s.getDouble("rot_z",  0),
                    (float) s.getDouble("scale",  1.0)
                ));
            }
        }

        transforms = loaded;
        Sword.getInstance().getLogger().info(
            "[WeaponDisplayRegistry] Loaded " + transforms.size() + " material transform(s)."
        );
    }

    /**
     * Returns the transform for the given material, or {@link WeaponDisplayTransform#DEFAULT}
     * if none is configured.
     *
     * @param material the item material
     * @return the registered transform or DEFAULT
     */
    public static WeaponDisplayTransform get(Material material) {
        return transforms.getOrDefault(material, WeaponDisplayTransform.DEFAULT);
    }

    /**
     * Updates the in-memory transform for a material.
     * Call {@link #save()} afterwards to persist the change.
     *
     * @param material  the material key
     * @param transform the new transform
     */
    public static void set(Material material, WeaponDisplayTransform transform) {
        Map<Material, WeaponDisplayTransform> mutable = new EnumMap<>(transforms);
        mutable.put(material, transform);
        transforms = mutable;
    }

    /**
     * Writes all current in-memory transforms back to {@code weapon_display.yml}.
     */
    public static void save() {
        File file = new File(plugin.getDataFolder(), FILE_NAME);
        YamlConfiguration yaml = new YamlConfiguration();

        for (Map.Entry<Material, WeaponDisplayTransform> entry : transforms.entrySet()) {
            String base = "materials." + entry.getKey().name();
            WeaponDisplayTransform t = entry.getValue();
            yaml.set(base + ".offset_right",   (double) t.offsetRight());
            yaml.set(base + ".offset_up",       (double) t.offsetUp());
            yaml.set(base + ".offset_forward",  (double) t.offsetForward());
            yaml.set(base + ".rot_x",  (double) t.rotX());
            yaml.set(base + ".rot_y",  (double) t.rotY());
            yaml.set(base + ".rot_z",  (double) t.rotZ());
            yaml.set(base + ".scale",  (double) t.scale());
        }

        try {
            yaml.save(file);
        } catch (IOException e) {
            Sword.getInstance().getLogger().warning(
                "[WeaponDisplayRegistry] Failed to save weapon_display.yml: " + e.getMessage()
            );
        }
    }

    // -------------------------------------------------------

    private static void saveDefaultIfAbsent() {
        File file = new File(plugin.getDataFolder(), FILE_NAME);
        if (file.exists()) return;
        try (InputStream in = plugin.getResource(FILE_NAME)) {
            if (in == null) {
                Sword.getInstance().getLogger().warning(
                    "[WeaponDisplayRegistry] No bundled weapon_display.yml found in jar."
                );
                return;
            }
            file.getParentFile().mkdirs();
            Files.copy(in, file.toPath());
        } catch (IOException e) {
            Sword.getInstance().getLogger().warning(
                "[WeaponDisplayRegistry] Failed to save default weapon_display.yml: " + e.getMessage()
            );
        }
    }
}
