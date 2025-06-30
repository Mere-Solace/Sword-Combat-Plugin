package btm.sword.system.entity;

import btm.sword.system.playerdata.CombatProfile;

import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

public class Hostile extends Combatant {
	
	ItemStack itemInLeftHand = new ItemStack(Material.IRON_AXE);
	ItemStack itemInRightHand = new ItemStack(Material.SHIELD);
	
	public Hostile(LivingEntity associatedEntity, CombatProfile combatProfile) {
		super(associatedEntity, combatProfile);
		EntityEquipment equipment = associatedEntity.getEquipment();
		if (equipment != null) {
			equipment.setItemInMainHand(itemInLeftHand);
			equipment.setItemInOffHand(itemInRightHand);
			
			equipment.setChestplate(new ItemStack(Material.NETHERITE_CHESTPLATE));
		}
	}
	
	@Override
	public void onSpawn() {
		super.onSpawn();
		
	}
	
	@Override
	public void onDeath() {
	
	}
}
