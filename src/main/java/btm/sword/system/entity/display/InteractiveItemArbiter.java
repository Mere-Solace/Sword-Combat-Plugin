package btm.sword.system.entity.display;

import btm.sword.system.entity.Combatant;
import btm.sword.util.Cache;
import btm.sword.util.ParticleWrapper;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.HashSet;

public class InteractiveItemArbiter {
	public static final HashSet<ItemDisplay> interactiveItems = new HashSet<>();
	
	public static void put(ItemDisplay itemDisplay) {
		interactiveItems.add(itemDisplay);
	}
	
	public static boolean remove(ItemDisplay itemDisplay) {
		if(interactiveItems.remove(itemDisplay)) {
			itemDisplay.remove();
			return true;
		}
		else return false;
	}
	
	public static void onGrab(ItemDisplay interactiveItem, Combatant executor) {
		if (!remove(interactiveItem)) return;
		ItemStack item = interactiveItem.getItemStack();
		if (!item.isEmpty()) {
			executor.giveItem(item);
			Location i = interactiveItem.getLocation();
			if (item.getType().isBlock()) {
				new ParticleWrapper(Particle.BLOCK, 50, 0.25, 0.25, 0.25, item.getType().createBlockData())
						.display(i);
			}
			Block b = i.clone().add(new Vector(0,-0.5,0)).getBlock();
			if (!b.getType().isAir()) {
				new ParticleWrapper(Particle.BLOCK, 30, 0.5, 0.5, 0.5, b.getBlockData())
						.display(i);
			}
			Cache.grabCloudParticle.display(interactiveItem.getLocation());
			interactiveItem.remove();
		}
	}
}
