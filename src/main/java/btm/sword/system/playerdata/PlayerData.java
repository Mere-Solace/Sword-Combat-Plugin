package btm.sword.system.playerdata;

import java.util.Date;
import java.util.UUID;

import btm.sword.system.entity.base.CombatProfile;
import lombok.Getter;

@Getter
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
}
