package btm.sword.system;

import btm.sword.combat.attack.AttackOptions;
import btm.sword.system.input.InputType;
import btm.sword.combat.attack.attacktype.AttackType;
import org.bukkit.Material;

import java.util.*;

public class DefaultAttackMap {
	
	public static final HashMap<Material, HashMap<List<InputType>, AttackOptions>> basicAttackMapping;
	
	public static final HashMap<SwordClassType, HashMap<Material, HashMap<List<InputType>, AttackOptions>>> classAttackMapping;
	
	static {
		basicAttackMapping = new HashMap<>();
		for (Material item : Material.values()) {
			if (item.name().endsWith("AIR")) {
				basicAttackMapping.put(item, new HashMap<>());
				basicAttackMapping.get(item).put(List.of(InputType.LEFT),  new AttackOptions().attackType(AttackType.BASIC_FIST));
			}
			else if (item.name().endsWith("_SWORD")) {
				basicAttackMapping.put(item, new HashMap<>());
				basicAttackMapping.get(item).put(List.of(InputType.LEFT), new AttackOptions().attackType(AttackType.BASIC_SWORD));
			}
			else if (item.name().endsWith("_AXE")) {
				basicAttackMapping.put(item, new HashMap<>());
				basicAttackMapping.get(item).put(List.of(InputType.LEFT), new AttackOptions().attackType(AttackType.BASIC_AXE));
			}
		}
		
		classAttackMapping = new HashMap<>();
	}
	
	public static AttackOptions getBasicAttackOption(Material item, List<InputType> trigger) {
		HashMap<List<InputType>, AttackOptions> inputAttackPair = basicAttackMapping.getOrDefault(item, null);
		if (inputAttackPair == null) return null;
		
		return inputAttackPair.getOrDefault(trigger, null);
	}
	
	public static AttackOptions getClassAttackOption(SwordClassType swordClass, Material item, List<InputType> trigger) {
		HashMap<Material, HashMap<List<InputType>, AttackOptions>> itemMapPair = classAttackMapping.getOrDefault(swordClass, null);
		if (itemMapPair == null) return null;
		
		HashMap<List<InputType>, AttackOptions> inputAttackPair = itemMapPair.getOrDefault(item, null);
		if (inputAttackPair == null) return null;
		
		return inputAttackPair.getOrDefault(trigger, null);
	}
}
