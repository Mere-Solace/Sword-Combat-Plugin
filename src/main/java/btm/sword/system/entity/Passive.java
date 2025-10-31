package btm.sword.system.entity;

import btm.sword.system.playerdata.CombatProfile;
import org.bukkit.entity.LivingEntity;

public class Passive extends SwordEntity {

    public Passive(LivingEntity associatedEntity, CombatProfile combatProfile) {
        super(associatedEntity, combatProfile);
    }

    @Override
    public void onSpawn() {

    }

    @Override
    public void onDeath() {

    }
}
