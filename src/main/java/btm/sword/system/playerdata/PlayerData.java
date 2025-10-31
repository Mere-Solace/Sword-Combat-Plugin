package btm.sword.system.playerdata;

import java.util.Date;
import java.util.UUID;

/**
 * Stores persistent data for a player.
 * <p>
 * This class maintains player-specific information including their unique identifier,
 * first login timestamp, and combat profile. This data is typically serialized to JSON
 * for persistent storage and loaded when the player joins.
 * </p>
 *
 * @see CombatProfile
 */
public class PlayerData {
    /** The player's unique identifier. */
    private final UUID uuid;

    /** Timestamp of when the player first joined the server. */
    private final Date dateOfFirstLogin;

    /** The player's combat configuration and stats. */
    private final CombatProfile combatProfile;

    /**
     * Constructs a new PlayerData for the specified player UUID.
     * <p>
     * Initializes the first login date to the current time and creates
     * a new default {@link CombatProfile}.
     * </p>
     *
     * @param uuid the player's unique identifier
     */
    public PlayerData(UUID uuid) {
        this.uuid = uuid;
        dateOfFirstLogin = new Date();
        combatProfile = new CombatProfile();
    }

    /**
     * Gets the player's unique identifier.
     *
     * @return the player UUID
     */
    public UUID getUniqueId() {
        return uuid;
    }

    /**
     * Gets the timestamp of when the player first joined.
     *
     * @return the first login date
     */
    public Date getDateOfFirstLogin() {
        return dateOfFirstLogin;
    }

    /**
     * Gets the player's combat profile.
     *
     * @return the combat profile containing stats and configuration
     */
    public CombatProfile getCombatProfile() {
        return combatProfile;
    }
}
