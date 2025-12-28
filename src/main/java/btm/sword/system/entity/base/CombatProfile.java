package btm.sword.system.entity.base;

import java.util.ArrayList;
import java.util.HashMap;

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
 *   <li>{@link btm.sword.system.entity.impl.Combatant} references its {@code CombatProfile} to determine
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

    // Persisted as integers for compact storage. -1 indicates empty slot.
    private final ArrayList<Integer> unlockedSkillIds = new ArrayList<>();
    private final ArrayList<Integer> equippedSkillIds = new ArrayList<>();

    // Number of skill slots unlocked for this profile (default 1, max 3)
    private int unlockedSkillSlots = 1;

    private static final int MAX_SKILL_SLOTS = 3;

    // Ensure equippedSkillIds has fixed size initialized with -1
    {
        for (int i = 0; i < MAX_SKILL_SLOTS; i++) equippedSkillIds.add(-1);
    }

    /**
     * The maximum number of consecutive air-dodges the entity can perform
     * before landing. Reset in {@link btm.sword.system.entity.impl.Combatant#resetAirDashesPerformed()}.
     */
    private int maxAirDodges;

    /**
     * Constructs a new {@code CombatProfile} with the default {@link SwordClassType#SWORD_THROWER}
     * and baseline {@link AspectType} stat distributions loaded from configuration.
     */
    public CombatProfile() {
        swordClass = SwordClassType.SWORD_THROWER;

        // Load combat profile values from config - static field access
        for (AspectType stat : AspectType.values()) {
            switch (stat) {
                case SHARDS -> stats.put(stat, new ResourceValue(
                    btm.sword.config.Config.Entity.COMBAT_PROFILE_SHARDS_CURRENT,
                    btm.sword.config.Config.Entity.COMBAT_PROFILE_SHARDS_REGEN_PERIOD,
                    btm.sword.config.Config.Entity.COMBAT_PROFILE_SHARDS_REGEN_AMOUNT
                ));
                case TOUGHNESS -> stats.put(stat, new ResourceValue(
                    btm.sword.config.Config.Entity.COMBAT_PROFILE_TOUGHNESS_CURRENT,
                    btm.sword.config.Config.Entity.COMBAT_PROFILE_TOUGHNESS_REGEN_PERIOD,
                    btm.sword.config.Config.Entity.COMBAT_PROFILE_TOUGHNESS_REGEN_AMOUNT
                ));
                case SOULFIRE -> stats.put(stat, new ResourceValue(
                    btm.sword.config.Config.Entity.COMBAT_PROFILE_SOULFIRE_CURRENT,
                    btm.sword.config.Config.Entity.COMBAT_PROFILE_SOULFIRE_REGEN_PERIOD,
                    btm.sword.config.Config.Entity.COMBAT_PROFILE_SOULFIRE_REGEN_AMOUNT
                ));
                case FORM -> stats.put(stat, new ResourceValue(
                    btm.sword.config.Config.Entity.COMBAT_PROFILE_FORM_CURRENT,
                    btm.sword.config.Config.Entity.COMBAT_PROFILE_FORM_REGEN_PERIOD,
                    btm.sword.config.Config.Entity.COMBAT_PROFILE_FORM_REGEN_AMOUNT
                ));
                default -> stats.put(stat, new AspectValue(1));
            }
        }

        this.maxAirDodges = btm.sword.config.Config.Entity.COMBAT_PROFILE_MAX_AIR_DODGES;
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
     * @see btm.sword.system.entity.impl.Combatant#resetAirDashesPerformed()
     */
    public void increaseNumAirDodges() {
        maxAirDodges++;
    }
}
