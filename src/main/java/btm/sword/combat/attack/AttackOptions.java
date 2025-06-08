package btm.sword.combat.attack;

import btm.sword.combat.attack.attacktype.AttackType;

public class AttackOptions {
	private AttackType attackType = AttackType.BASIC_FIST;
	double shieldPenetration = 0.0;
//	more as attacks and the combat system develop
	
	public AttackOptions() { }
	
	public AttackType getAttackType() {
		return attackType;
	}
	
	public AttackOptions attackType(AttackType attackType) {
		this.attackType = attackType;
		return this;
	}
}
