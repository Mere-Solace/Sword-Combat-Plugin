package btm.sword.playerdata.store;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

import btm.sword.playerdata.PlayerData;

/**
 * Backend-agnostic contract for player data persistence.
 * <p>
 * Implementations may back this with SQLite (local dev), MySQL (hosted), or any other
 * JDBC-compatible store. The {@link btm.sword.playerdata.PlayerDataManager} depends
 * on this interface; swapping the implementation requires no changes to calling code.
 * </p>
 */
public interface PlayerDataStore {

    /**
     * Loads a single player's data from the backing store.
     * Returns {@code null} if no record exists for the given UUID (first-time player).
     *
     * @param uuid the player's unique ID
     * @return the loaded {@link PlayerData}, or {@code null} if not found
     */
    PlayerData load(UUID uuid);

    /**
     * Persists a single player's data to the backing store.
     * Inserts or updates as needed (upsert semantics).
     *
     * @param uuid the player's unique ID
     * @param data the current player data to persist
     */
    void save(UUID uuid, PlayerData data);

    /**
     * Persists data for all players in the given collection.
     * Equivalent to calling {@link #save} for each entry; implementations may
     * batch these writes for efficiency.
     *
     * @param uuids the UUIDs to save; data is retrieved from the in-memory cache
     * @param cache the full in-memory player data map
     */
    void saveAll(Collection<UUID> uuids, Map<UUID, PlayerData> cache);

    /**
     * Closes any open connections or resources held by this store.
     * Called once on server shutdown after the final flush.
     */
    void close();
}
