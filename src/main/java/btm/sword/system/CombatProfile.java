package btm.sword.system;

import btm.sword.combat.AttackTriggerType;
import btm.sword.combat.attack.Attack;
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
	
	HashMap<Material, HashMap<AttackTriggerType, Attack>> itemAttackMapPairing = new HashMap<>();
	
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
	
	public Attack getAttack(Material item, AttackTriggerType trigger) {
		return itemAttackMapPairing.get(item).get(trigger);
	}
	
	public void setAttack(Material item, AttackTriggerType trigger, Attack attack) {
		itemAttackMapPairing.putIfAbsent(item, new HashMap<>());
		itemAttackMapPairing.get(item).put(trigger, attack);
	}
}
