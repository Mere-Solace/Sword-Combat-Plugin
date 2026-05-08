package btm.sword.menu.main;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import btm.sword.config.section.ColorConfig;
import btm.sword.entity.arbiter.SwordEntityArbiter;
import btm.sword.entity.mob.Dummy;
import btm.sword.entity.player.SwordPlayer;
import btm.sword.gamemode.QueueManager;
import btm.sword.gamemode.type.CaptureTheFlag1v1;
import btm.sword.gamemode.type.RoguelikeRun;
import btm.sword.item.core.ItemStackBuilder;
import btm.sword.item.special.NonMovableItem;
import btm.sword.menu.InventoryMenuManager;
import btm.sword.menu.Menu;
import btm.sword.menu.character.CharacterMenu;
import btm.sword.menu.character.MovesetMenu;
import btm.sword.menu.dev.DevMenu;
import btm.sword.runtime.scheduler.SwordScheduler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.Window;

/**
 * The primary player-facing hub menu.
 * <p>
 * Displays the how-to-play reference, CTF queue button, dummy spawner, player info, and—for
 * operators—a Dev Menu shortcut. Navigation to sub-screens (character, combat reference) is
 * also wired here.
 * </p>
 */
public class MainMenu extends Menu {
    public static final List<Component> HOW_TO_PLAY = List.of(
        Component.text("", ColorConfig.TEXT_ITEM_BASE),

        Component.text("Combat Basics", ColorConfig.TEXT_ITEM_HEADER, TextDecoration.ITALIC),

        Component.text("Left Click", ColorConfig.TEXT_ITEM_CONTROLS)
            .append(Component.text(" – Basic Attack Chain (× 3)", ColorConfig.TEXT_ITEM_BASE)),
        Component.text("  • Chain up to 3 slashes", ColorConfig.TEXT_ITEM_BASE),

        Component.text("Shift + Left Click", ColorConfig.TEXT_ITEM_CONTROLS)
            .append(Component.text(" – Grab", ColorConfig.TEXT_ITEM_BASE)),
        Component.text("  • Short-range control tool", ColorConfig.TEXT_ITEM_BASE),

        Component.text("Drop + Right Click, hold to release", ColorConfig.TEXT_ITEM_CONTROLS)
            .append(Component.text(" – Throw Weapon", ColorConfig.TEXT_ITEM_BASE)),
        Component.text("  • Thrown weapons deal Toughness / Shards damage on hit", ColorConfig.TEXT_ITEM_BASE),

        Component.text("", ColorConfig.TEXT_ITEM_BASE),

        Component.text("Umbral Blade", ColorConfig.TEXT_ITEM_HEADER, TextDecoration.ITALIC),

        Component.text("Shift + Swap", ColorConfig.TEXT_ITEM_CONTROLS)
            .append(Component.text(" – Toggle Standby / Sheathed", ColorConfig.TEXT_ITEM_BASE)),
        Component.text("Shift + Drop", ColorConfig.TEXT_ITEM_CONTROLS)
            .append(Component.text(" – Wield Blade", ColorConfig.TEXT_ITEM_BASE)),
        Component.text("Left Click × 3 (Standby)", ColorConfig.TEXT_ITEM_CONTROLS)
            .append(Component.text(" – Quick Attacks", ColorConfig.TEXT_ITEM_BASE)),
        Component.text("Drop + Left Click (× 3)", ColorConfig.TEXT_ITEM_CONTROLS)
            .append(Component.text(" – Heavy Sweep", ColorConfig.TEXT_ITEM_BASE)),
        Component.text("Drop + Right Click (Standby)", ColorConfig.TEXT_ITEM_CONTROLS)
            .append(Component.text(" – Lunge", ColorConfig.TEXT_ITEM_BASE)),
        Component.text("Swap + Left [combo]", ColorConfig.TEXT_ITEM_CONTROLS)
            .append(Component.text(" – Umbral Skills", ColorConfig.TEXT_ITEM_BASE)),

        Component.text("", ColorConfig.TEXT_ITEM_BASE),

        Component.text("→ Open Combat Reference for a full moveset guide.", ColorConfig.TEXT_ITEM_HEADER)
    );


