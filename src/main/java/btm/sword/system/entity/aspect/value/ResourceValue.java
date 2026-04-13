package btm.sword.system.entity.aspect.value;

import lombok.Getter;
import lombok.Setter;

/** Extends {@link AspectValue} with regeneration parameters used by resource-type aspects. */
@Getter
@Setter
public class ResourceValue extends AspectValue {
    /** The base period (in ticks) between regeneration events. */
    private int regenPeriod;
    private float regenAmount;

    /** Constructs a resource value with base value, tick-period between regeneration events, and amount per event. */
    public ResourceValue(float value, int regenPeriod, float regenAmount) {
        super(value);
        this.regenPeriod = regenPeriod;
        this.regenAmount = regenAmount;
    }
}
