package btm.sword.system.entity.types;

import btm.sword.system.entity.base.SwordEntity;
import btm.sword.system.entity.base.CombatProfile;
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
