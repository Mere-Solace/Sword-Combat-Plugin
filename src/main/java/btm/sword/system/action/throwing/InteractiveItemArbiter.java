package btm.sword.system.action.throwing;

import java.util.HashMap;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import btm.sword.system.action.throwing.types.DroppedItem;
import btm.sword.system.action.throwing.types.ThrownItem;
import btm.sword.system.entity.base.SwordEntity;
import btm.sword.system.entity.impl.Combatant;
import btm.sword.system.entity.umbral.UmbralBlade;
import btm.sword.utility.Prefab;
import btm.sword.utility.display.ParticleWrapper;

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
    private static final HashMap<ItemDisplay, InteractiveItem> INTERACTIVE_ITEMS = new HashMap<>();

    /**
     * Registers a new {@link ThrownItem} with its {@link ItemDisplay} as the key.
     *
     * @param interactiveItem The thrown item to register.
     */
    public static void put(InteractiveItem interactiveItem) {
        INTERACTIVE_ITEMS.put(interactiveItem.getDisplay(), interactiveItem);
    }

    /**
     * Checks if the given {@link ItemDisplay} is currently interactive (i.e., associated with a {@link ThrownItem}).
     *
     * @param id The display entity to check.
     * @return {@code true} if the display is tracked as an interactive item, otherwise {@code false}.
     */
    public static boolean checkIfInteractive(ItemDisplay id) {
        return INTERACTIVE_ITEMS.containsKey(id);
    }

    public static boolean isUmbralBlade(ItemDisplay id) {
        return INTERACTIVE_ITEMS.get(id) instanceof UmbralBlade;
    }

    public static boolean isImpaling(SwordEntity self, ItemDisplay targeted) {
        InteractiveItem thrown = INTERACTIVE_ITEMS.getOrDefault(targeted, null);
        return thrown instanceof ThrownItem ti && ti.getHitEntity() != null && ti.getHitEntity().equals(self);
    }

    /**
     * Removes and disposes of a {@link ThrownItem} associated with the given {@link ItemDisplay}.
     * <p>
     * This should be called when the item is picked up or otherwise invalidated.
     *
     * @param display The display entity to remove.
     * @return The removed {@link ThrownItem}, or {@code null} if none was registered.
     */
    public static InteractiveItem remove(ItemDisplay display, boolean dispose) {
        InteractiveItem thrownItem = INTERACTIVE_ITEMS.remove(display);
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
        InteractiveItem interactiveItem = remove(display, false);
        if (interactiveItem == null) return;
        if (interactiveItem instanceof ThrownItem thrownItem) {
            thrownItem.setRetrieved(true);
//            if (thrownItem.getHitEntity() instanceof SwordEntity swordEntity && swordEntity.self().isValid()) {
//                Impalement impalement = thrownItem.getThisImpalement();
//                swordEntity.removeImpalement(impalement);
//            }
        }

        // UmbralBlade manages its own item state (weapon/link/blade fields) and never populates
        // the inherited SimulatedDisplay.itemStack field — check before the null guard below.
        if (interactiveItem instanceof UmbralBlade umbralBlade) {
            umbralBlade.setRetrieved(true);
            umbralBlade.onGrab(executor);
            return;
        }

        ItemStack item = interactiveItem.getItemStack();
        if (item == null) return;
        if (!item.isEmpty()) {
            interactiveItem.dispose();
            executor.giveItem(item);
            Location displayLoc = display.getLocation();
            if (item.getType().isBlock()) {
                new ParticleWrapper(Particle.BLOCK, 50, 0.25, 0.25, 0.25, item.getType().createBlockData())
                        .display(displayLoc);
            }
            Block b = displayLoc.clone().add(new Vector(0,-0.5,0)).getBlock();
            if (!b.getType().isAir()) {
                new ParticleWrapper(Particle.BLOCK, 30, 0.5, 0.5, 0.5, b.getBlockData())
                        .display(displayLoc);
            }
            Prefab.Particles.GRAB_CLOUD.display(display.getLocation());
            interactiveItem.dispose();
        }
    }

    /**
     * Cleans up all active thrown item displays during server shutdown.
     * Disposes of all registered thrown items and clears the registry.
     */
    public static void cleanupAll() {
        // TODO: #81 - Cleanup considerations for thrown items
        for (InteractiveItem interactiveItem : INTERACTIVE_ITEMS.values()) {
            interactiveItem.dispose();
        }
        INTERACTIVE_ITEMS.clear();
    }

    public static void dropNaturally(Location origin, ItemStack stack) {
        if (!stack.isEmpty()) {
            Vector dropVel = new Vector(
                Math.random() - 0.5,
                Math.random() + 0.5,
                Math.random() - 0.5
            ).multiply(0.5);

            DroppedItem stuck = new DroppedItem(origin, dropVel, stack);
            stuck.register();
        }
    }
}
