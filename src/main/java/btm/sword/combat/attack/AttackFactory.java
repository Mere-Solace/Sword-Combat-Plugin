package btm.sword.combat.attack;

import btm.sword.Sword;
import btm.sword.combat.attack.attacktype.BasicAxeAttack;
import btm.sword.combat.attack.attacktype.BasicFistAttack;
import btm.sword.combat.attack.attacktype.BasicSwordAttack;
import btm.sword.system.entity.SwordEntity;

public class AttackFactory {
	private static final AttackManager attackManager = Sword.getAttackManager();
	
	public static Attack create(AttackOptions options, SwordEntity executor) {
		switch (options.getAttackType()) {
			case BASIC_SWORD -> { return  new BasicSwordAttack(attackManager, options, executor); }
			case BASIC_AXE -> { return new BasicAxeAttack(attackManager, options, executor); }
			
			default -> { return  new BasicFistAttack(attackManager, options, executor); }
		}
	}
}
