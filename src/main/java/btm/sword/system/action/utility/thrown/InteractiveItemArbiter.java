package btm.sword.system.action.utility.thrown;

import java.util.HashMap;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import btm.sword.system.entity.base.SwordEntity;
import btm.sword.system.entity.types.Combatant;
import btm.sword.system.entity.umbral.UmbralBlade;
import btm.sword.system.entity.umbral.input.BladeRequest;
import btm.sword.util.Prefab;
import btm.sword.util.display.ParticleWrapper;

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
    private static final HashMap<ItemDisplay, ThrownItem> thrownItems = new HashMap<>();

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

    public static boolean isImaplingEntity(SwordEntity self, ItemDisplay targeted) {
        ThrownItem thrown = thrownItems.getOrDefault(targeted, null);
        return thrown != null && thrown.getHitEntity() != null && thrown.getHitEntity().equals(self);
    }

    /**
     * Removes and disposes of a {@link ThrownItem} associated with the given {@link ItemDisplay}.
     * <p>
     * This should be called when the item is picked up or otherwise invalidated.
     *
     * @param display The display entity to remove.
     * @return The removed {@link ThrownItem}, or {@code null} if none was registered.
     */
    public static ThrownItem remove(ItemDisplay display, boolean dispose) {
        ThrownItem thrownItem = thrownItems.remove(display);
        if (thrownItem != null) {
            if (dispose) thrownItem.dispose();
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
        ThrownItem thrownItem = remove(display, true); // Stop displaying the ItemDisplay
        if (thrownItem == null) return;

        ItemStack item = display.getItemStack();

        if (!item.isEmpty()) {
            if (thrownItem instanceof UmbralBlade umbralBlade) {
                if (!umbralBlade.isOwnedBy(executor)) {
                    // TODO maybe play some cool rejection animation here
                    return;
                }

                umbralBlade.returnToWielderAndRequestState(BladeRequest.SHEATH); // TODO add umbral blade onPickup()
                return;
            }

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
            Prefab.Particles.GRAB_CLOUD.display(display.getLocation());
            thrownItem.dispose();
        }
    }

    /**
     * Cleans up all active thrown item displays during server shutdown.
     * Disposes of all registered thrown items and clears the registry.
     */
    public static void cleanupAll() {
        for (ThrownItem thrownItem : thrownItems.values()) {
            thrownItem.dispose();
        }
        thrownItems.clear();
    }
}
