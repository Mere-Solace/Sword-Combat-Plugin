package btm.sword.action.skill.type;

import java.util.Set;

import btm.sword.action.skill.AbilityType;
import btm.sword.action.skill.AbilityUseType;

/**
 * A one-shot ability that consumes its physical item on activation.
 *
 * <p>The player must have the physical ability item in their inventory to cast.
 * After casting, one item is removed. To use the ability again they must find
 * another item. Extend this class and implement {@link #execute},
 * {@link #calculateCooldown}, {@link #canPerform}, and {@link #buildWorldItem}.
 */
public abstract class ConsumableAbility extends ActivatableAbility {

    @Override
    public AbilityType abilityType() {
        return AbilityType.CONSUMABLE;
    }

    @Override
    public boolean consumesOnUse() {
        return true;
    }

    @Override
    public boolean requiresPhysicalItemToUse() {
        return true;
    }

    /** Consumable abilities default to stack-based consumption. */
    @Override
    public Set<AbilityUseType> useTypes() {
        return Set.of(AbilityUseType.STACK);
    }
}
