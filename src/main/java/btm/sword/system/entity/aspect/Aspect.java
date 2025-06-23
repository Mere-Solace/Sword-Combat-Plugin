package btm.sword.system.entity.aspect;

public class Aspect {
	public final AspectType type;
	public float baseValue;
	public float effPercent; // between 0 and 1
	
	public Aspect(AspectType type, float baseValue) {
		this.type = type;
		this.baseValue = baseValue;
		effPercent = 1f;
	}
	
	public AspectType getType() {
		return type;
	}
	
	public float getBaseValue() {
		return baseValue;
	}
	
	public void setBaseValue(float baseValue) {
		this.baseValue = baseValue;
	}
	
	public void addBaseValue(float amount) {
		baseValue += amount;
	}
	
	public float effectiveValue() {
		return baseValue * effPercent;
	}
	
	public void setEffPercent(float effPercent) {
		this.effPercent = effPercent;
	}
	
	public void addEffPercent(float percent) {
		effPercent += percent;
	}
}
