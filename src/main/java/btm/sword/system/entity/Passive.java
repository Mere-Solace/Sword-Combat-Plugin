package btm.sword.system.entity;

import btm.sword.system.playerdata.CombatProfile;
import org.bukkit.entity.LivingEntity;

/**
 * Represents a passive (non-hostile) NPC entity.
 * <p>
 * Passive entities extend {@link SwordEntity} but do not engage in combat.
 * They can still have aspects and resources for potential future interactions.
 * </p>
 *
 * <p><b>Note:</b> This class is currently a minimal implementation and may be
 * expanded in future updates with passive-specific behaviors. See Issue #37.</p>
 *
 * @see SwordEntity
 * @see Hostile
 */
public class Passive extends SwordEntity {

    /**
     * Constructs a Passive entity with the specified Bukkit entity and combat profile.
     *
     * @param associatedEntity the Bukkit LivingEntity to wrap
     * @param combatProfile the combat profile with stats and resources
     */
    public Passive(LivingEntity associatedEntity, CombatProfile combatProfile) {
        super(associatedEntity, combatProfile);
    }

    /**
     * Called when the entity spawns.
     * <p>
     * Currently empty - may be implemented for passive-specific spawn logic.
     * </p>
     */
    @Override
    public void onSpawn() {

    }

    /**
     * Called when the entity dies.
     * <p>
     * Currently empty - may be implemented for passive-specific death handling.
     * </p>
     */
    @Override
    public void onDeath() {

    }
}
