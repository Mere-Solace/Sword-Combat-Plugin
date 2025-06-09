package btm.sword.system;

import btm.sword.combat.attack.AttackOptions;
import btm.sword.combat.attack.AttackTriggerType;
import btm.sword.combat.attack.attacktype.AttackType;
import org.bukkit.Material;

import java.util.HashMap;

public class DefaultAttackMap {
	
	public static final HashMap<Material, HashMap<AttackTriggerType, AttackOptions>> basicAttackMapping;
	
	public static final HashMap<SwordClassType, HashMap<Material, HashMap<AttackTriggerType, AttackOptions>>> classAttackMapping;
	
	static {
		basicAttackMapping = new HashMap<>();
		for (Material item : Material.values()) {
			if (item.name().endsWith("AIR")) {
				basicAttackMapping.put(item, new HashMap<>());
				basicAttackMapping.get(item).put(AttackTriggerType.LEFT, new AttackOptions().attackType(AttackType.BASIC_FIST));
			}
			else if (item.name().endsWith("_SWORD")) {
				basicAttackMapping.put(item, new HashMap<>());
				basicAttackMapping.get(item).put(AttackTriggerType.LEFT, new AttackOptions().attackType(AttackType.BASIC_SWORD));
			}
			else if (item.name().endsWith("_AXE")) {
				basicAttackMapping.put(item, new HashMap<>());
				basicAttackMapping.get(item).put(AttackTriggerType.LEFT, new AttackOptions().attackType(AttackType.BASIC_AXE));
			}
		}
		
		classAttackMapping = new HashMap<>();
	}
	
	public static AttackOptions getBasicAttackOption(Material item, AttackTriggerType trigger) {
		HashMap<AttackTriggerType, AttackOptions> pair = basicAttackMapping.getOrDefault(item, null);
		if (pair == null) return null;
		
		return pair.getOrDefault(trigger, null);
	}
	
	public static AttackOptions getClassAttackOption(SwordClassType swordClass, Material item, AttackTriggerType trigger) {
		HashMap<Material, HashMap<AttackTriggerType, AttackOptions>> map = classAttackMapping.getOrDefault(swordClass, null);
		if (map == null) return null;
		
		HashMap<AttackTriggerType, AttackOptions> pair = map.getOrDefault(item, null);
		if (pair == null) return null;
		
		return pair.getOrDefault(trigger, null);
	}
}
