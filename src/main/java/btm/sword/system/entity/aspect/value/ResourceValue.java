package btm.sword.system.entity.aspect.value;

/**
 * A value container for resource data in player profiles.
 * <p>
 * Extends {@link AspectValue} by adding regeneration parameters. This class
 * is used for serialization and deserialization of resource values when
 * saving/loading player data.
 * </p>
 *
 * @see AspectValue
 * @see btm.sword.system.entity.aspect.Resource
 * @see btm.sword.system.playerdata.CombatProfile
 */
public class ResourceValue extends AspectValue {
    /** The regeneration period in ticks. */
    private int regenPeriod;

    /** The amount regenerated per period. */
    private float regenAmount;

    /**
     * Constructs a new ResourceValue with the specified parameters.
     *
     * @param value the resource value
     * @param regenPeriod the regeneration period in ticks
     * @param regenAmount the amount regenerated per period
     */
    public ResourceValue(float value, int regenPeriod, float regenAmount) {
        super(value);
        this.regenPeriod = regenPeriod;
        this.regenAmount = regenAmount;
    }

    /**
     * Gets the regeneration period.
     *
     * @return the regeneration period in ticks
     */
    public int getRegenPeriod() {
        return regenPeriod;
    }

    /**
     * Sets the regeneration period.
     *
     * @param regenPeriod the new regeneration period in ticks
     */
    public void setRegenPeriod(int regenPeriod) {
        this.regenPeriod = regenPeriod;
    }

    /**
     * Gets the regeneration amount.
     *
     * @return the amount regenerated per period
     */
    public float getRegenAmount() {
        return regenAmount;
    }

    /**
     * Sets the regeneration amount.
     *
     * @param regenAmount the new amount regenerated per period
     */
    public void setRegenAmount(float regenAmount) {
        this.regenAmount = regenAmount;
    }
}
