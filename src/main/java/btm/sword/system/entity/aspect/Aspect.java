package btm.sword.system.entity.aspect;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Aspect {
    public final AspectType type;
    public float baseValue;
    public float effPercent; // between 0 and 1

    public Aspect(AspectType type, float baseValue) {
        this.type = type;
        this.baseValue = baseValue;
        effPercent = 1f;
    }

    public void addBaseValue(float amount) {
        baseValue += amount;
    }

    public float effectiveValue() {
        return baseValue * effPercent;
    }

    public void addEffPercent(float percent) {
        effPercent += percent;
    }
}
