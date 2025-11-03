package btm.sword.system.entity.base;

import btm.sword.system.entity.aspect.AspectType;
import btm.sword.system.entity.aspect.value.AspectValue;
import btm.sword.system.entity.aspect.value.ResourceValue;
import btm.sword.system.playerdata.SwordClassType;
import java.util.HashMap;
import lombok.Getter;

/**
 * Represents the combat-related statistical profile for a {@link btm.sword.system.entity.base.SwordEntity}.
 * <p>
 * Each {@code CombatProfile} defines a unique set of {@link AspectType aspect values}
 * that govern core combat resources such as shards, toughness, and soulfire.
 * These stats are stored in a {@link HashMap} mapping {@link AspectType} keys to
 * {@link AspectValue} or {@link ResourceValue} instances.
 * </p>
 *
 * <p>
 * The profile also tracks additional combat-related parameters such as
 * air-dodge limits, and it provides utility methods to modify or retrieve
 * individual {@link AspectValue}s.
 * </p>
 *
 * <h2>Usage</h2>
 * A {@code CombatProfile} is created and owned by each {@link btm.sword.system.entity.base.SwordEntity},
 * where it is used through {@link btm.sword.system.entity.base.EntityAspects} to drive resource updates
 * and regeneration over time.
 * </p>
 *
 * <h3>Examples of Use:</h3>
 * <ul>
 *   <li>{@link btm.sword.system.entity.base.SwordEntity#resetResources()} resets stats via this profile.</li>
 *   <li>{@link btm.sword.system.entity.types.Combatant} references its {@code CombatProfile} to determine
 *       resource thresholds for mechanics such as dashing, blocking, and casting.</li>
 *   <li>{@link btm.sword.system.combat.Affliction} subclasses may query stats such as
 *       {@link AspectType#TOUGHNESS} to calculate their strength or duration.</li>
 * </ul>
 *
 * <h2>Default Initialization</h2>
 * Upon construction, the profile assigns baseline values for all {@link AspectType}s:
 * <ul>
 *   <li>{@link AspectType#SHARDS}: {@code new ResourceValue(5, 40, 1)}</li>
 *   <li>{@link AspectType#TOUGHNESS}: {@code new ResourceValue(100, 20, 0.5f)}</li>
 *   <li>{@link AspectType#SOULFIRE}: {@code new ResourceValue(50, 2, 0.2f)}</li>
 *   <li>{@link AspectType#FORM}: {@code new ResourceValue(10, 60, 1)}</li>
 *   <li>All other aspects: {@code new AspectValue(1)}</li>
 * </ul>
 *
 * <p>
 * These defaults define the regeneration behavior and maximums for each resource.
 * Values can later be overridden using {@link #setStat(AspectType, AspectValue)}.
 * </p>
 *
 * @see btm.sword.system.entity.base.SwordEntity
 * @see btm.sword.system.entity.base.EntityAspects
 * @see AspectType
 * @see AspectValue
 * @see ResourceValue
 */
@Getter
public class CombatProfile {
    /**
     * Defines the combat class specialization this profile belongs to,
     * e.g., {@link SwordClassType#SWORD_THROWER}.
     */
    @lombok.Setter
    private SwordClassType swordClass;

    /**
     * The map storing all combat-related stats for this entity.
     * Keys correspond to {@link AspectType}, values to {@link AspectValue} or its subclass {@link ResourceValue}.
     * <p>
     * These represent the entity's “base” stats — they are wrapped and dynamically
     * adjusted in-game through {@link btm.sword.system.entity.base.EntityAspects}.
     * </p>
     */
    private final HashMap<AspectType, AspectValue> stats = new HashMap<>(); // max Stats

    /**
     * The maximum number of consecutive air-dodges the entity can perform
     * before landing. Reset in {@link btm.sword.system.entity.types.Combatant#resetAirDashesPerformed()}.
     */
    private int maxAirDodges = 1;

    /**
     * Constructs a new {@code CombatProfile} with the default {@link SwordClassType#SWORD_THROWER}
     * and baseline {@link AspectType} stat distributions.
     */
    public CombatProfile() {
        swordClass = SwordClassType.SWORD_THROWER;

        for (AspectType stat : AspectType.values()) {
            switch (stat) {
                case SHARDS -> stats.put(stat, new ResourceValue(30, 30, 1));
                case TOUGHNESS -> stats.put(stat, new ResourceValue(30, 20, 0.5f));
                case SOULFIRE -> stats.put(stat, new ResourceValue(100, 1, 0.2f));
                case FORM -> stats.put(stat, new ResourceValue(10, 60, 1));
                default -> stats.put(stat, new AspectValue(1));
            }
        }
    }

    /**
     * Assigns or replaces the {@link AspectValue} for a given {@link AspectType}.
     * <p>
     * Typically invoked by systems that recalculate derived stats or
     * apply upgrades, such as when equipping a modifier or leveling up.
     * </p>
     *
     * @param type   the {@link AspectType} to modify
     * @param values the new {@link AspectValue} or {@link ResourceValue} to assign
     */
    public void setStat(AspectType type, AspectValue values) {
        stats.put(type, values);
    }

    /**
     * Retrieves the {@link AspectValue} associated with the given {@link AspectType}.
     *
     * @param type the aspect type to fetch
     * @return the current {@link AspectValue}, or {@code null} if none is defined
     */
    public AspectValue getStat(AspectType type) {
        return stats.get(type);
    }

    /**
     * Increases the maximum number of air dodges this profile supports.
     * Used by ability upgrades and temporary effects.
     *
     * @see btm.sword.system.entity.types.Combatant#resetAirDashesPerformed()
     */
    public void increaseNumAirDodges() {
        maxAirDodges++;
    }
}
