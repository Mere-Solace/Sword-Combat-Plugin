package btm.sword.system.action;

import btm.sword.system.entity.Combatant;

public abstract class SwordAction {
	
	protected static void endTask(Combatant executor) {
		executor.setAbilityTask(null);
	}
}
