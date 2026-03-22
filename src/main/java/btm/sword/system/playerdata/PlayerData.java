package btm.sword.system.playerdata;

import java.util.Date;
import java.util.UUID;

import btm.sword.system.entity.base.CombatProfile;
import lombok.Getter;
import lombok.Setter;

@Getter
public class PlayerData {
    private final UUID uuid;
    private final Date dateOfFirstLogin;
    private final CombatProfile combatProfile;

    /**
     * Whether this player has completed the initial join sequence (intro cutscene + first setup).
     * When {@code false}, the server-join pipeline routes them to the intro cutscene instead of
     * the main menu character scene.
     */
    @Getter
    @Setter
    private boolean joinSequenceCompleted = false;

    public PlayerData(UUID uuid) {
        this.uuid = uuid;
        dateOfFirstLogin = new Date();
        combatProfile = new CombatProfile();

    }

    public UUID getUniqueId() {
        return uuid;
    }
}
