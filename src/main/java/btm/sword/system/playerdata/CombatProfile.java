package btm.sword.system.playerdata;

import btm.sword.system.entity.aspect.AspectType;
import btm.sword.system.entity.aspect.value.AspectValue;
import btm.sword.system.entity.aspect.value.ResourceValue;

import java.util.HashMap;

public class CombatProfile {
	private SwordClassType swordClass;
	
	private final HashMap<AspectType, AspectValue> stats = new HashMap<>(); // max Stats
	
	private int maxAirDodges = 1;
	
	public CombatProfile() {
		swordClass = SwordClassType.LOSAH;
		
		for (AspectType stat : AspectType.values()) {
			switch (stat) {
				case SHARDS -> stats.put(stat, new ResourceValue(5, 40, 1));
				case TOUGHNESS -> stats.put(stat, new ResourceValue(20, 20, 1));
				case SOULFIRE -> stats.put(stat, new ResourceValue(50, 2, 0.2f));
				case FORM -> stats.put(stat, new ResourceValue(10, 60, 2));
				default -> stats.put(stat, new AspectValue(1));
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
	
	public void setStat(AspectType type, AspectValue values) {
		stats.put(type, values);
	}
	
	public AspectValue getStat(AspectType type) {
		return stats.get(type);
	}
	
	public void increaseNumAirDodges() {
		maxAirDodges++;
	}
	
	public int getMaxAirDodges() {
		return maxAirDodges;
	}
}
