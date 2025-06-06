package btm.sword.system.entity;

import btm.sword.combat.AttackTriggerType;
import btm.sword.system.CombatProfile;
import btm.sword.system.playerdata.PlayerData;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;

public class SwordPlayer extends SwordEntity implements Combatant{
	PlayerData data;
	CombatProfile combatProfile;
	
	public SwordPlayer(LivingEntity associatedEntity, PlayerData data) {
		super(associatedEntity);
		this.data = data;
		combatProfile = this.data.getCombatProfile();
	}
	
	@Override
	public void performAbility(Material itemType, AttackTriggerType trigger) {
		if (itemType.toString().toLowerCase().contains("sword")) {
			switch(trigger) {
				case LEFT -> associatedEntity.sendMessage("Attack with SWORD!");
				case RIGHT -> associatedEntity.sendMessage("Perform skill with SWORD!");
			}
		}
		if (itemType.toString().toLowerCase().contains("axe")) {
			switch(trigger) {
				case LEFT -> associatedEntity.sendMessage("Attack with AXE!");
				case RIGHT -> associatedEntity.sendMessage("Perform skill with AXE!");
			}
		}
	}
}
