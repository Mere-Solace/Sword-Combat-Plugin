package btm.sword.config;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.logging.Level;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import btm.sword.Sword;

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
     * Saves the current configuration to disk.
     * <p>
     * Note: This saves the in-memory config state from the YAML file.
     * To persist changes made to static Config fields, they must first
     * be written back to the config object.
     * </p>
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
