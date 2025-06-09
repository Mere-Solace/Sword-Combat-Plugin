package btm.sword.system.entity;

import btm.sword.combat.attack.AttackTriggerType;
import org.bukkit.Material;

public interface Combatant {
	
	void performAbility(Material itemType, AttackTriggerType trigger);
}
