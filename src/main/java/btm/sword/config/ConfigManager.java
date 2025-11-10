package btm.sword.config;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.logging.Level;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import btm.sword.Sword;
import btm.sword.config.section.AudioConfig;
import btm.sword.config.section.CombatConfig;
import btm.sword.config.section.DebugConfig;
import btm.sword.config.section.DetectionConfig;
import btm.sword.config.section.DisplayConfig;
import btm.sword.config.section.EntityConfig;
import btm.sword.config.section.MovementConfig;
import btm.sword.config.section.PhysicsConfig;
import btm.sword.config.section.TimingConfig;
import btm.sword.config.section.WorldConfig;
import lombok.Getter;

/**
 * Centralized configuration manager for Sword: Combat Evolved.
 * <p>
 * Provides type-safe access to all configuration values with hot-reload capabilities.
 * Values can be updated at runtime using {@code /sword reload} without server restart.
 * </p>
 * <p>
 * Thread-safe singleton pattern ensures consistent access across the plugin.
 * </p>
 *
 * @see <a href="https://github.com/Mere-Solace/Sword-Combat-Plugin/issues/66">Issue #66</a>
 */
public final class ConfigManager {
    private static ConfigManager instance;

    @Getter
    private final Sword plugin;

    private File configFile;
    private FileConfiguration config;

    // Section accessors for type-safe config access
    @Getter
    private PhysicsConfig physics;
    @Getter
    private CombatConfig combat;
    @Getter
    private DisplayConfig display;
    @Getter
    private DetectionConfig detection;
    @Getter
    private TimingConfig timing;
    @Getter
    private AudioConfig audio;
    @Getter
    private EntityConfig entities;
    @Getter
    private MovementConfig movement;
    @Getter
    private WorldConfig world;
    @Getter
    private DebugConfig debug;

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
     * Loads (or reloads) the configuration from disk.
     * <p>
     * This method can be called at runtime to hot-reload configuration changes.
     * All cached section objects are recreated with new values.
     * </p>
     *
     * @return true if reload was successful, false if errors occurred
     */
    public boolean loadConfig() {
        try {
            config = YamlConfiguration.loadConfiguration(configFile);

            // Reload all section accessors
            physics = new PhysicsConfig(config);
            combat = new CombatConfig(config);
            display = new DisplayConfig(config);
            detection = new DetectionConfig(config);
            timing = new TimingConfig(config);
            audio = new AudioConfig(config);
            entities = new EntityConfig(config);
            movement = new MovementConfig(config);
            world = new WorldConfig(config);
            debug = new DebugConfig(config);

            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load config.yaml! Using previous values.", e);
            return false;
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
     * Note: This saves the in-memory config state, not the section objects.
     * Typically used after programmatic config modifications.
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
     * Gets the raw Bukkit FileConfiguration object.
     * <p>
     * For advanced use cases. Prefer using typed section accessors when possible.
     * </p>
     *
     * @return The underlying FileConfiguration
     */
    public FileConfiguration getRawConfig() {
        return config;
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
