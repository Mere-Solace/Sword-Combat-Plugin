package btm.sword.system.playerdata.store.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

import btm.sword.system.entity.aspect.AspectType;
import btm.sword.system.entity.aspect.value.AspectValue;
import btm.sword.system.entity.aspect.value.ResourceValue;

/**
 * Handles reads and writes for the {@code player_aspects} table.
 * One row per {@code (player, aspect_type)} pair; stores base stat values
 * for both {@link AspectValue} and {@link ResourceValue} instances.
 */
public class AspectRepository {

    private final Connection conn;

    /**
     * Constructs a repository backed by the given JDBC connection.
     *
     * @param conn the shared JDBC connection
     */
    public AspectRepository(Connection conn) {
        this.conn = conn;
    }

    /**
     * Creates the {@code player_aspects} table if it does not already exist.
     *
     * @throws SQLException on any DB error
     */
    public void createTable() throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
                "CREATE TABLE IF NOT EXISTS player_aspects ("
                + "uuid TEXT NOT NULL, "
                + "aspect_type TEXT NOT NULL, "
                + "value REAL NOT NULL, "
                + "regen_period INTEGER, "
                + "regen_amount REAL, "
                + "PRIMARY KEY (uuid, aspect_type)"
                + ")")) {
            stmt.execute();
        }
    }

    /**
     * Loads all stored aspect values for the given player.
     * Unknown or removed aspect types are silently skipped.
     *
     * @param uuid the player's unique ID
     * @return a map of {@link AspectType} to its stored {@link AspectValue} (may be empty)
     * @throws SQLException on any DB error
     */
    public Map<AspectType, AspectValue> load(UUID uuid) throws SQLException {
        Map<AspectType, AspectValue> result = new EnumMap<>(AspectType.class);
        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT aspect_type, value, regen_period, regen_amount FROM player_aspects WHERE uuid = ?")) {
            stmt.setString(1, uuid.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    AspectType type;
                    try {
                        type = AspectType.valueOf(rs.getString("aspect_type"));
                    } catch (IllegalArgumentException e) {
                        continue; // aspect was removed from the game — skip
                    }
                    float value = rs.getFloat("value");
                    int regenPeriod = rs.getInt("regen_period");
                    boolean isResource = !rs.wasNull();
                    if (isResource) {
                        float regenAmount = rs.getFloat("regen_amount");
                        result.put(type, new ResourceValue(value, regenPeriod, regenAmount));
                    } else {
                        result.put(type, new AspectValue(value));
                    }
                }
            }
        }
        return result;
    }

    /**
     * Upserts all provided aspect values for the given player in a single batch.
     *
     * @param uuid  the player's unique ID
     * @param stats the current aspect values to persist
     * @throws SQLException on any DB error
     */
    public void save(UUID uuid, Map<AspectType, AspectValue> stats) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO player_aspects (uuid, aspect_type, value, regen_period, regen_amount)"
                + " VALUES (?, ?, ?, ?, ?)"
                + " ON CONFLICT(uuid, aspect_type) DO UPDATE SET"
                + " value=excluded.value, regen_period=excluded.regen_period, regen_amount=excluded.regen_amount")) {
            for (Map.Entry<AspectType, AspectValue> entry : stats.entrySet()) {
                stmt.setString(1, uuid.toString());
                stmt.setString(2, entry.getKey().name());
                stmt.setFloat(3, entry.getValue().getValue());
                if (entry.getValue() instanceof ResourceValue rv) {
                    stmt.setInt(4, rv.getRegenPeriod());
                    stmt.setFloat(5, rv.getRegenAmount());
                } else {
                    stmt.setNull(4, Types.INTEGER);
                    stmt.setNull(5, Types.REAL);
                }
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }
}
