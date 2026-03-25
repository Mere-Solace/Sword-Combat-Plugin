package btm.sword.system.action.skill.type;

import java.util.Set;

import btm.sword.system.action.skill.AbilityType;
import btm.sword.system.action.skill.AbilityUseType;

/**
 * An ability that is thrown on a held right-click.
 *
 * <p>Each throw consumes one physical ability item from the player's inventory.
 * The player must have at least one such item to throw. Extend this class and implement
 * {@link #execute}, {@link #calculateCooldown}, {@link #canPerform}, and {@link #buildWorldItem}.
 */
public abstract class ThrowableAbility extends ActivatableAbility {

    @Override
    public AbilityType abilityType() {
        return AbilityType.THROWABLE;
    }

    /** Throwable abilities are always activated via hold, not tap. */
    @Override
    public boolean requiresHold() {
        return true;
    }

    @Override
    public boolean consumesOnUse() {
        return true;
    }

    @Override
    public boolean requiresPhysicalItemToUse() {
        return true;
    }

    /** Throwable abilities default to stack-based consumption. */
    @Override
    public Set<AbilityUseType> useTypes() {
        return Set.of(AbilityUseType.STACK);
    }
}
