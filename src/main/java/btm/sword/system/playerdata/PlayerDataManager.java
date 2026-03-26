package btm.sword.system.playerdata;

import java.io.File;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;

import btm.sword.Sword;
import btm.sword.config.Config;
import btm.sword.system.playerdata.store.PlayerDataStore;
import btm.sword.system.playerdata.store.SqlitePlayerDataStore;

/**
 * Manages persistent player data for all online players.
 *
 * <h2>Architecture</h2>
 * An in-memory {@code Map<UUID, PlayerData>} acts as the hot cache — all in-game reads
 * hit this map directly at zero latency. The backing {@link PlayerDataStore} (SQLite by
 * default) is the source of truth on disk.
 *
 * <h2>Save strategy</h2>
 * <ul>
 *   <li><b>On quit</b> — async save of that player's data only (via the plugin scheduler).</li>
 *   <li><b>Every 5 minutes</b> — async flush of all online players.</li>
 *   <li><b>On shutdown</b> — synchronous flush of all cached data before the connection closes.</li>
 * </ul>
 */
public class PlayerDataManager {

    private static final Map<UUID, PlayerData> cache = new ConcurrentHashMap<>();
    private static PlayerDataStore store;

    /**
     * Initialises the data manager: opens the SQLite connection, creates schema,
     * loads any players already online, and schedules the periodic async flush.
     */
    public static void initialize() {
        File dbFile = new File("plugins/sword/playerdata.db");
        SqlitePlayerDataStore sqliteStore = new SqlitePlayerDataStore(dbFile, Sword.getInstance().getLogger());
        try {
            sqliteStore.open();
        } catch (SQLException e) {
            Sword.getInstance().getLogger().severe("[PlayerData] Failed to open database: " + e.getMessage());
            // Fall through — store is still assigned so null-checks below protect callers
        }
        store = sqliteStore;

        for (org.bukkit.entity.Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            register(onlinePlayer);
        }

        // Periodic async flush — every 5 minutes
        Sword.getScheduler().scheduleAtFixedRate(
            PlayerDataManager::flushAll, 5, 5, TimeUnit.MINUTES);
    }

    /**
     * Synchronously flushes all cached player data to disk and closes the store connection.
     * Called once on server shutdown.
     * <p>
     * If {@link Config.Debug#SKIP_DATA_SAVE} is {@code true}, the flush is skipped but the
     * database connection is still closed cleanly.
     * </p>
     */
    public static void shutdown() {
        if (store == null) return;
        if (!Config.Debug.SKIP_DATA_SAVE) {
            store.saveAll(cache.keySet(), cache);
        }
        store.close();
    }

    /**
     * Registers a player, loading their data from the database if it exists
     * or creating a fresh default record for first-time players.
     * <p>
     * If {@link Config.Debug#SKIP_DATA_LOAD} is {@code true}, the database load is skipped
     * entirely and a fresh {@link PlayerData} is always created, simulating a first-time join.
     * </p>
     *
     * @param entity the player entity to register
     */
    public static void register(LivingEntity entity) {
        UUID uuid = entity.getUniqueId();
        if (cache.containsKey(uuid)) return;

        if (Config.Debug.SKIP_DATA_LOAD) {
            cache.put(uuid, new PlayerData(uuid));
            return;
        }

        PlayerData loaded = store != null ? store.load(uuid) : null;
        cache.put(uuid, loaded != null ? loaded : new PlayerData(uuid));
    }

    /**
     * Returns the cached {@link PlayerData} for the given UUID, or {@code null} if not loaded.
     *
     * @param uuid the player's unique ID
     * @return their data, or {@code null}
     */
    public static PlayerData getPlayerData(UUID uuid) {
        return cache.get(uuid);
    }

    /**
     * Asynchronously saves a single player's data to the database.
     * Call this when a player quits so their data is persisted promptly.
     * <p>
     * This method is a no-op when {@link Config.Debug#SKIP_DATA_SAVE} is {@code true}.
     * </p>
     *
     * @param uuid the player's unique ID
     */
    public static void saveAsync(UUID uuid) {
        if (Config.Debug.SKIP_DATA_SAVE) return;
        PlayerData data = cache.get(uuid);
        if (data == null || store == null) return;
        Sword.getScheduler().submit(() -> store.save(uuid, data));
    }

    /**
     * Removes a player's data from the in-memory cache.
     * Should be called <em>after</em> {@link #saveAsync} on player quit so the data
     * is still available to the async save task.
     *
     * @param uuid the player's unique ID
     */
    public static void evict(UUID uuid) {
        cache.remove(uuid);
    }

    /**
     * Replaces the cached {@link PlayerData} for the given UUID with a completely fresh instance,
     * as if the player had never joined before. Session-only — no database interaction.
     * <p>
     * Useful for testing the first-join sequence without clearing the database. The change takes
     * effect immediately; the player's next action will reflect the fresh data.
     * </p>
     *
     * @param uuid the UUID of the online player whose session data should be reset
     */
    public static void resetToFresh(UUID uuid) {
        cache.put(uuid, new PlayerData(uuid));
    }

    // =========================================================================
    // Internal helpers
    // =========================================================================

    /**
     * Async flush of all currently cached players — called by the periodic scheduler.
     * <p>
     * This method is a no-op when {@link Config.Debug#SKIP_DATA_SAVE} is {@code true}.
     * </p>
     */
    public static void flushAll() {
        if (Config.Debug.SKIP_DATA_SAVE) return;
        if (store == null || cache.isEmpty()) return;
        Collection<UUID> snapshot = cache.keySet();
        store.saveAll(snapshot, cache);
    }
}
