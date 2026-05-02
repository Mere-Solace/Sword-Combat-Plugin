package btm.sword.action.skill.type;

import btm.sword.action.skill.AbilityType;

/**
 * An ability that is pressed to activate, has a cooldown, and is never consumed.
 *
 * <p>The physical world item grants access to this ability when picked up, but is not
 * removed from inventory on use. Extend this class and implement {@link #execute},
 * {@link #calculateCooldown}, {@link #canPerform}, and {@link #buildWorldItem}.
 */
public abstract class ActivatableAbility extends ActiveSkill implements AbilitySkill {

    /** Creates an {@code ActivatableAbility}. */
    protected ActivatableAbility() {}

    @Override
    public AbilityType abilityType() {
        return AbilityType.ACTIVATABLE;
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
