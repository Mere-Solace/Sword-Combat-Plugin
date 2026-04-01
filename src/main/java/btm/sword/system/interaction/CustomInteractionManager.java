package btm.sword.system.interaction;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.event.inventory.InventoryType;

import btm.sword.system.interaction.smithing.RotateThrowStyleSmithingInteraction;

/**
 * Registry and lookup facade for custom inventory interactions.
 */
public final class CustomInteractionManager {
    private static final List<CustomInventoryInteraction> INTERACTIONS = new ArrayList<>();

    private CustomInteractionManager() {}

    public static void initialize() {
        INTERACTIONS.clear();
        register(new RotateThrowStyleSmithingInteraction());
    }

    public static void register(CustomInventoryInteraction interaction) {
        INTERACTIONS.add(interaction);
    }

    public static CustomInventoryInteraction find(CustomInteractionContext context) {
        InventoryType type = context.inventoryType();
        for (CustomInventoryInteraction interaction : INTERACTIONS) {
            if (interaction.inventoryType() == type && interaction.matches(context)) {
                return interaction;
            }
        }
        return null;
    }
}
