package btm.sword.system.entity.base;

import java.util.HashMap;

import btm.sword.config.ConfigManager;
import btm.sword.config.section.EntityConfig;
import btm.sword.system.attack.Attack;
import btm.sword.system.entity.aspect.AspectType;
import btm.sword.system.entity.aspect.value.AspectValue;
import btm.sword.system.entity.aspect.value.ResourceValue;
import btm.sword.system.playerdata.SwordClassType;
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
 * Upon construction, the profile loads baseline values for all {@link AspectType}s from configuration:
 * <ul>
 *   <li>{@link AspectType#SHARDS}: Loaded from {@code entities.combat_profile.shards} (default: 10/50/1.0)</li>
 *   <li>{@link AspectType#TOUGHNESS}: Loaded from {@code entities.combat_profile.toughness} (default: 20/20/0.5)</li>
 *   <li>{@link AspectType#SOULFIRE}: Loaded from {@code entities.combat_profile.soulfire} (default: 100/5/0.2)</li>
 *   <li>{@link AspectType#FORM}: Loaded from {@code entities.combat_profile.form} (default: 10/60/1.0)</li>
 *   <li>All other aspects: {@code new AspectValue(1)}</li>
 * </ul>
 *
 * <p>
 * These values define the regeneration behavior and maximums for each resource, and can be
 * tuned via {@code config.yaml} with hot-reload support. Values can also be overridden at
 * runtime using {@link #setStat(AspectType, AspectValue)}.
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

    private final Attack[] basicAttacks;

    private final Attack[] heavyAttacks;

    /**
     * The maximum number of consecutive air-dodges the entity can perform
     * before landing. Reset in {@link btm.sword.system.entity.types.Combatant#resetAirDashesPerformed()}.
     */
    private int maxAirDodges;

    /**
     * Constructs a new {@code CombatProfile} with the default {@link SwordClassType#SWORD_THROWER}
     * and baseline {@link AspectType} stat distributions loaded from configuration.
     */
    public CombatProfile() {
        swordClass = SwordClassType.SWORD_THROWER;

        // I think I'll keep this for a potential customization of the umbral blade, but it's not used anywhere right now
        basicAttacks = new Attack[]{
        };

        heavyAttacks = new Attack[]{

        };

        // Load combat profile values from config - direct field access (hybrid pattern)
        EntityConfig.CombatProfileConfig config = ConfigManager.getInstance().getEntities().getCombatProfile();

        for (AspectType stat : AspectType.values()) {
            switch (stat) {
                case SHARDS -> stats.put(stat, new ResourceValue(
                    config.getShardsCurrent(),
                    config.getShardsRegenPeriod(),
                    config.getShardsRegenAmount()
                ));
                case TOUGHNESS -> stats.put(stat, new ResourceValue(
                    config.getToughnessCurrent(),
                    config.getToughnessRegenPeriod(),
                    config.getToughnessRegenAmount()
                ));
                case SOULFIRE -> stats.put(stat, new ResourceValue(
                    config.getSoulfireCurrent(),
                    config.getSoulfireRegenPeriod(),
                    config.getSoulfireRegenAmount()
                ));
                case FORM -> stats.put(stat, new ResourceValue(
                    config.getFormCurrent(),
                    config.getFormRegenPeriod(),
                    config.getFormRegenAmount()
                ));
                default -> stats.put(stat, new AspectValue(1));
            }
        }

        this.maxAirDodges = config.getMaxAirDodges();
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
