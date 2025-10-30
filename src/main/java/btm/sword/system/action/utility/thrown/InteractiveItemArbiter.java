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

/**
 * Manages {@link ThrownItem} instances that are currently active and displayed in the world.
 * <p>
 * Handles registration, lookup, interaction, and cleanup of thrown items that use {@link ItemDisplay} entities
 * for visual representation and interaction tracking.
 */
public class InteractiveItemArbiter {
    /**
     * Registry of all active thrown items mapped by their associated {@link ItemDisplay}.
     */
	public static final HashMap<ItemDisplay, ThrownItem> thrownItems = new HashMap<>();

    /**
     * Registers a new {@link ThrownItem} with its {@link ItemDisplay} as the key.
     *
     * @param thrownItem The thrown item to register.
     */
	public static void put(ThrownItem thrownItem) {
		thrownItems.put(thrownItem.getDisplay(), thrownItem);
	}

    /**
     * Checks if the given {@link ItemDisplay} is currently interactive (i.e., associated with a {@link ThrownItem}).
     *
     * @param id The display entity to check.
     * @return {@code true} if the display is tracked as an interactive item, otherwise {@code false}.
     */
    public static boolean checkIfInteractive(ItemDisplay id) {
        return thrownItems.containsKey(id);
    }

    /**
     * Removes and disposes of a {@link ThrownItem} associated with the given {@link ItemDisplay}.
     * <p>
     * This should be called when the item is picked up or otherwise invalidated.
     *
     * @param display The display entity to remove.
     * @return The removed {@link ThrownItem}, or {@code null} if none was registered.
     */
	public static ThrownItem remove(ItemDisplay display) {
		ThrownItem thrownItem = thrownItems.remove(display);
		if (thrownItem != null) {
			thrownItem.dispose();
			return thrownItem;
		}
		else return null;
	}

    /**
     * Handles when a {@link Combatant} grabs an interactive {@link ItemDisplay}.
     * <p>
     * Transfers the associated {@link ItemStack} to the executor, displays pickup particles,
     * and disposes of the {@link ThrownItem}.
     *
     * @param display  The item display being grabbed.
     * @param executor The combatant performing the grab.
     */
	public static void onGrab(ItemDisplay display, Combatant executor) {
		ThrownItem thrownItem = remove(display); // Stop displaying the ItemDisplay
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
