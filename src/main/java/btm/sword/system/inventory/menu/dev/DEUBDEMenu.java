package btm.sword.system.inventory.menu.dev;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import btm.sword.system.entity.SwordEntityArbiter;
import btm.sword.system.entity.base.SwordEntity;
import btm.sword.system.entity.impl.SwordPlayer;
import btm.sword.system.inventory.menu.Menu;
import btm.sword.system.item.ItemStackBuilder;
import btm.sword.system.scene.CameraSystem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.Window;

/**
 * Dev hub for DisplayEntityUtils tools.
 *
 * <ul>
 *   <li><b>Animations</b> — browse and play registered animations.</li>
 *   <li><b>Spawn Groups</b> — browse and spawn raw DEU groups at the player's location.</li>
 *   <li><b>Cancel All</b> — stops every active camera controller for all online players.</li>
 *   <li><b>Despawn All</b> — removes every {@link Display} entity from all loaded worlds.</li>
 * </ul>
 *
 * <h2>Layout</h2>
 * <pre>
 *   # # # # # # # # #
 *   # A # G # K # D #    A=Animations, G=Spawn Groups, K=Cancel All, D=Despawn All
 *   # # # # # # # # #
 *   # # # # # # # # #
 *   B # # # # # # # #
 * </pre>
 */
public class DEUBDEMenu extends Menu {

    /**
     * Creates a DEU hub menu for the given player.
     *
     * @param player the owning sword player
     */
    public DEUBDEMenu(SwordPlayer player) {
        super(player);
    }

    @Override
    public void open() {
        Player player = swordPlayer.player();

        SimpleItem animations = new SimpleItem(
            new ItemStackBuilder(Material.RECOVERY_COMPASS)
                .name(Component.text("Animations", NamedTextColor.AQUA, TextDecoration.BOLD))
                .lore(List.of(Component.text("Browse and play registered animations", NamedTextColor.DARK_GRAY)))
                .build(),
            click -> new AnimationBrowserMenu(swordPlayer).open()
        );

        SimpleItem spawnGroups = new SimpleItem(
            new ItemStackBuilder(Material.ITEM_FRAME)
                .name(Component.text("Spawn Groups", NamedTextColor.GREEN, TextDecoration.BOLD))
                .lore(List.of(Component.text("Browse and spawn raw DEU groups", NamedTextColor.DARK_GRAY)))
                .build(),
            click -> new DeuGroupBrowserMenu(swordPlayer).open()
        );

        SimpleItem cancelAll = new SimpleItem(
            new ItemStackBuilder(Material.BARRIER)
                .name(Component.text("Cancel All Animations", NamedTextColor.RED, TextDecoration.BOLD))
                .lore(List.of(Component.text("Stops all active camera controllers", NamedTextColor.DARK_GRAY)))
                .build(),
            click -> {
                int stopped = 0;
                for (Player online : Bukkit.getOnlinePlayers()) {
                    SwordEntity entity = SwordEntityArbiter.getOrAdd(online);
                    if (entity instanceof SwordPlayer sp && CameraSystem.hasActiveController(sp)) {
                        CameraSystem.stopController(sp);
                        stopped++;
                    }
                }
                player.sendMessage(Component.text(
                    "[Sword] Stopped " + stopped + " active controller(s).", NamedTextColor.YELLOW));
            }
        );

        SimpleItem despawnAll = new SimpleItem(
            new ItemStackBuilder(Material.TNT)
                .name(Component.text("Despawn All Displays", NamedTextColor.RED, TextDecoration.BOLD))
                .lore(List.of(
                    Component.text("Removes all Display entities from all worlds", NamedTextColor.DARK_GRAY),
                    Component.text("(ItemDisplay, BlockDisplay, TextDisplay)", NamedTextColor.DARK_GRAY)))
                .build(),
            click -> {
                int removed = 0;
                for (World world : Bukkit.getWorlds()) {
                    for (Entity entity : world.getEntities()) {
                        if (entity instanceof Display) {
                            entity.remove();
                            removed++;
                        }
                    }
                }
                player.sendMessage(Component.text(
                    "[Sword] Removed " + removed + " display entity(s).", NamedTextColor.YELLOW));
            }
        );

        SimpleItem back = new SimpleItem(
            new ItemStackBuilder(Material.COPPER_TRAPDOOR)
                .name(Component.text("Back", NamedTextColor.GRAY))
                .build(),
            click -> new DevMenu(swordPlayer).open()
        );

        Gui gui = Gui.normal()
            .setStructure(
                "# # # # # # # # #",
                "# A G . K D . . #",
                "B # # # # # # # #")
            .addIngredient('#', BORDER)
            .addIngredient('A', animations)
            .addIngredient('G', spawnGroups)
            .addIngredient('K', cancelAll)
            .addIngredient('D', despawnAll)
            .addIngredient('B', back)
            .build();

        Window window = Window.single()
            .setViewer(player)
            .setTitle("DisplayEntityUtils")
            .setGui(gui)
            .build();

        window.open();
    }
}
