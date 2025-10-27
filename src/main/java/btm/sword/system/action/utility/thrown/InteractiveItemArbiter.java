package btm.sword.system.action.utility.thrown;

import btm.sword.system.entity.Combatant;
import btm.sword.util.Cache;
import btm.sword.util.ParticleWrapper;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.HashMap;

public class InteractiveItemArbiter {
	public static final HashMap<ItemDisplay, ThrownItem> thrownItems = new HashMap<>();
	
	public static void put(ThrownItem thrownItem) {
		thrownItems.put(thrownItem.getDisplay(), thrownItem);
	}

    public static boolean checkIfInteractive(ItemDisplay id) {
        return thrownItems.containsKey(id);
    }

	public static ThrownItem remove(ItemDisplay display) {
		ThrownItem thrownItem = thrownItems.remove(display);
		if (thrownItem != null) {
			thrownItem.dispose();
			return thrownItem;
		}
		else return null;
	}
	
	public static void onGrab(ItemDisplay display, Combatant executor) {
		ThrownItem thrownItem = remove(display);
		if (thrownItem == null) return;
		
		ItemStack item = display.getItemStack();
		if (!item.isEmpty()) {
			executor.giveItem(item);
			Location i = display.getLocation();
			if (item.getType().isBlock()) {
				new ParticleWrapper(Particle.BLOCK, 50, 0.25, 0.25, 0.25, item.getType().createBlockData())
						.display(i);
			}
			Block b = i.clone().add(new Vector(0,-0.5,0)).getBlock();
			if (!b.getType().isAir()) {
				new ParticleWrapper(Particle.BLOCK, 30, 0.5, 0.5, 0.5, b.getBlockData())
						.display(i);
			}
			Cache.grabCloudParticle.display(display.getLocation());
			thrownItem.dispose();
		}
	}
}
