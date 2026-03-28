package btm.sword.system.playerdata.store.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

import btm.sword.system.action.skill.container.SkillSlot;

/**
 * Handles reads and writes for the {@code player_ability_state} table.
 *
 * <p>Each row stores the remaining resource counts for a single ability slot (e.g.
 * {@link SkillSlot#ACTIVE_1} or {@link SkillSlot#ACTIVE_2}). Both
 * {@link btm.sword.system.action.skill.AbilityUseType#STACK} and
 * {@link btm.sword.system.action.skill.AbilityUseType#DURABILITY} are persisted
 * independently per slot. {@code -1} in either column indicates that tracking type is
 * not active for the current ability in that slot.
 * {@link btm.sword.system.action.skill.AbilityUseType#COOLDOWN} is never persisted
 * (cooldowns reset across sessions).</p>
 */
public class AbilityStateRepository {

    /**
     * Persisted resource state for a single ability slot.
     *
     * @param remainingUses       remaining stack-use count, or {@code -1} if not tracked
     * @param remainingDurability remaining durability, or {@code -1} if not tracked
     */
    public record SlotState(int remainingUses, int remainingDurability) {}

    private final Connection conn;

    /**
     * Constructs a repository backed by the given JDBC connection.
     *
     * @param conn the shared JDBC connection
     */
    public AbilityStateRepository(Connection conn) {
        this.conn = conn;
    }

    /**
     * Creates the ability state table if it does not already exist.
     *
     * @throws SQLException on any DB error
     */
    public void createTable() throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
                "CREATE TABLE IF NOT EXISTS player_ability_state ("
                + "uuid TEXT NOT NULL,"
                + "slot TEXT NOT NULL,"
                + "remaining_uses INTEGER NOT NULL DEFAULT -1,"
                + "remaining_durability INTEGER NOT NULL DEFAULT -1,"
                + "PRIMARY KEY (uuid, slot))")) {
            stmt.execute();
        }
    }

    /**
     * Loads the resource state for all active ability slots for the given player.
     *
     * @param uuid the player's unique ID
     * @return a map of slot to {@link SlotState} (only contains entries for slots with persisted data)
     * @throws SQLException on any DB error
     */
    public Map<SkillSlot, SlotState> load(UUID uuid) throws SQLException {
        EnumMap<SkillSlot, SlotState> result = new EnumMap<>(SkillSlot.class);
        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT slot, remaining_uses, remaining_durability"
                + " FROM player_ability_state WHERE uuid = ?")) {
            stmt.setString(1, uuid.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    try {
                        SkillSlot slot = SkillSlot.valueOf(rs.getString("slot"));
                        int uses = rs.getInt("remaining_uses");
                        int durability = rs.getInt("remaining_durability");
                        result.put(slot, new SlotState(uses, durability));
                    } catch (IllegalArgumentException e) {
                        // Unknown slot name — skip
                    }
                }
            }
        }
        return result;
    }

    /**
     * Persists the resource state for a single ability slot using {@code INSERT OR REPLACE}.
     *
     * @param uuid               the player's unique ID
     * @param slot               the ability slot
     * @param remainingUses      remaining stack-use count, or {@code -1} if not tracked
     * @param remainingDurability remaining durability, or {@code -1} if not tracked
     * @throws SQLException on any DB error
     */
    public void save(UUID uuid, SkillSlot slot, int remainingUses, int remainingDurability) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
                "INSERT OR REPLACE INTO player_ability_state (uuid, slot, remaining_uses, remaining_durability)"
                + " VALUES (?, ?, ?, ?)")) {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, slot.name());
            stmt.setInt(3, remainingUses);
            stmt.setInt(4, remainingDurability);
            stmt.execute();
        }
    }

    /**
     * Persists resource state for multiple slots in a batch.
     *
     * @param uuid      the player's unique ID
     * @param slotStates map of slot to {@link SlotState}
     * @throws SQLException on any DB error
     */
    public void saveAll(UUID uuid, Map<SkillSlot, SlotState> slotStates) throws SQLException {
        if (slotStates.isEmpty()) return;
        try (PreparedStatement stmt = conn.prepareStatement(
                "INSERT OR REPLACE INTO player_ability_state (uuid, slot, remaining_uses, remaining_durability)"
                + " VALUES (?, ?, ?, ?)")) {
            for (Map.Entry<SkillSlot, SlotState> entry : slotStates.entrySet()) {
                stmt.setString(1, uuid.toString());
                stmt.setString(2, entry.getKey().name());
                stmt.setInt(3, entry.getValue().remainingUses());
                stmt.setInt(4, entry.getValue().remainingDurability());
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }
}
