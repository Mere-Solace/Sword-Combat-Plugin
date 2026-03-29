package btm.sword.system.action.skill.type;

/**
 * An active skill that is consumed on use (e.g. has a finite number of uses, repair charges, etc.).
 *
 * <p>Extend this class for skills where the world item is depleted or expended through
 * repeated activations, as opposed to {@link ActivatableAbility} which never consumes
 * the backing item.</p>
 */
public abstract class ConsumableActive extends ActiveSkill {
    // uses left, repair uses, etc.
}
