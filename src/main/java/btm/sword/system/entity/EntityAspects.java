package btm.sword.system.entity;

import btm.sword.system.entity.aspect.Aspect;
import btm.sword.system.entity.aspect.AspectType;
import btm.sword.system.entity.aspect.Resource;
import btm.sword.system.entity.aspect.value.AspectValue;
import btm.sword.system.entity.aspect.value.ResourceValue;
import btm.sword.system.playerdata.CombatProfile;

public class EntityAspects {
	private final Aspect[] stats = new Aspect[12];
	
	private final Resource shards;
	private final Resource toughness;
	private final Resource soulfire;
	private final Resource form;
	
	private final Aspect might;
	private final Aspect resolve;
	private final Aspect finesse;
	private final Aspect prowess;
	private final Aspect armor;
	private final Aspect fortitude;
	private final Aspect celerity;
	private final Aspect willpower;
	
	public EntityAspects(CombatProfile profile) {
		AspectValue shardVals = profile.getStat(AspectType.SHARDS);
		shards = new Resource(
				AspectType.SHARDS,
				shardVals.getValue(),
				((ResourceValue) shardVals).getRegenPeriod(),
				((ResourceValue) shardVals).getRegenAmount());
		shards.startRegenTask();
		stats[0] = shards;
		
		AspectValue toughnessVals = profile.getStat(AspectType.TOUGHNESS);
		toughness = new Resource(
				AspectType.TOUGHNESS,
				toughnessVals.getValue(),
				((ResourceValue) toughnessVals).getRegenPeriod(),
				((ResourceValue) toughnessVals).getRegenAmount());
		toughness.startRegenTask();
		stats[1] = toughness;
		
		AspectValue soulfireVals = profile.getStat(AspectType.SOULFIRE);
		soulfire = new Resource(
				AspectType.SOULFIRE,
				soulfireVals.getValue(),
				((ResourceValue) soulfireVals).getRegenPeriod(),
				((ResourceValue) soulfireVals).getRegenAmount());
		soulfire.startRegenTask();
		stats[2] = soulfire;
		
		AspectValue formVals = profile.getStat(AspectType.FORM);
		form = new Resource(
				AspectType.FORM,
				formVals.getValue(),
				((ResourceValue) formVals).getRegenPeriod(),
				((ResourceValue) formVals).getRegenAmount());
		form.startRegenTask();
		stats[3] = form;
		
		might = new Aspect(AspectType.MIGHT, profile.getStat(AspectType.MIGHT).getValue());
		stats[4] = might;
		resolve = new Aspect(AspectType.RESOLVE, profile.getStat(AspectType.RESOLVE).getValue());
		stats[5] = resolve;
		finesse = new Aspect(AspectType.FINESSE, profile.getStat(AspectType.FINESSE).getValue());
		stats[6] = finesse;
		prowess = new Aspect(AspectType.PROWESS, profile.getStat(AspectType.PROWESS).getValue());
		stats[7] = prowess;
		armor = new Aspect(AspectType.ARMOR, profile.getStat(AspectType.ARMOR).getValue());
		stats[8] = armor;
		fortitude = new Aspect(AspectType.FORTITUDE, profile.getStat(AspectType.FORTITUDE).getValue());
		stats[9] = fortitude;
		celerity = new Aspect(AspectType.CELERITY, profile.getStat(AspectType.CELERITY).getValue());
		stats[10] = celerity;
		willpower = new Aspect(AspectType.WILLPOWER, profile.getStat(AspectType.WILLPOWER).getValue());
		stats[11] = willpower;
	}
	
	public Aspect getAspect(AspectType type) {
		return switch (type) {
			case SHARDS -> shards;
			case TOUGHNESS -> toughness;
			case SOULFIRE -> soulfire;
			case FORM -> form;
			
			case MIGHT -> might;
			case RESOLVE -> resolve;
			case FINESSE -> finesse;
			case PROWESS -> prowess;
			case ARMOR -> armor;
			case FORTITUDE -> fortitude;
			case CELERITY -> celerity;
			case WILLPOWER -> willpower;
		};
	}
	
	public Aspect[] aspectSet() {
		return stats;
	}
	
	public float getAspectVal(AspectType type) {
		return getAspect(type).effectiveValue();
	}
	
	public Resource shards() { return shards; }
	public Resource toughness() { return toughness; }
	public Resource soulfire() { return soulfire; }
	public Resource form() { return form; }
	
	public Aspect might() { return might; }
	public Aspect resolve() { return resolve; }
	public Aspect finesse() { return finesse; }
	public Aspect prowess() { return prowess; }
	public Aspect armor() { return armor; }
	public Aspect fortitude() { return fortitude; }
	public Aspect celerity() { return celerity; }
	public Aspect willpower() { return willpower; }
	
	public float shardsVal() { return shards.effectiveValue(); }
	public float toughnessVal() { return toughness.effectiveValue(); }
	public float soulfireVal() { return soulfire.effectiveValue(); }
	public float formVal() { return form.effectiveValue(); }
	
	public float mightVal() { return might.effectiveValue(); }
	public float resolveVal() { return resolve.effectiveValue(); }
	public float finesseVal() { return finesse.effectiveValue(); }
	public float prowessVal() { return prowess.effectiveValue(); }
	public float armorVal() { return armor.effectiveValue(); }
	public float fortitudeVal() { return fortitude.effectiveValue(); }
	public float celerityVal() { return celerity.effectiveValue(); }
	public float willpowerVal() { return willpower.effectiveValue(); }
	
	public float shardsCur() { return shards.cur(); }
	public float toughnessCur() { return toughness.cur(); }
	public float soulfireCur() { return soulfire.cur(); }
	public float formCur() { return form.cur(); }
	
	public String curResources() {
		return "Shards: " + shardsCur() +
				"\nToughness: " + toughnessCur() +
				"\nSoulfire: " + soulfireCur() +
				"\nForm: " + formCur();
	}
}
