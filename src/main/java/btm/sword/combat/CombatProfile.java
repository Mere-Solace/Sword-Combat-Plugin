package btm.sword.combat;

import btm.sword.combat.attack.Attack;
import btm.sword.combat.attack.AttackFactory;
import btm.sword.combat.attack.AttackTriggerType;
import btm.sword.combat.attack.AttackOptions;
import btm.sword.combat.attack.attacktype.AttackType;
import btm.sword.system.StatType;
import btm.sword.system.entity.SwordEntity;
import org.bukkit.Material;

import java.util.HashMap;

public class CombatProfile {
	int shards = 5;
	int toughness = 20;
	int soulfire = 50;
	int form = 1;
	int might = 1;
	int resolve = 1;
	int finesse = 1;
	int prowess = 1;
	int armor = 1;
	int fortitude = 1;
	int celerity = 1;
	int willpower = 1;
	
	HashMap<StatType, Integer> stats = new HashMap<>();
	HashMap<Material, HashMap<AttackTriggerType, AttackOptions>> itemAttackMapPairing = new HashMap<>();
	
	public CombatProfile() {
		for (StatType stat : StatType.values()) {
			stats.put(stat, 1);
		}
		// replace item material type with a specific item metadata in the future
		for (Material itemType : Material.values()) {
			if (itemType.name().endsWith("AIR")) {
				setAttack(itemType, AttackTriggerType.LEFT, new AttackOptions().attackType(AttackType.BASIC_FIST));
			}
			else if (itemType.name().endsWith("_SWORD")) {
				setAttack(itemType, AttackTriggerType.LEFT, new AttackOptions().attackType(AttackType.BASIC_SWORD));
			}
			else if (itemType.name().endsWith("_AXE")) {
				setAttack(itemType, AttackTriggerType.LEFT, new AttackOptions().attackType(AttackType.BASIC_AXE));
			}
		}
	}
	
	public void setStat(StatType type, int value) {
		stats.put(type, value);
	}
	
	public void addStat(StatType type, int delta) {
		stats.putIfAbsent(type, 0);
		stats.replace(type, getStat(type) + delta);
	}
	
	public int getStat(StatType type) {
		return stats.getOrDefault(type, -1);
	}
	
	public Attack getAttack(Material item, AttackTriggerType trigger, SwordEntity executor) {
		AttackOptions options = itemAttackMapPairing.get(item).get(trigger);
		if (options == null) return null;
		return AttackFactory.create(options, executor);
	}
	
	public void setAttack(Material item, AttackTriggerType trigger, AttackOptions attackOptions) {
		itemAttackMapPairing.putIfAbsent(item, new HashMap<>());
		itemAttackMapPairing.get(item).put(trigger, attackOptions);
	}
}
