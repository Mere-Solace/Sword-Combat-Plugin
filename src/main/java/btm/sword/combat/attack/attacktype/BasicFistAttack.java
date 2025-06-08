package btm.sword.combat.attack.attacktype;

import btm.sword.combat.attack.Attack;
import btm.sword.combat.attack.AttackManager;
import btm.sword.combat.attack.AttackOptions;
import btm.sword.system.entity.SwordEntity;

public class BasicFistAttack extends Attack {
	
	public BasicFistAttack(AttackManager attackManager, AttackOptions options, SwordEntity executor) {
		super(attackManager, options, executor);
	}
	
	@Override
	public void onRun() {
		executor.getAssociatedEntity().sendMessage("Performed a Basic Fist Attack.");
	}
}
