package btm.sword.system.entity.ai.ability;

/**
 * Categorizes a {@link MobAbility} by its combat range and movement pattern during the pre-attack wind-up.
 *
 * <p>The category determines how the mob moves during {@link btm.sword.system.entity.ai.state.PreAttackState}:
 * <ul>
 *   <li>{@link #MELEE} — mob closes in on the target at 110% speed during the wind-up.</li>
 *   <li>{@link #RANGED} — mob retreats from the target during the wind-up.</li>
 * </ul>
 */
public enum AbilityCategory {
    /** Mob approaches the target during pre-attack. Used for melee attacks. */
    MELEE,
    /** Mob retreats from the target during pre-attack. Used for ranged abilities. */
    RANGED
}
