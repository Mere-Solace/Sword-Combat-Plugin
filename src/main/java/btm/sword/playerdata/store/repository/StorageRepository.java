package btm.sword.playerdata.store.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

import btm.sword.item.material.MaterialType;
import btm.sword.playerdata.PlayerStorage;

/**
 * Handles reads and writes for the {@code player_storage} and {@code player_materials} tables.
 * <p>
 * {@code player_storage} stores per-player economy scalars (credits, auto-pickup toggles).
 * {@code player_materials} stores per-player material counts as one row per material type.
 * </p>
 */
public class StorageRepository {

    private final Connection conn;

    /** @param conn the shared JDBC connection */
    public StorageRepository(Connection conn) {
        this.conn = conn;
    }

    /**
     * Creates both storage tables if they do not already exist.
     *
     * @throws SQLException on any DB error
     */
    public void createTable() throws SQLException {
        try (PreparedStatement s1 = conn.prepareStatement(
                "CREATE TABLE IF NOT EXISTS player_storage ("
                + "uuid TEXT PRIMARY KEY, "
                + "steel_credits INTEGER NOT NULL DEFAULT 100, "
                + "auto_pickup_materials INTEGER NOT NULL DEFAULT 0, "
                + "auto_pickup_credits INTEGER NOT NULL DEFAULT 0)");
             PreparedStatement s2 = conn.prepareStatement(
                "CREATE TABLE IF NOT EXISTS player_materials ("
                + "uuid TEXT NOT NULL, "
                + "material_type TEXT NOT NULL, "
                + "count INTEGER NOT NULL DEFAULT 0, "
                + "PRIMARY KEY (uuid, material_type))")) {
            s1.execute();
            s2.execute();
        }
    }

    /**
     * Loads economy storage data for the given player.
     * Returns a fresh default {@link PlayerStorage} if no row exists (first-time player).
     *
     * @param uuid the player's unique ID
     * @return the loaded (or default) {@link PlayerStorage}
     * @throws SQLException on any DB error
     */
    public PlayerStorage load(UUID uuid) throws SQLException {
        int credits = 100;
        boolean autoMaterials = false;
        boolean autoCredits = false;

        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT steel_credits, auto_pickup_materials, auto_pickup_credits"
                + " FROM player_storage WHERE uuid = ?")) {
            stmt.setString(1, uuid.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    credits = rs.getInt("steel_credits");
                    autoMaterials = rs.getInt("auto_pickup_materials") == 1;
                    autoCredits = rs.getInt("auto_pickup_credits") == 1;
                }
            }
        }

        Map<MaterialType, Integer> materialCounts = new EnumMap<>(MaterialType.class);
        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT material_type, count FROM player_materials WHERE uuid = ?")) {
            stmt.setString(1, uuid.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    try {
                        MaterialType type = MaterialType.valueOf(rs.getString("material_type"));
                        materialCounts.put(type, rs.getInt("count"));
                    } catch (IllegalArgumentException e) {
                        // Removed material type — skip
                    }
                }
            }
        }

        return new PlayerStorage(credits, autoMaterials, autoCredits, materialCounts);
    }

    /**
     * Upserts all economy data for the given player.
     *
     * @param uuid    the player's unique ID
     * @param storage the current player storage to persist
     * @throws SQLException on any DB error
     */
    public void save(UUID uuid, PlayerStorage storage) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO player_storage (uuid, steel_credits, auto_pickup_materials, auto_pickup_credits)"
                + " VALUES (?, ?, ?, ?)"
                + " ON CONFLICT(uuid) DO UPDATE SET"
                + " steel_credits=excluded.steel_credits,"
                + " auto_pickup_materials=excluded.auto_pickup_materials,"
                + " auto_pickup_credits=excluded.auto_pickup_credits")) {
            stmt.setString(1, uuid.toString());
            stmt.setInt(2, storage.getSteelCredits());
            stmt.setInt(3, storage.isAutoPickupMaterials() ? 1 : 0);
            stmt.setInt(4, storage.isAutoPickupCredits() ? 1 : 0);
            stmt.execute();
        }

        // Delete existing material rows and re-insert non-zero counts
        try (PreparedStatement del = conn.prepareStatement(
                "DELETE FROM player_materials WHERE uuid = ?")) {
            del.setString(1, uuid.toString());
            del.execute();
        }

        Map<MaterialType, Integer> counts = storage.getMaterialCountsSnapshot();
        if (!counts.isEmpty()) {
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO player_materials (uuid, material_type, count) VALUES (?, ?, ?)")) {
                for (Map.Entry<MaterialType, Integer> entry : counts.entrySet()) {
                    if (entry.getValue() <= 0) continue;
                    stmt.setString(1, uuid.toString());
                    stmt.setString(2, entry.getKey().name());
                    stmt.setInt(3, entry.getValue());
                    stmt.addBatch();
                }
                stmt.executeBatch();
            }
        }
    }
}
