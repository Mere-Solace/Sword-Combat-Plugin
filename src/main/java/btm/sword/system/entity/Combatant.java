package btm.sword.system.entity;

import btm.sword.system.input.InputType;
import org.bukkit.Material;

import java.util.List;

public interface Combatant {
	
	void performAbility(Material itemType, List<InputType> trigger);
}
