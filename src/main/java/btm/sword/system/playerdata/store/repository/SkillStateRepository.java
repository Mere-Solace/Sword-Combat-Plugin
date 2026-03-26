package btm.sword.system.playerdata.store.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

import btm.sword.system.action.skill.container.SkillSlot;
import btm.sword.system.action.skill.container.SkillSlotState;

/**
 * Handles reads and writes for the {@code player_skill_slot_state} table.
 * <p>
 * Each row tracks the persisted resource state for one ability slot:
 * remaining uses, remaining durability, and cooldown expiry timestamp (epoch-millis).
 * This lets cooldowns survive log-off/log-in cycles so players cannot reset
 * long-duration abilities by disconnecting.
 * </p>
 */
public class SkillStateRepository {

    private final Connection conn;

    /** @param conn the shared JDBC connection */
    public SkillStateRepository(Connection conn) {
        this.conn = conn;
    }

    /**
     * Creates the slot-state table if it does not already exist.
     *
     * @throws SQLException on any DB error
     */
    public void createTable() throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
                "CREATE TABLE IF NOT EXISTS player_skill_slot_state ("
                + "uuid TEXT NOT NULL, slot TEXT NOT NULL,"
                + " remaining_uses INTEGER NOT NULL DEFAULT -1,"
                + " remaining_durability INTEGER NOT NULL DEFAULT -1,"
                + " cooldown_expires_at INTEGER NOT NULL DEFAULT 0,"
                + " PRIMARY KEY (uuid, slot))")) {
            stmt.execute();
        }
    }

    /**
     * Loads all persisted slot states for the given player.
     *
     * @param uuid the player's unique ID
     * @return map of skill slot to its state; missing slots default to {@link SkillSlotState#EMPTY}
     * @throws SQLException on any DB error
     */
    public Map<SkillSlot, SkillSlotState> load(UUID uuid) throws SQLException {
        Map<SkillSlot, SkillSlotState> result = new EnumMap<>(SkillSlot.class);
        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT slot, remaining_uses, remaining_durability, cooldown_expires_at"
                + " FROM player_skill_slot_state WHERE uuid = ?")) {
            stmt.setString(1, uuid.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    try {
                        SkillSlot slot = SkillSlot.valueOf(rs.getString("slot"));
                        int uses = rs.getInt("remaining_uses");
                        int durability = rs.getInt("remaining_durability");
                        long cooldown = rs.getLong("cooldown_expires_at");
                        result.put(slot, new SkillSlotState(uses, durability, cooldown));
                    } catch (IllegalArgumentException e) {
                        // Unknown or removed slot — skip
                    }
                }
            }
        }
        return result;
    }

    /**
     * Replaces all slot-state data for the given player.
     *
     * @param uuid   the player's unique ID
     * @param states the current slot states to persist
     * @throws SQLException on any DB error
     */
    public void save(UUID uuid, Map<SkillSlot, SkillSlotState> states) throws SQLException {
        try (PreparedStatement del = conn.prepareStatement(
                "DELETE FROM player_skill_slot_state WHERE uuid = ?")) {
            del.setString(1, uuid.toString());
            del.execute();
        }
        if (states.isEmpty()) return;
        try (PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO player_skill_slot_state"
                + " (uuid, slot, remaining_uses, remaining_durability, cooldown_expires_at)"
                + " VALUES (?, ?, ?, ?, ?)")) {
            for (Map.Entry<SkillSlot, SkillSlotState> entry : states.entrySet()) {
                SkillSlotState state = entry.getValue();
                stmt.setString(1, uuid.toString());
                stmt.setString(2, entry.getKey().name());
                stmt.setInt(3, state.remainingUses());
                stmt.setInt(4, state.remainingDurability());
                stmt.setLong(5, state.cooldownExpiresAt());
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }
}
