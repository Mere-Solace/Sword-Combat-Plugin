package btm.sword.system.entity.aspect.value;

public class ResourceValue extends AspectValue {
	private int regenPeriod;
	private float regenAmount;
	
	public ResourceValue(float value, int regenPeriod, float regenAmount) {
		super(value);
		this.regenPeriod = regenPeriod;
		this.regenAmount = regenAmount;
	}
	
	public int getRegenPeriod() {
		return regenPeriod;
	}
	
	public void setRegenPeriod(int regenPeriod) {
		this.regenPeriod = regenPeriod;
	}
	
	public float getRegenAmount() {
		return regenAmount;
	}
	
	public void setRegenAmount(float regenAmount) {
		this.regenAmount = regenAmount;
	}
}
