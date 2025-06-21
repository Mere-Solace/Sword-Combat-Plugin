package btm.sword.system.playerdata;

import java.util.HashMap;

public class CombatProfile {
	private SwordClassType swordClass;
	
	private final HashMap<StatType, Integer> stats = new HashMap<>();
	
	private int maxAirDodges = 1;
	
	public CombatProfile() {
		swordClass = SwordClassType.LOSAH;
		
		for (StatType stat : StatType.values()) {
			switch (stat) {
				case SHARDS -> stats.put(stat, 5);
				case TOUGHNESS -> stats.put(stat, 20);
				case SOULFIRE -> stats.put(stat, 50);
				default -> stats.put(stat, 1);
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
	
	public void increaseNumAirDodges() {
		maxAirDodges++;
	}
	
	public int getMaxAirDodges() {
		return maxAirDodges;
	}
}
