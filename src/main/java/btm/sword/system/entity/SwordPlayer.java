package btm.sword.system.entity;

import btm.sword.combat.attack.Attack;
import btm.sword.combat.attack.AttackTriggerType;
import btm.sword.combat.CombatProfile;
import btm.sword.system.playerdata.PlayerData;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;

public class SwordPlayer extends SwordEntity implements Combatant {
	PlayerData data;
	CombatProfile combatProfile;
	
	public SwordPlayer(LivingEntity associatedEntity, PlayerData data) {
		super(associatedEntity);
		this.data = data;
		combatProfile = this.data.getCombatProfile();
	}
	
	@Override
	public void performAbility(Material itemType, AttackTriggerType trigger) {
		Attack attack = combatProfile.getAttack(itemType, trigger, this);
		if (attack == null) return;
		
		attack.run();
	}
}
