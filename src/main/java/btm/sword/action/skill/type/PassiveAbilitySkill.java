package btm.sword.action.skill.type;

import btm.sword.action.skill.AbilityType;

/**
 * An ability that is always active while equipped in a passive slot.
 *
 * <p>No activation input is needed. The physical world item grants the passive effect
 * when picked up and equipped — it is never consumed on use. Extend this class and
 * implement {@link #buildWorldItem}.
 */
public abstract class PassiveAbilitySkill extends PassiveSkill implements AbilitySkill {

    @Override
    public AbilityType abilityType() {
        return AbilityType.PASSIVE;
    }

    @Override
    public boolean consumesOnUse() {
        return false;
    }

    @Override
    public boolean requiresPhysicalItemToUse() {
        return false;
    }
}
