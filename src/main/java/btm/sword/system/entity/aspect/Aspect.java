package btm.sword.system.entity.aspect;

import lombok.Getter;
import lombok.Setter;

/**
 * Represents a fundamental quantitative trait or stat associated with an entity,
 * such as health, stamina, or defense.
 * <p>
 * Each {@code Aspect} defines a {@link AspectType}, a {@code baseValue}, and an
 * {@code effPercent} modifier. The modifier determines how the effective value
 * scales relative to the base. For example, a 1.2 multiplier represents a 20%
 * boost, while 0.8 represents a 20% reduction.
 * </p>
 *
 * <p>
 * This class provides basic value manipulation and scaling utilities used by
 * higher-level systems like {@link Resource}.
 * </p>
 */
@Getter
@Setter
public class Aspect {
    /** The category or role of this aspect (e.g., HEALTH, STAMINA, DEFENSE). */
    public final AspectType type;

    /** The intrinsic, unmodified value of this aspect. */
    public float baseValue;

    /**
     * The effective multiplier applied to {@link #baseValue}.
     * <p>
     * This typically ranges between 0 and 1 for penalties or above 1 for bonuses.
     * The effective value is computed as {@code baseValue * effPercent}.
     * </p>
     */
    public float effPercent;

    /**
     * Constructs a new aspect with the given type and base value.
     *
     * @param type       the aspect category
     * @param baseValue  the unmodified base value
     */
    public Aspect(AspectType type, float baseValue) {
        this.type = type;
        this.baseValue = baseValue;
        effPercent = 1f;
    }

    /**
     * Increases the base value of this aspect by the specified amount.
     * <p>
     * This permanently raises the unmodified base level of the aspect,
     * regardless of current multipliers.
     * </p>
     *
     * @param amount the value to add to the base
     */
    public void addBaseValue(float amount) {
        baseValue += amount;
    }

    /**
     * Calculates the current effective value of this aspect.
     * <p>
     * This represents the operational value after applying all modifiers:
     * {@code baseValue * effPercent}.
     * </p>
     *
     * @return the effective aspect value
     */
    public float effectiveValue() {
        return baseValue * effPercent;
    }

    /**
     * Adjusts the effective percent modifier by the specified amount.
     * <p>
     * For example, adding {@code 0.1} increases effectiveness by 10%.
     * </p>
     *
     * @param percent the modifier delta to apply
     */
    public void addEffPercent(float percent) {
        effPercent += percent;
    }
}
