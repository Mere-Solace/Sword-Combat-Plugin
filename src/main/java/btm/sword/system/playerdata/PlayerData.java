package btm.sword.system.playerdata;

import java.util.Date;
import java.util.UUID;

import btm.sword.system.entity.base.CombatProfile;
import lombok.Getter;
import lombok.Setter;

/**
 * Persistent data container for a single player.
 * <p>
 * Holds all data that survives across sessions: identity fields, combat profile,
 * economy storage, and progression flags. An instance is created fresh on first join
 * and loaded from the database on subsequent joins.
 * </p>
 */
@Getter
public class PlayerData {
    private final UUID uuid;
    private final Date dateOfFirstLogin;
    private final CombatProfile combatProfile;
    private final PlayerStorage playerStorage;

    /**
     * Whether this player has completed the initial join sequence (intro cutscene + first setup).
     * When {@code false}, the server-join pipeline routes them to the intro cutscene instead of
     * the main menu character scene.
     */
    @Getter
    @Setter
    private boolean joinSequenceCompleted = false;

    /**
     * Constructs a new {@code PlayerData} for a first-time player.
     * All values default to fresh/empty state.
     *
     * @param uuid the player's unique ID
     */
    public PlayerData(UUID uuid) {
        this.uuid = uuid;
        dateOfFirstLogin = new Date();
        combatProfile = new CombatProfile();
        playerStorage = new PlayerStorage();
    }

    /**
     * Constructs a {@code PlayerData} from data loaded out of the database.
     * Used exclusively by the persistence layer — do not call from game logic.
     *
     * @param uuid                  the player's unique ID
     * @param dateOfFirstLogin      the stored first-login timestamp
     * @param joinSequenceCompleted the stored join-sequence completion flag
     * @param combatProfile         the restored combat profile
     * @param playerStorage         the restored economy storage
     */
    public PlayerData(UUID uuid, Date dateOfFirstLogin, boolean joinSequenceCompleted,
            CombatProfile combatProfile, PlayerStorage playerStorage) {
        this.uuid = uuid;
        this.dateOfFirstLogin = dateOfFirstLogin;
        this.joinSequenceCompleted = joinSequenceCompleted;
        this.combatProfile = combatProfile;
        this.playerStorage = playerStorage;
    }

    /** @return this player's unique ID (alias for {@link #getUuid()} for convenience). */
    public UUID getUniqueId() {
        return uuid;
    }
}
