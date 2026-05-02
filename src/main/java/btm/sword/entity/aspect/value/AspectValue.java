package btm.sword.entity.aspect.value;

/** Holds the raw numeric value for a single entity aspect. */
public class AspectValue {
    private float value;

    /** Constructs an aspect value with the given initial amount. */
    public AspectValue(float value) {
        this.value = value;
    }

    /** Returns the current numeric value of this aspect. */
    public float getValue() {
        return value;
    }

    /** Sets the current numeric value of this aspect. */
    public void setValue(float value) {
        this.value = value;
    }
}