    public static final ItemStack HOW_TO_PLAY_ITEM = ItemStackBuilder
        .of(Material.KNOWLEDGE_BOOK)
        .hideAll()
        .name(Component.text("Input Instructions", ColorConfig.TEXT_COOL, TextDecoration.BOLD))
        .lore(HOW_TO_PLAY)
        .build();

    /** Constructs the main navigation menu for the given player. */
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

        SimpleItem queueForRoguelike = new SimpleItem(
            new ItemBuilder(Material.WITHER_SKELETON_SKULL)
                .setDisplayName("Enter the Roguelike!"),
            click -> QueueManager.enqueue(
                RoguelikeRun.class, (SwordPlayer) SwordEntityArbiter.getOrAdd(click.getPlayer())
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
                .name(Component.text("Combat Reference", ColorConfig.TEXT_COOL, TextDecoration.BOLD))
                .lore(List.of(Component.text("View your full moveset and toggle movement inputs.", ColorConfig.TEXT_ITEM_BASE)))
                .build(),
            click -> InventoryMenuManager.openMenu(MovesetMenu.class, swordPlayer)
        );

        SimpleItem trashItem = new SimpleItem(
            new ItemStackBuilder(Material.BARRIER)
                .hideAll()
                .name(Component.text("Item Trash", ColorConfig.TEXT_COOL, TextDecoration.BOLD))
                .lore(List.of(
                    Component.text("Click with an item to destroy it.", ColorConfig.TEXT_ITEM_BASE),
                    Component.text("Shift + Left Click to clear your entire inventory.", ColorConfig.TEXT_ITEM_BASE)
                ))
                .build(),
            click -> {
                if (click.getClickType() == ClickType.SHIFT_LEFT) {
                    Player p = click.getPlayer();
                    for (int i = 0; i < 36; i++) {
                        ItemStack item = p.getInventory().getItem(i);
                        if (item != null && !item.isEmpty() && !NonMovableItem.isNonMovable(item)) {
                            p.getInventory().setItem(i, null);
                        }
                    }
                    return;
                }
                ItemStack cursor = click.getPlayer().getItemOnCursor();
                if (cursor.isEmpty() || NonMovableItem.isNonMovable(cursor)) return;
                click.getPlayer().setItemOnCursor(ItemStack.of(Material.AIR));
            }
        );

        Gui.Builder.Normal builder = Gui.normal()
            .setStructure(
                "# # # . . . # # #",
                "# . . . P . . . #",
                ". . . D Q R S . .",
                ". . . . . . . . .",
                "# T . . H . M . #",
                "< > # . V . # # #")
            .addIngredient('#', BORDER)
            .addIngredient('Q', queueForCTF)
            .addIngredient('R', queueForRoguelike)
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
                    .name(Component.text("Dev Menu", ColorConfig.TEXT_COOL, TextDecoration.BOLD))
                    .build(),
                click -> InventoryMenuManager.openMenu(DevMenu.class, swordPlayer)
            ));

            builder.addIngredient('S', new SimpleItem(
                new ItemStackBuilder(Material.RED_CONCRETE)
                    .name(Component.text("Stop Roguelike [DEV]", ColorConfig.TEXT_COOL, TextDecoration.BOLD))
                    .lore(List.of(
                        Component.text("Force-stops the active roguelike run.", ColorConfig.TEXT_ITEM_BASE),
                        Component.text("Despawns wave enemies and clears all players.", ColorConfig.TEXT_ITEM_BASE)
                    ))
                    .build(),
                click -> {
                    List<RoguelikeRun> runs = QueueManager.getActiveRoguelikeRuns();
                    if (runs.isEmpty()) {
                        SwordPlayer clicker = (SwordPlayer) SwordEntityArbiter.getOrAdd(click.getPlayer());
                        if (clicker != null) {
                            clicker.message("No active roguelike run found.");
                        }
                        return;
                    }
                    List.copyOf(runs).forEach(RoguelikeRun::stop);
                    SwordPlayer clicker = (SwordPlayer) SwordEntityArbiter.getOrAdd(click.getPlayer());
                    if (clicker != null) {
                        clicker.message("Roguelike stopped.");
                    }
                }
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
