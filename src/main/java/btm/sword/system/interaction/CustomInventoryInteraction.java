package btm.sword.system.interaction;

import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;

/**
 * Extensible contract for custom inventory-driven item transformations.
 */
public interface CustomInventoryInteraction {

    /**
     * Returns the top-inventory type this interaction applies to.
     *
     * @return the inventory type
     */
    InventoryType inventoryType();

    /**
     * Returns {@code true} when the interaction should provide a custom result.
     *
     * @param context the current interaction context
     * @return whether this interaction matches
     */
    boolean matches(CustomInteractionContext context);

    /**
     * Builds the preview/result item shown to the player.
     *
     * @param context the current interaction context
     * @return the crafted result
     */
    ItemStack createResult(CustomInteractionContext context);

    /**
     * Consumes or mutates the input slots after the player takes the result.
     *
     * @param context the current interaction context
     */
    void consumeInputs(CustomInteractionContext context);
}
