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

    /** Clears all registered interactions and re-registers the built-in set. */
    public static void initialize() {
        INTERACTIONS.clear();
        register(new RotateThrowStyleSmithingInteraction());
    }

    /** Adds a custom inventory interaction to the registry. */
    public static void register(CustomInventoryInteraction interaction) {
        INTERACTIONS.add(interaction);
    }

    /** Returns the first registered interaction whose type and match predicate satisfy the context, or {@code null}. */
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
