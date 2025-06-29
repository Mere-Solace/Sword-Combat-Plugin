package btm.sword.system.entity.display;

import btm.sword.system.entity.Combatant;
import btm.sword.util.Cache;
import btm.sword.util.ParticleWrapper;
import org.bukkit.Particle;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;

public class InteractiveItemArbiter {
	public static final HashMap<ArmorStand, ItemDisplay> itemHitboxMap = new HashMap<>();
	
	public static void register(ArmorStand marker, ItemDisplay interactiveItem) {
		itemHitboxMap.put(marker, interactiveItem);
	}
	
	public static void remove(ArmorStand marker) {
		itemHitboxMap.remove(marker);
	}
	
	public static void onPickup(ArmorStand marker, Combatant executor, BlockData blockData) {
		marker.setMarker(true);
		marker.setInvulnerable(true);
		marker.setCanMove(false);
		marker.setGravity(false);
		marker.setVisible(false);
		marker.setCollidable(false);
		
		ItemDisplay interactiveItem = itemHitboxMap.get(marker);
		if (interactiveItem != null) {
			ItemStack itemStack = interactiveItem.getItemStack();
			if (!itemStack.isEmpty()) {
				executor.giveItem(itemStack);
				if (itemStack.getType().isBlock()) {
					new ParticleWrapper(Particle.BLOCK, 50, 0.25, 0.25, 0.25, itemStack.getType().createBlockData())
							.display(marker.getLocation());
				}
				new ParticleWrapper(Particle.BLOCK, 30, 0.5, 0.5, 0.5, blockData)
						.display(marker.getLocation());
				Cache.grabCloudParticle.display(marker.getLocation());
			}
			interactiveItem.remove();
		}
		remove(marker);
		marker.remove();
	}
}
