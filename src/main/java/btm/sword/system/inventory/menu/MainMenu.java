package btm.sword.system.inventory.menu;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import btm.sword.config.Config;
import btm.sword.gamemode.QueueManager;
import btm.sword.gamemode.type.CaptureTheFlag1v1;
import btm.sword.system.control.SwordScheduler;
import btm.sword.system.entity.SwordEntityArbiter;
import btm.sword.system.entity.impl.Dummy;
import btm.sword.system.entity.impl.SwordPlayer;
import btm.sword.system.inventory.InventoryMenuManager;
import btm.sword.system.item.ItemStackBuilder;
import btm.sword.system.item.special.NonMovableItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.Window;

public class MainMenu extends Menu {
    public static final List<Component> HOW_TO_PLAY = List.of(
        Component.text("", Config.SwordColor.TEXT_ITEM_BASE),

        Component.text("Combat Basics", Config.SwordColor.TEXT_ITEM_HEADER, TextDecoration.ITALIC),

        Component.text("Left Click", Config.SwordColor.TEXT_ITEM_CONTROLS)
            .append(Component.text(" – Basic Attack Chain (× 3)", Config.SwordColor.TEXT_ITEM_BASE)),
        Component.text("  • Chain up to 3 slashes", Config.SwordColor.TEXT_ITEM_BASE),

        Component.text("Shift + Left Click", Config.SwordColor.TEXT_ITEM_CONTROLS)
            .append(Component.text(" – Grab", Config.SwordColor.TEXT_ITEM_BASE)),
        Component.text("  • Short-range control tool", Config.SwordColor.TEXT_ITEM_BASE),

        Component.text("Drop + Right Click, hold to release", Config.SwordColor.TEXT_ITEM_CONTROLS)
            .append(Component.text(" – Throw Weapon", Config.SwordColor.TEXT_ITEM_BASE)),
        Component.text("  • Thrown weapons deal Toughness / Shards damage on hit", Config.SwordColor.TEXT_ITEM_BASE),

        Component.text("", Config.SwordColor.TEXT_ITEM_BASE),

        Component.text("Umbral Blade", Config.SwordColor.TEXT_ITEM_HEADER, TextDecoration.ITALIC),

        Component.text("Shift + Swap", Config.SwordColor.TEXT_ITEM_CONTROLS)
            .append(Component.text(" – Toggle Standby / Sheathed", Config.SwordColor.TEXT_ITEM_BASE)),
        Component.text("Shift + Drop", Config.SwordColor.TEXT_ITEM_CONTROLS)
            .append(Component.text(" – Wield Blade", Config.SwordColor.TEXT_ITEM_BASE)),
        Component.text("Left Click × 3 (Standby)", Config.SwordColor.TEXT_ITEM_CONTROLS)
            .append(Component.text(" – Quick Attacks", Config.SwordColor.TEXT_ITEM_BASE)),
        Component.text("Drop + Left Click (× 3)", Config.SwordColor.TEXT_ITEM_CONTROLS)
            .append(Component.text(" – Heavy Sweep", Config.SwordColor.TEXT_ITEM_BASE)),
        Component.text("Drop + Right Click (Standby)", Config.SwordColor.TEXT_ITEM_CONTROLS)
            .append(Component.text(" – Lunge", Config.SwordColor.TEXT_ITEM_BASE)),
        Component.text("Swap + Left [combo]", Config.SwordColor.TEXT_ITEM_CONTROLS)
            .append(Component.text(" – Umbral Skills", Config.SwordColor.TEXT_ITEM_BASE)),

        Component.text("", Config.SwordColor.TEXT_ITEM_BASE),

        Component.text("→ Open Combat Reference for a full moveset guide.", Config.SwordColor.TEXT_ITEM_HEADER)
    );


    public static final ItemStack HOW_TO_PLAY_ITEM = ItemStackBuilder
        .of(Material.KNOWLEDGE_BOOK)
        .hideAll()
        .name(Component.text("Input Instructions", Config.SwordColor.TEXT_COOL, TextDecoration.BOLD))
        .lore(HOW_TO_PLAY)
        .build();

    public MainMenu(SwordPlayer player) {
        super(player);
    }

