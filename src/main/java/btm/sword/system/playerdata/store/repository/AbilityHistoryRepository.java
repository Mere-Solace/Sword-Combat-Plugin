package btm.sword.system.playerdata.store.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import btm.sword.system.action.skill.SkillId;
import btm.sword.system.action.skill.history.AbilityHistoryAction;
import btm.sword.system.action.skill.history.AbilityHistoryEntry;
import btm.sword.system.action.skill.history.PlayerAbilityHistory;

/**
 * Handles reads and writes for the {@code player_ability_history} table.
 *
 * <p>Each row represents one {@link AbilityHistoryEntry}: the player UUID, the ability id,
 * the display name at time of event, the action (USED / SURRENDERED), and the timestamp.
 * Rows are append-only — existing entries are never updated or deleted.
 */
public class AbilityHistoryRepository {

    private final Connection conn;

    /**
     * Constructs a repository backed by the given JDBC connection.
     *
     * @param conn the shared JDBC connection
     */
    public AbilityHistoryRepository(Connection conn) {
        this.conn = conn;
    }

    /**
     * Creates the history table if it does not already exist.
     *
     * @throws SQLException on any DB error
     */
    public void createTable() throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
                "CREATE TABLE IF NOT EXISTS player_ability_history ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "uuid TEXT NOT NULL,"
                + "skill_id TEXT NOT NULL,"
                + "display_name TEXT NOT NULL,"
                + "action TEXT NOT NULL,"
                + "timestamp INTEGER NOT NULL)")) {
            stmt.execute();
        }
    }

    /**
     * Loads all history entries for the given player, oldest-first.
     *
     * @param uuid the player's unique ID
     * @return their full ability history
     * @throws SQLException on any DB error
     */
    public PlayerAbilityHistory load(UUID uuid) throws SQLException {
        List<AbilityHistoryEntry> entries = new ArrayList<>();

        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT skill_id, display_name, action, timestamp"
                + " FROM player_ability_history WHERE uuid = ? ORDER BY timestamp ASC")) {
            stmt.setString(1, uuid.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    try {
                        SkillId id = SkillId.parse(rs.getString("skill_id"));
                        String name = rs.getString("display_name");
                        AbilityHistoryAction action = AbilityHistoryAction.valueOf(rs.getString("action"));
                        long ts = rs.getLong("timestamp");
                        entries.add(new AbilityHistoryEntry(id, name, action, ts));
                    } catch (IllegalArgumentException e) {
                        // Removed skill or malformed action — skip row
                    }
                }
            }
        }

        return new PlayerAbilityHistory(entries);
    }

    /**
     * Appends any entries in {@code history} that are not yet in the database for this player.
     * Uses the entry count currently stored to determine the offset — new entries added
     * during this session are inserted; previously persisted rows are untouched.
     *
     * @param uuid    the player's unique ID
     * @param history the full in-memory history to persist
     * @throws SQLException on any DB error
     */
    public void save(UUID uuid, PlayerAbilityHistory history) throws SQLException {
        List<AbilityHistoryEntry> all = history.entries();
        if (all.isEmpty()) return;

        // Count how many rows we already have for this player so we only insert new ones.
        int stored = 0;
        try (PreparedStatement count = conn.prepareStatement(
                "SELECT COUNT(*) FROM player_ability_history WHERE uuid = ?")) {
            count.setString(1, uuid.toString());
            try (ResultSet rs = count.executeQuery()) {
                if (rs.next()) stored = rs.getInt(1);
            }
        }

        List<AbilityHistoryEntry> newEntries = all.subList(stored, all.size());
        if (newEntries.isEmpty()) return;

        try (PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO player_ability_history (uuid, skill_id, display_name, action, timestamp)"
                + " VALUES (?, ?, ?, ?, ?)")) {
            for (AbilityHistoryEntry entry : newEntries) {
                stmt.setString(1, uuid.toString());
                stmt.setString(2, entry.id().asString());
                stmt.setString(3, entry.displayName());
                stmt.setString(4, entry.action().name());
                stmt.setLong(5, entry.timestamp());
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }
}
