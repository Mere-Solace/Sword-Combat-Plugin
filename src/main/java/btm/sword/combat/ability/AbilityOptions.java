package btm.sword.combat.ability;

import btm.sword.combat.ability.abilitytype.AbilityType;

public class AbilityOptions {
	private AbilityType type = AbilityType.NO_OP;
	private int delayTicks = 0;
	
	public AbilityOptions type(AbilityType type) {
		this.type = type;
		return this;
	}
	
	public AbilityType getType() {
		return type;
	}
	
	public AbilityOptions delayTicks(int delayTicks) {
		this.delayTicks = delayTicks;
		return this;
	}
	
	public int getDelayTicks() {
		return delayTicks;
	}
	
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof AbilityOptions that)) return false;
		return      type == that.type
				&&  delayTicks == that.delayTicks;
	}
}
