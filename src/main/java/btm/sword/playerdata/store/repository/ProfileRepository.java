package btm.sword.playerdata.store.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.UUID;

import btm.sword.playerdata.SwordClassType;

/**
 * Handles reads and writes for the {@code player_profiles} table.
 * One row per player; stores identity and progression fields.
 */
public class ProfileRepository {

    private final Connection conn;

    /** @param conn the shared JDBC connection */
    public ProfileRepository(Connection conn) {
        this.conn = conn;
    }

    /**
     * Creates the {@code player_profiles} table if it does not already exist.
     *
     * @throws SQLException on any DB error
     */
    public void createTable() throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
                "CREATE TABLE IF NOT EXISTS player_profiles ("
                + "uuid TEXT PRIMARY KEY, "
                + "sword_class TEXT NOT NULL DEFAULT 'SWORD_THROWER', "
                + "join_sequence_completed INTEGER NOT NULL DEFAULT 0, "
                + "first_login INTEGER NOT NULL, "
                + "max_air_dodges INTEGER NOT NULL DEFAULT 2"
                + ")")) {
            stmt.execute();
        }
    }

    /**
     * Loads profile data for the given player.
     * Returns {@code null} if no row exists.
     *
     * @param uuid the player's unique ID
     * @return a {@link ProfileRow} with the stored data, or {@code null}
     * @throws SQLException on any DB error
     */
    public ProfileRow load(UUID uuid) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT sword_class, join_sequence_completed, first_login, max_air_dodges"
                + " FROM player_profiles WHERE uuid = ?")) {
            stmt.setString(1, uuid.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) return null;
                SwordClassType swordClass;
                try {
                    swordClass = SwordClassType.valueOf(rs.getString("sword_class"));
                } catch (IllegalArgumentException e) {
                    swordClass = SwordClassType.SWORD_THROWER;
                }
                boolean joinDone = rs.getInt("join_sequence_completed") == 1;
                Date firstLogin = new Date(rs.getLong("first_login"));
                int maxAirDodges = rs.getInt("max_air_dodges");
                return new ProfileRow(swordClass, joinDone, firstLogin, maxAirDodges);
            }
        }
    }

    /**
     * Upserts profile data for the given player.
     * Inserts a new row or updates existing fields (excluding {@code first_login}).
     *
     * @param uuid                  the player's unique ID
     * @param swordClass            the player's combat class
     * @param joinSequenceCompleted whether the player has completed the intro cutscene
     * @param firstLoginMs          the first-login timestamp in epoch millis
     * @param maxAirDodges          the player's air-dodge cap
     * @throws SQLException on any DB error
     */
    public void save(UUID uuid, SwordClassType swordClass, boolean joinSequenceCompleted,
            long firstLoginMs, int maxAirDodges) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO player_profiles (uuid, sword_class, join_sequence_completed, first_login, max_air_dodges)"
                + " VALUES (?, ?, ?, ?, ?)"
                + " ON CONFLICT(uuid) DO UPDATE SET"
                + " sword_class=excluded.sword_class,"
                + " join_sequence_completed=excluded.join_sequence_completed,"
                + " max_air_dodges=excluded.max_air_dodges")) {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, swordClass.name());
            stmt.setInt(3, joinSequenceCompleted ? 1 : 0);
            stmt.setLong(4, firstLoginMs);
            stmt.setInt(5, maxAirDodges);
            stmt.execute();
        }
    }

    /**
     * Raw data loaded from {@code player_profiles}.
     *
     * @param swordClass            the stored combat class
     * @param joinSequenceCompleted the stored intro-cutscene completion flag
     * @param firstLogin            the stored first-login timestamp
     * @param maxAirDodges          the stored air-dodge cap
     */
    public record ProfileRow(SwordClassType swordClass, boolean joinSequenceCompleted,
            Date firstLogin, int maxAirDodges) {}
}
