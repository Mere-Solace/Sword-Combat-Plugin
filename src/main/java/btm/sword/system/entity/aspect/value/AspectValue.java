package btm.sword.system.entity.aspect.value;

/**
 * A simple value container for aspect data in player profiles.
 * <p>
 * This class is used for serialization and deserialization of aspect values
 * when saving/loading player data. It provides a lightweight wrapper around
 * a single float value.
 * </p>
 *
 * @see btm.sword.system.entity.aspect.Aspect
 * @see btm.sword.system.playerdata.CombatProfile
 */
public class AspectValue {
    /** The aspect value. */
    private float value;

    /**
     * Constructs a new AspectValue with the specified value.
     *
     * @param value the aspect value
     */
    public AspectValue(float value) {
        this.value = value;
    }

    /**
     * Gets the aspect value.
     *
     * @return the value
     */
    public float getValue() {
        return value;
    }

    /**
     * Sets the aspect value.
     *
     * @param value the new value
     */
    public void setValue(float value) {
        this.value = value;
    }
}
