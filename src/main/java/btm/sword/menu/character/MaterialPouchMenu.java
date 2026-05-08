package btm.sword.menu.character;

import java.util.List;
import java.util.Map;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import btm.sword.config.section.ColorConfig;
import btm.sword.entity.player.SwordPlayer;
import btm.sword.item.core.ItemStackBuilder;
import btm.sword.item.material.MaterialType;
import btm.sword.menu.Menu;
import btm.sword.playerdata.PlayerStorage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.Window;

/**
 * Pouch menu displaying the player's stored {@link MaterialType} stacks.
 *
 * <p><b>Layout</b> (3-row chest, 27 slots):</p>
 * <pre>
 * # # # # # # # # #
 * &lt; M M M M M M T #
 * # # # # # # # # #
 * </pre>
 *
 * <ul>
 *   <li>{@code M} — one slot per {@link MaterialType} (up to 6 visible types). Shows count in lore.
 *       <br>Left-click with cursor item → store if matching type.
 *       <br>Left-click (no cursor) → withdraw 1 to cursor.
 *       <br>Right-click (no cursor) → withdraw all to inventory.
 *       <br>Shift+Left-click → bulk-store all matching items from player's main inventory.
 *   <li>{@code T} — auto-pickup-materials toggle.
 *   <li>{@code &lt;} — back navigation.
 * </ul>
 */
public class MaterialPouchMenu extends Menu {

    /** Constructs the material pouch menu for the given player. */
    public MaterialPouchMenu(SwordPlayer player) {
        super(player);
    }

    @Override
    public void open() {
        Player player = swordPlayer.player();
        PlayerStorage storage = swordPlayer.getPlayerStorage();
        MaterialType[] types = MaterialType.values();

        Gui.Builder.Normal builder = Gui.normal()
            .setStructure(
                "# # # # # # # # #",
                "< M M M M M M T #",
                "# # # # # # # # #")
            .addIngredient('#', BORDER)
            .addIngredient('<', generatePreviousButtonOrDefault())
            .addIngredient('T', toggle(
                "Auto-Pickup Materials",
                storage::isAutoPickupMaterials,
                storage::toggleAutoPickupMaterials
            ));

        // 'M' slots have no bound ingredient — overridden below per MaterialType.
        // Up to 6 types fit in the row; pagination can be added later if needed.
        Gui gui = builder.build();

        // Material slots: GUI indices 10–15 (row 2, columns 2–7, 9-wide grid).
        int[] materialIndices = {10, 11, 12, 13, 14, 15};
        for (int i = 0; i < materialIndices.length && i < types.length; i++) {
            MaterialType type = types[i];
            gui.setItem(materialIndices[i], buildMaterialSlot(player, storage, type));
        }

        Window.single()
            .setViewer(player)
            .setTitle("Material Pouch")
            .setGui(gui)
            .build()
            .open();
    }

