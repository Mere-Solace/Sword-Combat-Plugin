package btm.sword.combat.ability;

import btm.sword.combat.ability.abilitytype.BasicFist1Attack;
import btm.sword.combat.ability.abilitytype.BasicSword1Attack;
import btm.sword.combat.ability.abilitytype.DashAbility;
import btm.sword.combat.ability.abilitytype.NoopAbility;
import btm.sword.system.entity.SwordEntity;

public class AbilityFactory {
	
	public static Ability create(AbilityOptions abilityOptions, SwordEntity executor) {
		
		switch(abilityOptions.getType()) {
			
			case DASH -> {
				return new DashAbility(abilityOptions, executor);
			}
			case BASIC_FIST_1 -> {
				return new BasicFist1Attack(abilityOptions, executor);
			}
			case BASIC_SWORD_1 -> {
				return new BasicSword1Attack(abilityOptions, executor);
			}
			default -> {
				return new NoopAbility(abilityOptions, executor);
			}
		}
	}
}
