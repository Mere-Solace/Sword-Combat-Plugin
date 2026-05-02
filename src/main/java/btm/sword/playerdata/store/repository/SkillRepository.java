package btm.sword.playerdata.store.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import btm.sword.action.skill.Skill;
import btm.sword.action.skill.SkillId;
import btm.sword.action.skill.SkillRegistry;
import btm.sword.action.skill.container.SkillAvailability;
import btm.sword.action.skill.container.SkillSlot;

/**
 * Handles reads and writes for the {@code player_skill_slots} and
 * {@code player_available_skills} tables.
 * <p>
 * Equipped slots are stored as one row per {@code (uuid, slot)} pair.
 * Available (unlocked) skills are stored as one row per {@code (uuid, skill_id)} pair.
 * </p>
 */
public class SkillRepository {

    private final Connection conn;

    /** @param conn the shared JDBC connection */
    public SkillRepository(Connection conn) {
        this.conn = conn;
    }

    /**
     * Creates both skill tables if they do not already exist.
     *
     * @throws SQLException on any DB error
     */
    public void createTable() throws SQLException {
        try (PreparedStatement s1 = conn.prepareStatement(
                "CREATE TABLE IF NOT EXISTS player_skill_slots ("
                + "uuid TEXT NOT NULL, slot TEXT NOT NULL, skill_id TEXT NOT NULL,"
                + " PRIMARY KEY (uuid, slot))");
             PreparedStatement s2 = conn.prepareStatement(
                "CREATE TABLE IF NOT EXISTS player_available_skills ("
                + "uuid TEXT NOT NULL, skill_id TEXT NOT NULL,"
                + " availability TEXT NOT NULL DEFAULT 'AVAILABLE',"
                + " PRIMARY KEY (uuid, skill_id))")) {
            s1.execute();
            s2.execute();
        }
    }

    /**
     * Migrates the {@code player_available_skills} table from schema v1 (no availability column)
     * to v2 by adding the column with a default of {@code AVAILABLE} for all existing rows.
     *
     * @throws SQLException on any DB error
     */
    public void migrateV1ToV2() throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
                "ALTER TABLE player_available_skills ADD COLUMN"
                + " availability TEXT NOT NULL DEFAULT 'AVAILABLE'")) {
            stmt.execute();
        }
    }

    /**
     * Loads the equipped-slot map and available-skill list for the given player.
     *
     * @param uuid the player's unique ID
     * @return a {@link SkillData} containing both equipped and available skill sets
     * @throws SQLException on any DB error
     */
    public SkillData load(UUID uuid) throws SQLException {
        EnumMap<SkillSlot, SkillId> equipped = new EnumMap<>(SkillSlot.class);
        Map<SkillId, SkillAvailability> available = new HashMap<>();

        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT slot, skill_id FROM player_skill_slots WHERE uuid = ?")) {
            stmt.setString(1, uuid.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    try {
                        SkillSlot slot = SkillSlot.valueOf(rs.getString("slot"));
                        SkillId id = SkillId.parse(rs.getString("skill_id"));
                        equipped.put(slot, id);
                    } catch (IllegalArgumentException e) {
                        // Removed slot or invalid id — skip
                    }
                }
            }
        }

        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT skill_id, availability FROM player_available_skills WHERE uuid = ?")) {
            stmt.setString(1, uuid.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    try {
                        SkillId id = SkillId.parse(rs.getString("skill_id"));
                        SkillAvailability avail = SkillAvailability.valueOf(rs.getString("availability"));
                        available.put(id, avail);
                    } catch (IllegalArgumentException e) {
                        // Invalid id or unknown availability value — skip
                    }
                }
            }
        }

        return new SkillData(available, equipped);
    }

    /**
     * Replaces all skill data for the given player.
     * Deletes existing rows and re-inserts the provided data.
     *
     * @param uuid      the player's unique ID
     * @param equipped  the current equipped-slot map
     * @param available the current available (unlocked) skill list
     * @throws SQLException on any DB error
     */
    public void save(UUID uuid, Map<SkillSlot, SkillId> equipped, Map<SkillId, SkillAvailability> available)
            throws SQLException {
        try (PreparedStatement del1 = conn.prepareStatement(
                "DELETE FROM player_skill_slots WHERE uuid = ?");
             PreparedStatement del2 = conn.prepareStatement(
                "DELETE FROM player_available_skills WHERE uuid = ?")) {
            del1.setString(1, uuid.toString());
            del1.execute();
            del2.setString(1, uuid.toString());
            del2.execute();
        }

        if (!equipped.isEmpty()) {
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO player_skill_slots (uuid, slot, skill_id) VALUES (?, ?, ?)")) {
                for (Map.Entry<SkillSlot, SkillId> entry : equipped.entrySet()) {
                    stmt.setString(1, uuid.toString());
                    stmt.setString(2, entry.getKey().name());
                    stmt.setString(3, entry.getValue().asString());
                    stmt.addBatch();
                }
                stmt.executeBatch();
            }
        }

        if (!available.isEmpty()) {
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO player_available_skills (uuid, skill_id, availability) VALUES (?, ?, ?)")) {
                for (Map.Entry<SkillId, SkillAvailability> entry : available.entrySet()) {
                    Skill skill = SkillRegistry.get(entry.getKey());
                    if (skill == null) continue; // skill was removed from game — don't persist
                    stmt.setString(1, uuid.toString());
                    stmt.setString(2, entry.getKey().asString());
                    stmt.setString(3, entry.getValue().name());
                    stmt.addBatch();
                }
                stmt.executeBatch();
            }
        }
    }

    /**
     * Raw skill data loaded from the database.
     *
     * @param available a map of all discovered skill IDs to their availability state
     * @param equipped  the map of equipped skill slots to their assigned skill
     */
    public record SkillData(Map<SkillId, SkillAvailability> available, EnumMap<SkillSlot, SkillId> equipped) {}
}
