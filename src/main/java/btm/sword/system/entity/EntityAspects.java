package btm.sword.system.entity;

import btm.sword.system.entity.aspect.Aspect;
import btm.sword.system.entity.aspect.AspectType;
import btm.sword.system.entity.aspect.Resource;
import btm.sword.system.entity.aspect.value.AspectValue;
import btm.sword.system.entity.aspect.value.ResourceValue;
import btm.sword.system.playerdata.CombatProfile;

public class EntityAspects {
	protected Resource shards;
	protected Resource toughness;
	protected Resource soulfire;
	protected Resource form;
	
	protected Aspect might;
	protected Aspect resolve;
	protected Aspect finesse;
	protected Aspect prowess;
	protected Aspect armor;
	protected Aspect fortitude;
	protected Aspect celerity;
	protected Aspect willpower;
	
	public EntityAspects(CombatProfile profile) {
		AspectValue shardVals = profile.getStat(AspectType.SHARDS);
		shards = new Resource(
				AspectType.SHARDS,
				shardVals.getValue(),
				((ResourceValue) shardVals).getRegenPeriod(),
				((ResourceValue) shardVals).getRegenAmount());
		shards.startRegenTask();
		
		AspectValue toughnessVals = profile.getStat(AspectType.TOUGHNESS);
		toughness = new Resource(
				AspectType.TOUGHNESS,
				toughnessVals.getValue(),
				((ResourceValue) toughnessVals).getRegenPeriod(),
				((ResourceValue) toughnessVals).getRegenAmount());
		toughness.startRegenTask();
		
		AspectValue soulfireVals = profile.getStat(AspectType.SOULFIRE);
		soulfire = new Resource(
				AspectType.SOULFIRE,
				soulfireVals.getValue(),
				((ResourceValue) soulfireVals).getRegenPeriod(),
				((ResourceValue) soulfireVals).getRegenAmount());
		soulfire.startRegenTask();
		
		AspectValue formVals = profile.getStat(AspectType.FORM);
		form = new Resource(
				AspectType.FORM,
				formVals.getValue(),
				((ResourceValue) formVals).getRegenPeriod(),
				((ResourceValue) formVals).getRegenAmount());
		form.startRegenTask();
		
		might = new Aspect(AspectType.MIGHT, profile.getStat(AspectType.MIGHT).getValue());
		resolve = new Aspect(AspectType.RESOLVE, profile.getStat(AspectType.RESOLVE).getValue());
		finesse = new Aspect(AspectType.FINESSE, profile.getStat(AspectType.FINESSE).getValue());
		prowess = new Aspect(AspectType.PROWESS, profile.getStat(AspectType.PROWESS).getValue());
		armor = new Aspect(AspectType.ARMOR, profile.getStat(AspectType.ARMOR).getValue());
		fortitude = new Aspect(AspectType.FORTITUDE, profile.getStat(AspectType.FORTITUDE).getValue());
		celerity = new Aspect(AspectType.CELERITY, profile.getStat(AspectType.CELERITY).getValue());
		willpower = new Aspect(AspectType.WILLPOWER, profile.getStat(AspectType.WILLPOWER).getValue());
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
}
