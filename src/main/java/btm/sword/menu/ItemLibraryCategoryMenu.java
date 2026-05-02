package btm.sword.menu;

import java.util.List;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import btm.sword.config.Config;
import btm.sword.entity.player.SwordPlayer;
import btm.sword.item.core.ItemStackBuilder;
import btm.sword.item.core.LibraryCategory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.Window;

/**
 * Menu displaying all items within a single {@link LibraryCategory}.
 *
 * <p>Up to seven items are shown in the centre row (GUI indices 10–16). Clicking an item
 * gives a copy to the player's inventory, dropping any overflow at their feet. This menu
 * is opened directly by {@link ItemLibraryMenu} and is not registered in
 * {@link btm.sword.menu.InventoryMenuManager}.</p>
 */
public class ItemLibraryCategoryMenu extends Menu {

    private final LibraryCategory category;

    /**
     * Creates a new category menu for the given player and category.
     *
     * @param player   the player opening this menu
     * @param category the item library category to display
     */
    public ItemLibraryCategoryMenu(SwordPlayer player, LibraryCategory category) {
        super(player);
        this.category = category;
    }

    @Override
    public void open() {
        Player player = swordPlayer.player();

        Gui gui = Gui.normal()
            .setStructure(
                "# # # # # # # # #",
                "< # # # # # # # #",
                "# # # # # # # # #")
            .addIngredient('#', BORDER)
            .addIngredient('<', generatePreviousButtonOrDefault())
            .build();

        List<ItemStack> items = category.getItems();

        if (items.isEmpty()) {
            ItemStack placeholder = new ItemStackBuilder(Material.BARRIER)
                .name(Component.text("No items yet", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false))
                .lore(List.of(
                    Component.text("Items will appear here as content is developed.", NamedTextColor.DARK_GRAY)
                        .decoration(TextDecoration.ITALIC, false)))
                .build();
            gui.setItem(13, new SimpleItem(placeholder));
        } else {
            int[] slots = {10, 11, 12, 13, 14, 15, 16};
            for (int i = 0; i < Math.min(items.size(), slots.length); i++) {
                ItemStack stack = items.get(i);
                Component itemName = stack.hasItemMeta() && stack.getItemMeta().hasDisplayName()
                    ? stack.getItemMeta().displayName()
                    : Component.text(stack.getType().name(), NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false);

                ItemStack displayItem = new ItemStackBuilder(stack.getType())
                    .setMeta(stack.getItemMeta())
                    .lore(List.of(
                        itemName,
                        Component.empty(),
                        Component.text("Left-click \u2014 Give to inventory", Config.SwordColor.TEXT_ITEM_CONTROLS)
                            .decoration(TextDecoration.ITALIC, false)))
                    .build();
                displayItem.setAmount(stack.getAmount());

                ItemStack finalStack = stack;
                gui.setItem(slots[i], new SimpleItem(displayItem, click -> {
                    Map<Integer, ItemStack> leftover = player.getInventory().addItem(finalStack.clone());
                    for (ItemStack overflow : leftover.values()) {
                        player.getWorld().dropItem(player.getLocation(), overflow);
                    }
                }));
            }
        }

        Window.single()
            .setViewer(player)
            .setTitle(category.displayName() + " \u2014 Item Library")
            .setGui(gui)
            .build()
            .open();
    }
}
