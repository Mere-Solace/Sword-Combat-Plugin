package btm.sword.system.entity;

import btm.sword.combat.AttackTriggerType;
import btm.sword.system.CombatProfile;

import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

public class Hostile extends SwordNPC implements Combatant {
	CombatProfile combatProfile;
	ItemStack itemInHand = new ItemStack(Material.IRON_AXE);
	
	public Hostile(LivingEntity associatedEntity) {
		super(associatedEntity);
		EntityEquipment equipment = associatedEntity.getEquipment();
		if (equipment != null) {
			equipment.setItemInMainHand(itemInHand);
			equipment.setItemInOffHand(itemInHand);
		}
	}
	
	@Override
	public void performAbility(Material itemType, AttackTriggerType trigger) {
	
	}
}
