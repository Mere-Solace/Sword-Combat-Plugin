package btm.sword.action.skill.type;

import btm.sword.action.skill.AbilityType;
import btm.sword.entity.base.Combatant;

/**
 * An ability that doubles as a quest turn-in item.
 *
 * <p>While held, the player can activate it like a normal {@link ActivatableAbility}.
 * The physical item is not consumed on use. However, it can be <em>surrendered</em>
 * via {@link #onSurrender(Combatant)}, which permanently removes it from the player's
 * possession and records it in their ability history.
 *
 * <p>This creates genuine stakes: surrendering completes the quest but forfeits the
 * ability's power forever.
 *
 * <p>Extend this class and implement {@link #execute}, {@link #calculateCooldown},
 * {@link #canPerform}, {@link #buildWorldItem}, and {@link #onSurrender}.
 */
public abstract class QuestAbility extends ActivatableAbility {

    @Override
    public AbilityType abilityType() {
        return AbilityType.QUEST;
    }

    /**
     * Called when the player surrenders this quest item to complete a quest.
     * Implement any quest-specific side effects here (e.g., advancing quest state,
     * spawning a reward, sending a message).
     *
     * <p>The physical item removal and history logging are handled externally by
     * {@code AbilitySurrenderUtil} — do not remove the item inside this method.
     *
     * @param combatant the player surrendering the item
     */
    public abstract void onSurrender(Combatant combatant);
}