    @Override
    public void open() {
        Player player = swordPlayer.player();

        SimpleItem playerInfo = new SimpleItem(
            swordPlayer.getPlayerHeadItemWithCustomText(
                Component.text("Character Info"),
                List.of(Component.text("View your stats, loadout, and progress"))
            ),
            click -> InventoryMenuManager.openMenu(CharacterMenu.class, swordPlayer)
        );

        SimpleItem queueForCTF = new SimpleItem(
            new ItemBuilder(Material.GUSTER_BANNER_PATTERN)
                .setDisplayName("Join the Queue for Capture the Flag (1v1)!"),
            click -> QueueManager.enqueue(
                CaptureTheFlag1v1.class, (SwordPlayer) SwordEntityArbiter.getOrAdd(click.getPlayer())
            )
        );

        SimpleItem spawnDummy = new SimpleItem(
            new ItemBuilder(Material.ARMOR_STAND)
                .setDisplayName("Spawn a Training Dummy (max: 3 per player)"),
            click ->  {
                if (swordPlayer.getCurNumDummies() >= swordPlayer.getMaxNumDummies()) {
                    swordPlayer.message("You have the max number of dummies active!");
                    return;
                }
                ArmorStand dummy = (ArmorStand) swordPlayer.world().spawnEntity(swordPlayer.locFromEyeDir(2), EntityType.ARMOR_STAND);
                dummy.addScoreboardTag("dummy");

                SwordScheduler.runBukkitTaskLater(() -> {
                        Dummy swordDummy = (Dummy) SwordEntityArbiter.getOrAdd(dummy);
                        if (swordDummy == null || swordDummy.isInvalid()) {
                            return;
                        }
                        swordDummy.setOwner(swordPlayer);
                        swordPlayer.getYourDummies().add(swordDummy);
                        swordPlayer.incrementNumDummies();
                    }, 100, TimeUnit.MILLISECONDS
                );
            }
        );

        SimpleItem combatReference = new SimpleItem(
            new ItemStackBuilder(Material.WRITABLE_BOOK)
                .hideAll()
                .name(Component.text("Combat Reference", Config.SwordColor.TEXT_COOL, TextDecoration.BOLD))
                .lore(List.of(Component.text("View your full moveset and toggle movement inputs.", Config.SwordColor.TEXT_ITEM_BASE)))
                .build(),
            click -> InventoryMenuManager.openMenu(MovesetMenu.class, swordPlayer)
        );

        SimpleItem trashItem = new SimpleItem(
            new ItemStackBuilder(Material.BARRIER)
                .hideAll()
                .name(Component.text("Item Trash", Config.SwordColor.TEXT_COOL, TextDecoration.BOLD))
                .lore(List.of(Component.text("Click with an item to destroy it.", Config.SwordColor.TEXT_ITEM_BASE)))
                .build(),
            click -> {
                if (click.getClickType() == ClickType.DOUBLE_CLICK) {

                }
                ItemStack cursor = click.getPlayer().getItemOnCursor();
                if (cursor.isEmpty() || NonMovableItem.isNonMovable(cursor)) return;
                click.getPlayer().setItemOnCursor(new ItemStack(Material.AIR));
            }
        );

        Gui.Builder.Normal builder = Gui.normal()
            .setStructure(
                "# # # . . . # # #",
                "# . . . P . . . #",
                ". . . . D Q . . .",
                ". . . . . . . . .",
                "# T . . H . M . #",
                "# # # < V > # # #")
            .addIngredient('#', BORDER)
            .addIngredient('Q', queueForCTF)
            .addIngredient('H', playerInfo)
            .addIngredient('P', HOW_TO_PLAY_ITEM)
            .addIngredient('D', spawnDummy)
            .addIngredient('M', combatReference)
            .addIngredient('T', trashItem)
            .addIngredient('<', generatePreviousButtonOrDefault())
            .addIngredient('>', generateForwardPreviousButtonOrDefault());

        if (player.isOp()) {
            builder.addIngredient('V', new SimpleItem(
                new ItemStackBuilder(Material.DEBUG_STICK)
                    .name(Component.text("Dev Menu", Config.SwordColor.TEXT_COOL, TextDecoration.BOLD))
                    .build(),
                click -> InventoryMenuManager.openMenu(DevMenu.class, swordPlayer)
            ));
        }

        Gui gui = builder.build();

        Window window = Window.single()
            .setViewer(player)
            .setTitle("MainMenu")
            .setGui(gui)
            .build();

        window.open();
    }
}
