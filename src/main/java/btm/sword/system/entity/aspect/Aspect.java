package btm.sword.system.entity.aspect;

import lombok.Getter;
import lombok.Setter;

/**
 * Represents a modifiable entity attribute with a base value and effectiveness modifier.
 * <p>
 * Aspects are used to model RPG-style stats such as damage, defense, speed, etc.
 * Each aspect has a base value and an effectiveness percentage that can be modified
 * by buffs, debuffs, or equipment.
 * </p>
 * <p>
 * The effective value is calculated as: {@code baseValue * effPercent}
 * </p>
 *
 * @see AspectType
 * @see Resource
 */
@Getter
@Setter
public class Aspect {
    /** The type of aspect this represents. */
    public final AspectType type;

    /** The base value before any modifiers are applied. */
    public float baseValue;

    /** The effectiveness percentage (0.0 to 1.0+) applied as a multiplier. */
    public float effPercent; // between 0 and 1

    /**
     * Constructs a new Aspect with the specified type and base value.
     * <p>
     * The effectiveness percentage is initialized to 1.0 (100%).
     * </p>
     *
     * @param type the aspect type
     * @param baseValue the base value before modifiers
     */
    public Aspect(AspectType type, float baseValue) {
        this.type = type;
        this.baseValue = baseValue;
        effPercent = 1f;
    }

    /**
     * Adds the specified amount to the base value.
     *
     * @param amount the amount to add (can be negative)
     */
    public void addBaseValue(float amount) {
        baseValue += amount;
    }

    /**
     * Calculates and returns the effective value after applying modifiers.
     *
     * @return the effective value ({@code baseValue * effPercent})
     */
    public float effectiveValue() {
        return baseValue * effPercent;
    }

    /**
     * Adds the specified percentage to the effectiveness modifier.
     * <p>
     * For example, adding 0.2 would increase effectiveness from 1.0 to 1.2 (120%).
     * Adding -0.5 would decrease effectiveness from 1.0 to 0.5 (50%).
     * </p>
     *
     * @param percent the percentage to add (can be negative)
     */
    public void addEffPercent(float percent) {
        effPercent += percent;
    }
}
