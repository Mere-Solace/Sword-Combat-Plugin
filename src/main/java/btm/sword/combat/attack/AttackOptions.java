package btm.sword.combat.attack;

import btm.sword.combat.attack.attacktype.AttackType;

public class AttackOptions {
	private AttackType attackType = AttackType.BASIC_FIST;
//	more as attacks and the combat system develop
	
	public AttackOptions() { }
	
	public AttackType getAttackType() {
		return attackType;
	}
	
	public AttackOptions attackType(AttackType attackType) {
		this.attackType = attackType;
		return this;
	}
	
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof AttackOptions)) return false;
		AttackOptions that = (AttackOptions) o;
		return attackType == that.attackType;
	}
}
