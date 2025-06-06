package btm.sword.system;

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
	
	public CombatProfile() { }
	
	public CombatProfile(int shards, int toughness, int soulfire, int form, int might, int resolve, int finesse, int prowess, int armor, int fortitude, int celerity, int willpower) {
		this.shards = shards;
		this.toughness = toughness;
		this.soulfire = soulfire;
		this.form = form;
		this. might = might;
		this. resolve = resolve;
		this.finesse = finesse;
		this.prowess = prowess;
		this.armor = armor;
		this.fortitude = fortitude;
		this.celerity = celerity;
		this.willpower = willpower;
	}
}
