package btm.sword.combat.ability.abilitytype;

import btm.sword.combat.ability.Ability;
import btm.sword.combat.ability.AbilityOptions;
import btm.sword.system.entity.SwordEntity;

public class NoopAbility extends Ability {
	
	public NoopAbility(AbilityOptions options, SwordEntity executor) {
		super(options, executor);
	}
	
	@Override
	public void onRun() {
		executor.getAssociatedEntity().sendMessage("NO-OP!");
	}
}
