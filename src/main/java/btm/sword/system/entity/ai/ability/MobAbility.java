package btm.sword.system.entity.ai.ability;

import btm.sword.system.entity.impl.Hostile;

/**
 * Represents a discrete combat ability available to a {@link Hostile} mob.
 *
 * <p>Abilities are selected at the start of {@link btm.sword.system.entity.ai.state.PreAttackState}
 * and executed at the start of {@link btm.sword.system.entity.ai.state.AttackState} — immediately
 * when the wind-up timer expires, with no proximity gate.
 */
public interface MobAbility {

    /**
     * Returns the unique name of this ability, used as the cooldown map key.
     *
     * @return the ability name
     */
    String name();

    /**
     * Returns the {@link AbilityCategory} of this ability, which determines mob movement during
     * the wind-up.
     *
     * @return the ability category
     */
    AbilityCategory category();

    /**
     * Returns {@code true} if this ability is currently usable by the given mob —
     * i.e., the cooldown has elapsed and any preconditions are met.
     *
     * @param h the {@link Hostile} attempting to use the ability
     * @return {@code true} if the ability can be selected
     */
    boolean canUse(Hostile h);

    /**
     * Executes this ability for the given mob.
     * Called at the start of {@link btm.sword.system.entity.ai.state.AttackState}.
     *
     * @param h the {@link Hostile} executing the ability
     */
    void execute(Hostile h);

    /**
     * Returns the cooldown duration in ticks after this ability is used.
     *
     * @return cooldown in ticks
     */
    int cooldownTicks();
}