    /**
     * Builds a {@link SimpleItem} for the given {@link MaterialType} slot.
     * Handles all click interactions: store from cursor, withdraw 1, withdraw all, bulk store.
     */
    private SimpleItem buildMaterialSlot(Player player, PlayerStorage storage, MaterialType type) {
        int count = storage.getMaterialCount(type);

        List<Component> slotLore = List.of(
            Component.empty(),
            Component.text("Stored: ", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                .append(Component.text(count, ColorConfig.TEXT_COOL).decoration(TextDecoration.ITALIC, false)),
            Component.empty(),
            Component.text("Left-click ", ColorConfig.TEXT_ITEM_CONTROLS).decoration(TextDecoration.ITALIC, false)
                .append(Component.text("(with item) — Store", ColorConfig.TEXT_ITEM_BASE).decoration(TextDecoration.ITALIC, false)),
            Component.text("Left-click ", ColorConfig.TEXT_ITEM_CONTROLS).decoration(TextDecoration.ITALIC, false)
                .append(Component.text("— Withdraw 1", ColorConfig.TEXT_ITEM_BASE).decoration(TextDecoration.ITALIC, false)),
            Component.text("Right-click ", ColorConfig.TEXT_ITEM_CONTROLS).decoration(TextDecoration.ITALIC, false)
                .append(Component.text("— Withdraw all", ColorConfig.TEXT_ITEM_BASE).decoration(TextDecoration.ITALIC, false)),
            Component.text("Shift + Left-click ", ColorConfig.TEXT_ITEM_CONTROLS).decoration(TextDecoration.ITALIC, false)
                .append(Component.text("— Store all from inventory", ColorConfig.TEXT_ITEM_BASE).decoration(TextDecoration.ITALIC, false))
        );

        ItemStack displayItem = new ItemStackBuilder(type.icon())
            .hideAll()
            .name(type.displayName())
            .lore(slotLore)
            .build();

        return new SimpleItem(displayItem, click -> {
            ClickType clickType = click.getClickType();

            if (clickType == ClickType.SHIFT_LEFT) {
                storeAllFromInventory(player, storage, type);
                this.open();
                return;
            }

            ItemStack cursor = player.getItemOnCursor();
            if (!cursor.isEmpty()) {
                MaterialType cursorType = MaterialType.fromItem(cursor);
                if (cursorType == type) {
                    int available = SwordPlayer.MATERIAL_SLOTS_TOTAL - storage.getTotalMaterialSlots();
                    int toStore = Math.min(cursor.getAmount(), available);
                    if (toStore > 0) {
                        storage.addMaterial(type, toStore);
                        cursor.setAmount(cursor.getAmount() - toStore);
                        player.setItemOnCursor(cursor.getAmount() <= 0 ? null : cursor);
                        alertStored(player, type, toStore);
                    }
                    this.open();
                }
                return;
            }

            int currentCount = storage.getMaterialCount(type);
            if (currentCount <= 0) return;

            if (clickType == ClickType.RIGHT) {
                // Withdraw all — distribute into inventory, drop overflow
                withdrawToInventory(player, storage, type, currentCount);
            } else {
                // Withdraw 1 — place on cursor
                storage.removeMaterial(type, 1);
                player.setItemOnCursor(type.buildItemStack(1));
            }
            this.open();
        });
    }

    /**
     * Scans slots 0–35 of the player's main inventory for items matching the given
     * {@link MaterialType} and stores them all into the pouch, respecting the slot cap.
     */
    private void storeAllFromInventory(Player player, PlayerStorage storage, MaterialType type) {
        int totalStored = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack slot = player.getInventory().getItem(i);
            if (slot == null || slot.isEmpty()) continue;
            if (MaterialType.fromItem(slot) != type) continue;

            int available = SwordPlayer.MATERIAL_SLOTS_TOTAL - storage.getTotalMaterialSlots();
            if (available <= 0) break;

            int toStore = Math.min(slot.getAmount(), available);
            storage.addMaterial(type, toStore);
            totalStored += toStore;
            slot.setAmount(slot.getAmount() - toStore);
            player.getInventory().setItem(i, slot.getAmount() <= 0 ? null : slot);
        }
        if (totalStored > 0) alertStored(player, type, totalStored);
    }

    /**
     * Withdraws up to {@code amount} of the given type from storage into the player's
     * main inventory. Any that do not fit are dropped at the player's location.
     */
    private void withdrawToInventory(Player player, PlayerStorage storage, MaterialType type, int amount) {
        storage.removeMaterial(type, amount);
        ItemStack stack = type.buildItemStack(amount);
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(stack);
        for (ItemStack drop : leftover.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), drop);
        }
    }

    /** Sends an action-bar alert to the player when materials are stored. */
    private void alertStored(Player player, MaterialType type, int amount) {
        player.sendActionBar(
            Component.text("[+", NamedTextColor.GREEN)
                .append(Component.text(amount, ColorConfig.TEXT_COOL))
                .append(Component.text(" ", NamedTextColor.GREEN))
                .append(type.displayName())
                .append(Component.text("] → Material Pouch", NamedTextColor.GREEN))
        );
    }
}
