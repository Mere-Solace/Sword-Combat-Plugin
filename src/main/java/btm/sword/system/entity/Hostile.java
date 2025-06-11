package btm.sword.system.entity;

import btm.sword.system.input.InputType;
import btm.sword.system.CombatProfile;

import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class Hostile extends SwordNPC implements Combatant {
	CombatProfile combatProfile;
	ItemStack itemInLeftHand = new ItemStack(Material.IRON_AXE);
	ItemStack itemInRightHand = new ItemStack(Material.SHIELD);
	
	public Hostile(LivingEntity associatedEntity) {
		super(associatedEntity);
		EntityEquipment equipment = associatedEntity.getEquipment();
		if (equipment != null) {
			equipment.setItemInMainHand(itemInLeftHand);
			equipment.setItemInOffHand(itemInRightHand);
			
			equipment.setChestplate(new ItemStack(Material.NETHERITE_CHESTPLATE));
		}
	}
	
	@Override
	public void performAbility(Material itemType, List<InputType> trigger) {
	
	}
}
