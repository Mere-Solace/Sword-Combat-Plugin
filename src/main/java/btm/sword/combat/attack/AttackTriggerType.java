package btm.sword.combat.attack;

public enum AttackTriggerType {
	LEFT,
	RIGHT,
	RIGHT_LEFT,
	DROP_DROP,
	DROP_SHIFT,
	DROP_LEFT,
	DROP_RIGHT_SHIFT,
	DROP_RIGHT_DROP,
	DROP_RIGHT_LEFT,
	DROP_RIGHT_RIGHT // always just drops the weapon. To drop a normal item, only 1 drop click is required
}