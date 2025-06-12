package btm.sword.system.playerdata;

public enum StatType {
	SHARDS,
	TOUGHNESS,
	SOULFIRE,
	FORM,
	MIGHT,
	RESOLVE,
	FINESSE,
	PROWESS,
	ARMOR,
	FORTITUDE,
	CELERITY,
	WILLPOWER;
	
	public int mapToBinary(StatType type) {
		int bits;
		switch (type) {
			case SHARDS -> bits = 0b0000;
			case TOUGHNESS -> bits = 0b0001;
			case SOULFIRE -> bits = 0b0010;
			default -> bits = 0b10000;
		}
		return bits;
	}
}
