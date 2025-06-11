package btm.sword.system;

import btm.sword.combat.ability.AbilityOptions;
import btm.sword.combat.ability.abilitytype.AbilityType;
import btm.sword.system.input.InputType;
import org.bukkit.Material;

import java.util.*;

public class DefaultAttackMap {
	
	public static final HashMap<Material, HashMap<List<InputType>, AbilityOptions>> basicAttackMapping;
	
	public static final HashMap<SwordClassType, HashMap<Material, HashMap<List<InputType>, AbilityOptions>>> classAttackMapping;
	
	static {
		basicAttackMapping = new HashMap<>();
		for (Material item : Material.values()) {
			if (item.name().endsWith("AIR")) {
				basicAttackMapping.put(item, new HashMap<>());
				basicAttackMapping.get(item).put(List.of(InputType.LEFT),  new AbilityOptions().type(AbilityType.BASIC_FIST_1));
			}
			else if (item.name().endsWith("_SWORD")) {
				basicAttackMapping.put(item, new HashMap<>());
				basicAttackMapping.get(item).put(List.of(InputType.LEFT), new AbilityOptions().type(AbilityType.BASIC_SWORD_1));
			}
			else if (item.name().endsWith("_AXE")) {
				basicAttackMapping.put(item, new HashMap<>());
				basicAttackMapping.get(item).put(List.of(InputType.LEFT), new AbilityOptions().type(AbilityType.BASIC_AXE_1));
			}
		}
		
		classAttackMapping = new HashMap<>();
	}
	
	public static AbilityOptions getBasicAttackOption(Material item, List<InputType> trigger) {
		HashMap<List<InputType>, AbilityOptions> inputAttackPair = basicAttackMapping.getOrDefault(item, null);
		if (inputAttackPair == null) return null;
		
		return inputAttackPair.getOrDefault(trigger, null);
	}
	
	public static AbilityOptions getClassAttackOption(SwordClassType swordClass, Material item, List<InputType> trigger) {
		HashMap<Material, HashMap<List<InputType>, AbilityOptions>> itemMapPair = classAttackMapping.getOrDefault(swordClass, null);
		if (itemMapPair == null) return null;
		
		HashMap<List<InputType>, AbilityOptions> inputAttackPair = itemMapPair.getOrDefault(item, null);
		if (inputAttackPair == null) return null;
		
		return inputAttackPair.getOrDefault(trigger, null);
	}
}
