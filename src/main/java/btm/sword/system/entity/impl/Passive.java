package btm.sword.system.entity.impl;

import org.bukkit.entity.LivingEntity;

import btm.sword.system.entity.base.CombatProfile;
import btm.sword.system.entity.base.SwordEntity;

/** Represents a non-hostile entity that joins the green team and does not engage in combat. */
public class Passive extends SwordEntity {

    /** Constructs a passive entity wrapping the given living entity with the provided combat profile. */
    public Passive(LivingEntity associatedEntity, CombatProfile combatProfile) {
        super(associatedEntity, combatProfile);
    }

    @Override
    public void onSpawn() {
        super.onSpawn();
        joinTeam(btm.sword.system.entity.SwordTeam.GREEN);
    }

    @Override
    public void onDeath() {
        super.onDeath();

    }
}
