package btm.sword.config;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.logging.Level;

import org.bukkit.Color;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import btm.sword.Sword;
import net.kyori.adventure.text.format.TextColor;

/**
 * Centralized configuration manager for Sword: Combat Evolved.
 * <p>
 * Loads configuration values from config.yaml into the static {@link Config} class.
 * Provides hot-reload capabilities via {@code /sword reload} without server restart.
 * </p>
 * <p>
 * Thread-safe singleton pattern ensures consistent access across the plugin.
 * Uses generic loading methods to minimize boilerplate and improve maintainability.
 * </p>
 *
 * @see <a href="https://github.com/Mere-Solace/Sword-Combat-Plugin/issues/116">Issue #116</a>
 */
public final class ConfigManager {
    private static ConfigManager instance;

    private final Sword plugin;
    private File configFile;
    private FileConfiguration config;

    /**
     * Private constructor for singleton pattern.
     *
     * @param plugin The main plugin instance
     */
    private ConfigManager(Sword plugin) {
        this.plugin = plugin;
    }

    /**
     * Initializes the configuration manager.
     * <p>
     * Should be called during plugin initialization (onEnable).
     * Creates default config file if it doesn't exist and loads all values.
     * </p>
     *
     * @param plugin The main plugin instance
     */
    public static void initialize(Sword plugin) {
        if (instance == null) {
            instance = new ConfigManager(plugin);
            instance.setupConfig();
            Config.forceInitializeAll();
            instance.loadConfig();
            plugin.getLogger().info("ConfigManager initialized successfully");
        }
    }

