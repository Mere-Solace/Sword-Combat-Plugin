package btm.sword.entity.mob;

import org.bukkit.entity.LivingEntity;

import btm.sword.entity.base.CombatProfile;
import btm.sword.entity.base.SwordEntity;

/** Represents a non-hostile entity that joins the green team and does not engage in combat. */
public class Passive extends SwordEntity {

    /** Constructs a passive entity wrapping the given living entity with the provided combat profile. */
    public Passive(LivingEntity associatedEntity, CombatProfile combatProfile) {
        super(associatedEntity, combatProfile);
    }

    @Override
    public void onSpawn() {
        super.onSpawn();
        joinTeam(btm.sword.entity.team.SwordTeam.GREEN);
    }

    @Override
    public void onDeath() {
        super.onDeath();

    }
}
