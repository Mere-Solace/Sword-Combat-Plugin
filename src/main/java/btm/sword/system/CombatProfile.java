package btm.sword.system;

import btm.sword.combat.attack.Attack;
import btm.sword.combat.attack.AttackFactory;
import btm.sword.system.input.InputType;
import btm.sword.combat.attack.AttackOptions;
import btm.sword.system.entity.SwordEntity;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class CombatProfile {
	SwordClassType swordClass;
	
	HashMap<StatType, Integer> stats = new HashMap<>();
	HashMap<Material, HashMap<List<InputType>, AttackOptions>> specialAttackTriggerMap = new HashMap<>();
	
	public CombatProfile() {
		swordClass = SwordClassType.LOSAH;
		
		for (StatType stat : StatType.values()) {
			switch (stat) {
				case SHARDS -> stats.put(stat, 5);
				case TOUGHNESS -> stats.put(stat, 20);
				case SOULFIRE -> stats.put(stat, 50);
				case FORM, MIGHT, RESOLVE, FINESSE, PROWESS, ARMOR, FORTITUDE, CELERITY, WILLPOWER -> stats.put(stat, 1);
			}
		}
		// replace item material type with a specific item metadata in the future
	}
	
	public SwordClassType getSwordClass() {
		return swordClass;
	}
	
	public void setSwordClass(SwordClassType swordClass) {
		this.swordClass = swordClass;
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
	
	public List<List<InputType>> getAllBoundSequences() {
		List<List<InputType>> boundInputSequences = new ArrayList<>();
		specialAttackTriggerMap.forEach((item, inputAttackPair) -> {
			inputAttackPair.forEach((inputSequence, attackOption) -> {
				if (!boundInputSequences.contains(inputSequence))
					boundInputSequences.add(List.copyOf(inputSequence));
			});
		});
		
		return  boundInputSequences;
	}
	
	public Attack getAttack(Material item, List<InputType> trigger, SwordEntity executor) {
		HashMap<List<InputType>, AttackOptions> pair = specialAttackTriggerMap.getOrDefault(item, null);
		AttackOptions options;
		if (pair == null) {
			options = DefaultAttackMap.getClassAttackOption(swordClass, item, trigger);
			if (options == null)
				options = DefaultAttackMap.getBasicAttackOption(item, trigger);
		}
		else {
			options = pair.get(trigger);
		}
		if (options == null) return null;
		
		return AttackFactory.create(options, executor);
	}
	
	public void setAttack(Material item, List<InputType> trigger, AttackOptions attackOptions) {
		if (Objects.equals(DefaultAttackMap.getBasicAttackOption(item, trigger), attackOptions)) return;
		
		specialAttackTriggerMap.putIfAbsent(item, new HashMap<>());
		specialAttackTriggerMap.get(item).put(trigger, attackOptions);
	}
}
