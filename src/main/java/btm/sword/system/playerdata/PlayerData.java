package btm.sword.system.playerdata;

import java.util.Date;
import java.util.UUID;

public class PlayerData {
    private final UUID uuid;
    private final Date dateOfFirstLogin;
    private final CombatProfile combatProfile;

    public PlayerData(UUID uuid) {
        this.uuid = uuid;
        dateOfFirstLogin = new Date();
        combatProfile = new CombatProfile();
    }

    public UUID getUniqueId() {
        return uuid;
    }

    public Date getDateOfFirstLogin() {
        return dateOfFirstLogin;
    }

    public CombatProfile getCombatProfile() {
        return combatProfile;
    }
}
