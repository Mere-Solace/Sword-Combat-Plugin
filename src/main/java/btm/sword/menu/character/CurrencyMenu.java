package btm.sword.menu.character;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import btm.sword.config.section.ColorConfig;
import btm.sword.entity.player.SwordPlayer;
import btm.sword.item.core.ItemStackBuilder;
import btm.sword.item.core.KeyRegistry;
import btm.sword.menu.Menu;
import btm.sword.playerdata.PlayerStorage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.Window;

/**
 * Pouch menu displaying the player's steel credit balance with an auto-pickup toggle.
 *
 * <p><b>Layout</b> (3-row chest, 27 slots):</p>
 * <pre>
 * # # # # # # # # #
 * &lt; . . C . . . T #
 * # # # # # # # # #
 * </pre>
 *
 * <ul>
 *   <li>{@code C} — credit balance display. Shift+Left-click collects all credit items
 *       from the player's main inventory.
 *   <li>{@code T} — auto-pickup-credits toggle.
 *   <li>{@code &lt;} — back navigation.
 * </ul>
 */
public class CurrencyMenu extends Menu {

    /** Constructs the currency menu for the given player. */
    public CurrencyMenu(SwordPlayer player) {
        super(player);
    }

    @Override
    public void open() {
        Player player = swordPlayer.player();
        PlayerStorage storage = swordPlayer.getPlayerStorage();

        SimpleItem creditDisplay = buildCreditDisplay(player, storage);

        Gui gui = Gui.normal()
            .setStructure(
                "# # # # # # # # #",
                "< . . C . . . T #",
                "# # # # # # # # #")
            .addIngredient('#', BORDER)
            .addIngredient('<', generatePreviousButtonOrDefault())
            .addIngredient('C', creditDisplay)
            .addIngredient('T', toggle(
                "Auto-Pickup Credits",
                storage::isAutoPickupCredits,
                storage::toggleAutoPickupCredits
            ))
            .build();

        Window.single()
            .setViewer(player)
            .setTitle("Currency Pouch")
            .setGui(gui)
            .build()
            .open();
    }

    private SimpleItem buildCreditDisplay(Player player, PlayerStorage storage) {
        final List<Component> lore = getLoreComponents(storage);

        ItemStack displayItem = new ItemStackBuilder(Material.NETHERITE_INGOT)
            .hideAll()
            .name(Component.text("Steel Credits", ColorConfig.TEXT_COOL, TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false))
            .lore(lore)
            .build();

        return new SimpleItem(displayItem, click -> {
            if (click.getClickType() != ClickType.SHIFT_LEFT) return;
            int collected = collectCreditsFromInventory(player, storage);
            if (collected > 0) {
                player.sendActionBar(
                    Component.text("[+", NamedTextColor.GREEN)
                        .append(Component.text(collected + " ✦", ColorConfig.TEXT_COOL))
                        .append(Component.text("] → Currency Pouch", NamedTextColor.GREEN))
                );
            }
            this.open();
        });
    }

    private static @NotNull List<Component> getLoreComponents(PlayerStorage storage) {
        int credits = storage.getSteelCredits();

        return List.of(
            Component.empty(),
            Component.text("Balance: ", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                .append(Component.text(credits + " ✦", ColorConfig.TEXT_COOL)
                    .decoration(TextDecoration.ITALIC, false)
                    .decoration(TextDecoration.BOLD, false)),
            Component.empty(),
            Component.text("Shift + Left-click ", ColorConfig.TEXT_ITEM_CONTROLS).decoration(TextDecoration.ITALIC, false)
                .append(Component.text("— Collect credit items from inventory", ColorConfig.TEXT_ITEM_BASE)
                    .decoration(TextDecoration.ITALIC, false))
        );
    }

    /**
     * Scans slots 0–35 of the player's main inventory for physical credit items
     * (tagged with {@link KeyRegistry#CREDIT_ITEM_KEY}) and deposits their value.
     *
     * @return the total credits collected
     */
    private int collectCreditsFromInventory(Player player, PlayerStorage storage) {
        int total = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack slot = player.getInventory().getItem(i);
            if (slot == null || slot.isEmpty()) continue;
            Integer value = KeyRegistry.getKeyField(slot, KeyRegistry.CREDIT_ITEM_KEY, PersistentDataType.INTEGER);
            if (value == null) continue;
            total += value * slot.getAmount();
            player.getInventory().setItem(i, null);
        }
        if (total > 0) storage.addCredits(total);
        return total;
    }
}