    /**
     * Gets the singleton instance of ConfigManager.
     *
     * @return The ConfigManager instance
     * @throws IllegalStateException if ConfigManager hasn't been initialized
     */
    public static ConfigManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("ConfigManager not initialized! Call initialize() first.");
        }
        return instance;
    }

    /**
     * Sets up the config file, creating it from default resource if needed.
     */
    @SuppressWarnings("all")
    private void setupConfig() {
        configFile = new File(plugin.getDataFolder(), "config.yaml");

        if (!configFile.exists()) {
            plugin.getDataFolder().mkdirs();
            try (InputStream defaultConfig = plugin.getResource("config.yaml")) {
                if (defaultConfig != null) {
                    Files.copy(defaultConfig, configFile.toPath());
                    plugin.getLogger().info("Created default config.yaml");
                } else {
                    plugin.getLogger().warning("Default config.yaml not found in resources!");
                    configFile.createNewFile();
                }
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to create config.yaml", e);
            }
        }
    }

    /**
     * Loads (or reloads) the configuration from disk into static Config class.
     * <p>
     * This method can be called at runtime to hot-reload configuration changes.
     * All static fields in Config are updated with new values from disk.
     * Uses the new ConfigEntry registration system to automatically load all fields.
     * </p>
     *
     * @return true if reload was successful, false if errors occurred
     */
    public boolean loadConfig() {
        try {
            config = YamlConfiguration.loadConfiguration(configFile);

            // Load all configuration entries using the registration system
            for (Config.ConfigEntry<?> entry : Config.ENTRIES) {
                loadEntry(entry);
            }

            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load config.yaml! Using previous values.", e);
            return false;
        }
    }

    /**
     * Loads a single ConfigEntry from the YAML configuration.
     * <p>
     * Uses the entry's custom loader and assignment lambda to read from YAML
     * and update the corresponding static field in Config.
     * </p>
     *
     * @param entry The ConfigEntry to load
     * @param <T> The type of the config value
     */
    private <T> void loadEntry(Config.ConfigEntry<T> entry) {
        try {
            T value = entry.loader.load(config, entry.path, entry.defaultValue);
            entry.assign.accept(value);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING,
                "Failed to load config entry '" + entry.path + "', using default: " + entry.defaultValue, e);
            entry.assign.accept(entry.defaultValue);
        }
    }


    /**
     * Reloads the configuration from disk (hot reload).
     * <p>
     * Intended for use with reload commands during runtime testing.
     * </p>
     *
     * @return true if reload was successful
     */
    public boolean reload() {
        plugin.getLogger().info("Reloading configuration...");
        boolean success = loadConfig();
        if (success) {
            plugin.getLogger().info("Configuration reloaded successfully!");
        }
        return success;
    }

    /**
     * Exposes the in-memory {@link FileConfiguration} for reading current config values.
     * The returned object is kept in sync with changes made via {@link #setValue}.
     *
     * @return the live in-memory YAML configuration
     */
    public FileConfiguration getConfig() {
        return config;
    }

    /**
     * Updates a config entry: applies the new value to the static Config field
     * and writes it back into the in-memory YAML so it is reflected in the
     * config menu and persisted by the next {@link #saveConfig} call.
     *
     * @param entry the entry to update
     * @param value the new value
     * @param <T>   the entry type
     */
    public <T> void setValue(Config.ConfigEntry<T> entry, T value) {
        entry.assign.accept(value);
        // Enums must be stored as name strings — YAML has no enum type
        if (value instanceof Enum<?> e) {
            config.set(entry.path, e.name());
        } else if (value instanceof Float f) {
            // Float must be stored as double — YAML has no float type
            config.set(entry.path, (double) f);
        } else if (value instanceof Color c) {
            // Bukkit Color has no YAML serializer; store as hex string matching loadColor()
            config.set(entry.path, String.format("#%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue()));
        } else if (value instanceof TextColor tc) {
            // Adventure TextColor has no YAML serializer; store as hex string matching loadTextColor()
            config.set(entry.path, String.format("#%06X", tc.value()));
        } else if (value instanceof org.bukkit.util.Vector v) {
            // Vector has no flat YAML representation; store as {x,y,z} section matching loadVector()
            org.bukkit.configuration.ConfigurationSection sec = config.createSection(entry.path);
            sec.set("x", v.getX());
            sec.set("y", v.getY());
            sec.set("z", v.getZ());
        } else {
            config.set(entry.path, value);
        }
    }

    /**
     * Saves the current in-memory configuration to the server's {@code plugins/sword/config.yaml}.
     * Only entries modified via {@link #setValue} (e.g. from the in-game config menu) are
     * guaranteed to be up-to-date; values changed directly on static fields are not captured.
     */
    public void saveConfig() {
        try {
            config.save(configFile);
            plugin.getLogger().info("Configuration saved to disk");
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save config.yaml", e);
        }
    }

    /**
     * Saves the current in-memory configuration to the project's
     * {@code src/main/resources/config.yaml}, keeping the bundled default in sync
     * with any runtime changes made via the in-game config menu.
     * <p>
     * Only works when the server is running from the Gradle {@code runServer} task
     * (i.e. the server root is {@code <project>/run/}). A warning is logged and the
     * save is skipped if the project config file cannot be found.
     * </p>
     */
    public void saveToProject() {
        // Walk up from the plugin data folder looking for build.gradle, which uniquely marks the
        // project root regardless of whether the server runs from run/, ./, or elsewhere.
        File projectRoot = null;
        File dir = plugin.getDataFolder().getAbsoluteFile();
        while (dir != null) {
            if (new File(dir, "build.gradle").exists() || new File(dir, "build.gradle.kts").exists()) {
                projectRoot = dir;
                break;
            }
            dir = dir.getParentFile();
        }
        if (projectRoot == null) {
            plugin.getLogger().warning("Project root not found (no build.gradle) — is the server running via ./gradlew runServer?");
            return;
        }
        File projectConfig = new File(projectRoot, "src/main/resources/config.yaml");
        if (!projectConfig.exists()) {
            plugin.getLogger().warning("Project config not found at: " + projectConfig.getAbsolutePath());
            return;
        }
        try {
            config.save(projectConfig);
            plugin.getLogger().info("Config saved to project source: " + projectConfig.getAbsolutePath());
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save config to project source", e);
        }
    }

    /**
     * Resets configuration to default values.
     * <p>
     * Backs up existing config and replaces with default from resources.
     * </p>
     *
     * @return true if reset was successful
     */
    @SuppressWarnings("all")
    public boolean resetToDefaults() {
        try {
            // Backup current config
            File backup = new File(plugin.getDataFolder(), "config.yaml.backup");
            Files.copy(configFile.toPath(), backup.toPath());

            // Delete current and recreate from defaults
            configFile.delete();
            setupConfig();
            loadConfig();

            plugin.getLogger().info("Configuration reset to defaults (backup saved as config.yaml.backup)");
            return true;
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to reset configuration", e);
            return false;
        }
    }
}
