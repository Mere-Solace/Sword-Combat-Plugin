package btm.sword.system.entity.types;

import org.bukkit.entity.LivingEntity;

import btm.sword.system.entity.base.CombatProfile;
import btm.sword.system.entity.base.SwordEntity;

public class Passive extends SwordEntity {

    public Passive(LivingEntity associatedEntity, CombatProfile combatProfile) {
        super(associatedEntity, combatProfile);
    }

    @Override
    public void onSpawn() {
        super.onSpawn();

    }

    @Override
    public void onDeath() {
        super.onDeath();

    }
}
