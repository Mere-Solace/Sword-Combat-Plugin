package btm.sword.entity.aspect;

/**
 * Enumeration of all stat types tracked by the Sword combat system.
 * <p>
 * Types split into two categories:
 * <ul>
 *   <li><b>Resources</b> — dynamic pools that regenerate over time and are managed as
 *       {@link Resource} instances: {@link #SHARDS} (HP), {@link #TOUGHNESS} (shield),
 *       {@link #SOULFIRE} (mana), {@link #FORM} (experience).</li>
 *   <li><b>Aspects</b> — static modifiers stored as plain {@link Aspect} instances:
 *       {@link #MIGHT}, {@link #RESOLVE}, {@link #FINESSE}, {@link #PROWESS},
 *       {@link #ARMOR}, {@link #FORTITUDE}, {@link #CELERITY}, {@link #WILLPOWER}.</li>
 * </ul>
 * Base values for each type are defined per entity in {@link btm.sword.entity.base.CombatProfile}
 * and are managed at runtime through {@link btm.sword.entity.base.EntityAspects}.
 *
 * @see btm.sword.entity.aspect.Aspect
 * @see btm.sword.entity.aspect.Resource
 * @see btm.sword.entity.base.CombatProfile
 * @see btm.sword.entity.base.EntityAspects
 */
public enum AspectType {
    /** Hit points. Depleted when toughness is broken; reaching zero kills the entity. */
    SHARDS,

    /** Shield layer. Absorbs damage before shards are affected; regenerates over time. */
    TOUGHNESS,

    /** Mana-equivalent resource consumed by abilities and transferred on hit. */
    SOULFIRE,

    /** Experience-equivalent resource that regenerates passively. */
    FORM,

    /** Offensive power modifier — scales physical damage output. */
    MIGHT,

    /** Defensive resilience modifier — scales resistance to crowd control and status effects. */
    RESOLVE,

    /** Attack speed modifier — reduces attack cooldown and cast timing. */
    FINESSE,

    /** Utility/skill modifier — affects ability effectiveness and range. */
    PROWESS,

    /** Physical damage reduction modifier. */
    ARMOR,

    /** Endurance modifier — affects toughness-related thresholds. */
    FORTITUDE,

    /** Movement speed modifier — scales dash distance and base move speed. */
    CELERITY,

    /** Soulfire capacity and regeneration modifier. */
    WILLPOWER
}
