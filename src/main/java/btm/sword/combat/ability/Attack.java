package btm.sword.combat.ability;

import btm.sword.system.entity.SwordEntity;

public abstract class Attack extends Ability {
	public Attack(AbilityOptions options, SwordEntity executor) {
		super(options, executor);
	}
}
