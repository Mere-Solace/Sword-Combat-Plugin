package btm.sword.system.inventory.menu;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import btm.sword.config.Config;
import btm.sword.gamemode.QueueManager;
import btm.sword.system.control.SwordScheduler;
import btm.sword.system.display.BossBarManager;
import btm.sword.system.display.ScoreboardManager;
import btm.sword.system.display.SwordBossBar;
import btm.sword.system.display.SwordScoreboard;
import btm.sword.system.entity.impl.SwordPlayer;
import btm.sword.system.item.ItemStackBuilder;
import btm.sword.system.scene.DEUAnimationController;
import btm.sword.system.scene.SceneManager;
import btm.sword.system.scene.animation.AnimationDef;
import btm.sword.system.scene.animation.AnimationRegistry;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.Window;

/**
 * In-game developer menu providing access to diagnostic utilities, dev tools, and the
 * {@link TogglesMenu} for runtime boolean flags. Accessible from the {@link MainMenu} for op players.
 * <p>
 * All changes take effect immediately without a server restart and are not persisted across restarts.
 * </p>
 */
public class DevMenu extends Menu {

    /**
     * Creates a new DevMenu for the given player.
     *
     * @param player the player opening this menu
     */
    public DevMenu(SwordPlayer player) {
        super(player);
    }

    @Override
    public void open() {
        Player player = swordPlayer.player();

        SimpleItem toggles = new SimpleItem(
            new ItemStackBuilder(Material.LEVER)
                .name(Component.text("Toggles", NamedTextColor.YELLOW, TextDecoration.BOLD))
                .lore(List.of(Component.text("Debug flags and world toggles", NamedTextColor.DARK_GRAY)))
                .build(),
            click -> new TogglesMenu(swordPlayer).open()
        );

        SimpleItem reloadProfile = new SimpleItem(
            new ItemStackBuilder(Material.RECOVERY_COMPASS)
                .name(Component.text("Reload Combat Profile", NamedTextColor.AQUA, TextDecoration.BOLD))
                .build(),
            click -> {
                swordPlayer.getCombatProfile().reloadFromConfig();
                swordPlayer.getAspects().reloadFromProfile(swordPlayer.getCombatProfile());
                swordPlayer.message(Component.text("Combat profile reloaded from config.", NamedTextColor.GREEN));
            }
        );

        SimpleItem configEditor = new SimpleItem(
            new ItemStackBuilder(Material.COMPARATOR)
                .name(Component.text("Config Editor", NamedTextColor.GOLD, TextDecoration.BOLD))
                .build(),
            click -> new ConfigMenu(swordPlayer).open()
        );

        SimpleItem woodenAxe = giveItem(Material.WOODEN_AXE, "Wooden Axe");
        SimpleItem witherSkeletonEgg = giveItem(Material.WITHER_SKELETON_SPAWN_EGG, "Wither Skeleton Spawn Egg");

        SimpleItem creativeMode;
        if (swordPlayer.isInCreativeDevMode()) {
            creativeMode = new SimpleItem(
                new ItemStackBuilder(Material.GRASS_BLOCK)
                    .name(Component.text("Return to Survival", NamedTextColor.GREEN, TextDecoration.BOLD))
                    .lore(List.of(Component.text("Restore inventory and re-enable special items", NamedTextColor.DARK_GRAY)))
                    .build(),
                click -> {
                    swordPlayer.exitCreativeDevMode();
                    new DevMenu(swordPlayer).open();
                }
            );
        } else {
            creativeMode = new SimpleItem(
                new ItemStackBuilder(Material.DIAMOND_PICKAXE)
                    .name(Component.text("Enter Creative Mode", NamedTextColor.AQUA, TextDecoration.BOLD))
                    .lore(List.of(Component.text("Save inventory, disable special items, enable block placing", NamedTextColor.DARK_GRAY)))
                    .build(),
                click -> {
                    swordPlayer.enterCreativeDevMode();
                    new DevMenu(swordPlayer).open();
                }
            );
        }

        SimpleItem creativeInventory = new SimpleItem(
            new ItemStackBuilder(Material.CHEST)
                .name(Component.text("Creative Inventory", NamedTextColor.YELLOW, TextDecoration.BOLD))
                .build(),
            click -> new CreativeInventoryMenu(swordPlayer).open()
        );

        SimpleItem reloadInventoryButtons = new SimpleItem(
            new ItemStackBuilder(Material.CLOCK)
                .name(Component.text("Reload Inventory Buttons", NamedTextColor.AQUA, TextDecoration.BOLD))
                .build(),
            click -> {
                swordPlayer.reloadInventoryButtons();
                swordPlayer.message(Component.text("Inventory buttons reloaded.", NamedTextColor.GREEN));
            }
        );

        SimpleItem staticScene = new SimpleItem(
            new ItemStackBuilder(Material.SPYGLASS)
                .name(Component.text("Static Scene Test", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD))
                .lore(List.of(Component.text("Play: " + Config.Animation.STATIC_MENU_ANIMATION_KEY, NamedTextColor.DARK_GRAY)))
                .build(),
            click -> {
                String animKey = Config.Animation.STATIC_MENU_ANIMATION_KEY;
                AnimationDef def = AnimationRegistry.get(animKey).orElse(null);
                if (def == null) {
                    swordPlayer.message(Component.text("Animation not found: " + animKey, NamedTextColor.RED));
                    return;
                }
                new DEUAnimationController(def, true, true).start(swordPlayer);
            }
        );

        double fakePlayerDist = Config.Scene.FAKE_PLAYER_DISTANCE;
        String animKeyLabel = Config.Animation.STATIC_MENU_ANIMATION_KEY;
        SimpleItem fakePlayerScene = new SimpleItem(
            new ItemStackBuilder(Material.PLAYER_HEAD)
                .name(Component.text("Fake Player Scene Test", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD))
                .lore(List.of(
                    Component.text("Spawn NPC " + fakePlayerDist + " blocks ahead, fixed camera", NamedTextColor.DARK_GRAY),
                    Component.text("Animation: " + animKeyLabel, NamedTextColor.DARK_GRAY)
                ))
                .build(),
            click -> {
                Location playerLoc = swordPlayer.player().getLocation();
                Location displayPosition = playerLoc.clone()
                    .add(playerLoc.getDirection().multiply(Config.Scene.FAKE_PLAYER_DISTANCE));
                SceneManager.enterStaticMenuScene(swordPlayer, displayPosition);
            }
        );

        SimpleItem deuTools = new SimpleItem(
            new ItemStackBuilder(Material.ITEM_FRAME)
                .name(Component.text("DisplayEntityUtils", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD))
                .lore(List.of(Component.text("Animations, groups, despawn tools", NamedTextColor.DARK_GRAY)))
                .build(),
            click -> new DEUBDEMenu(swordPlayer).open()
        );

        SimpleItem packetTests = new SimpleItem(
            new ItemStackBuilder(Material.COMMAND_BLOCK)
                .name(Component.text("Packet Tests", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD))
                .lore(List.of(Component.text("Fake player packet test harness", NamedTextColor.DARK_GRAY)))
                .build(),
            click -> new PacketTestMenu(swordPlayer).open()
        );

        SimpleItem joinCutscene = new SimpleItem(
            new ItemStackBuilder(Material.LIGHTNING_ROD)
                .name(Component.text("Initial Join Cutscene", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD))
                .lore(List.of(Component.text("Play the first-time join sequence", NamedTextColor.DARK_GRAY)))
                .build(),
            click -> btm.sword.system.join.InitialJoinCutscene.play(swordPlayer)
        );

        SimpleItem skillEquip = new SimpleItem(
            new ItemStackBuilder(Material.KNOWLEDGE_BOOK)
                .name(Component.text("Skill Equip", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD))
                .lore(List.of(Component.text("Force-equip any skill to any slot", NamedTextColor.DARK_GRAY)))
                .build(),
            click -> new DevSkillEquipMenu(swordPlayer).open()
        );

        SimpleItem itemLibrary = new SimpleItem(
            new ItemStackBuilder(Material.BOOKSHELF)
                .name(Component.text("Item Library", NamedTextColor.GOLD, TextDecoration.BOLD))
                .lore(List.of(Component.text("Browse all registered game items", NamedTextColor.DARK_GRAY)))
                .build(),
            click -> new ItemLibraryMenu(swordPlayer).open()
        );

        SimpleItem weaponDisplay = new SimpleItem(
            new ItemStackBuilder(Material.BLAZE_ROD)
                .name(Component.text("Weapon Display", NamedTextColor.GOLD, TextDecoration.BOLD))
                .lore(List.of(
                    Component.text("Tweak per-material weapon slot transforms", NamedTextColor.DARK_GRAY),
                    Component.text("Hold the item you want to configure", NamedTextColor.DARK_GRAY)
                ))
                .build(),
            click -> new WeaponDisplayEditorMenu(swordPlayer).open()
        );

        SimpleItem ctfDebug = new SimpleItem(
            new ItemStackBuilder(Material.WHITE_BANNER)
                .name(Component.text("Start CTF (Solo Debug)", NamedTextColor.AQUA, TextDecoration.BOLD))
                .lore(List.of(
                    Component.text("Launch a CTF instance instantly", NamedTextColor.DARK_GRAY),
                    Component.text("Bypasses 2-player queue requirement", NamedTextColor.DARK_GRAY)
                ))
                .build(),
            click -> {
                QueueManager.startCtfDebug(swordPlayer);
                swordPlayer.message(Component.text("CTF debug session started!", NamedTextColor.GREEN));
            }
        );

        SimpleItem bossBarTest = new SimpleItem(
            new ItemStackBuilder(Material.ORANGE_BANNER)
                .name(Component.text("Boss Bar Fill Test", NamedTextColor.GOLD, TextDecoration.BOLD))
                .lore(List.of(Component.text("Spawns a bar that fills over 5 seconds", NamedTextColor.DARK_GRAY)))
                .build(),
            click -> {
                SwordBossBar bar = BossBarManager.create(
                    Component.text("Charging...", NamedTextColor.YELLOW),
                    0f,
                    BossBar.Color.YELLOW,
                    BossBar.Overlay.NOTCHED_10
                );
                bar.addViewer(player);
                int steps = 20;
                int intervalMs = 250;
                for (int i = 1; i <= steps; i++) {
                    final float progress = (float) i / steps;
                    SwordScheduler.runBukkitTaskLater(() -> bar.setProgress(progress), i * intervalMs, TimeUnit.MILLISECONDS);
                }
                SwordScheduler.runBukkitTaskLater(() -> {
                    bar.setTitle(Component.text("Charged!", NamedTextColor.GREEN));
                    bar.setColor(BossBar.Color.GREEN);
                }, steps * intervalMs, TimeUnit.MILLISECONDS);
                SwordScheduler.runBukkitTaskLater(bar::remove, steps * intervalMs + 1000, TimeUnit.MILLISECONDS);
            }
        );

        boolean hasScoreboard = ScoreboardManager.get(player).isPresent();
        SimpleItem scoreboardTest = new SimpleItem(
            new ItemStackBuilder(hasScoreboard ? Material.LIME_BANNER : Material.WHITE_BANNER)
                .name(Component.text("Scoreboard Test", NamedTextColor.GREEN, TextDecoration.BOLD))
                .lore(List.of(Component.text(
                    hasScoreboard ? "Click to remove the test scoreboard" : "Click to show a test scoreboard",
                    NamedTextColor.DARK_GRAY
                )))
                .build(),
            click -> {
                Optional<SwordScoreboard> existing = ScoreboardManager.get(player);
                if (existing.isPresent()) {
                    ScoreboardManager.remove(player);
                    swordPlayer.message(Component.text("Scoreboard removed.", NamedTextColor.YELLOW));
                } else {
                    SwordScoreboard board = ScoreboardManager.create(
                        player,
                        Component.text("✦ Sword Test", NamedTextColor.GOLD, TextDecoration.BOLD)
                    );
                    board.setLine(1, Component.text("HP", NamedTextColor.RED)
                        .append(Component.text(": 100", NamedTextColor.WHITE)));
                    board.setLine(2, Component.empty());
                    board.setLine(3, Component.text("Soulfire", NamedTextColor.AQUA)
                        .append(Component.text(": 75", NamedTextColor.WHITE)));
                    board.setLine(4, Component.empty());
                    board.setLine(5, Component.text("Toughness", NamedTextColor.GRAY)
                        .append(Component.text(": 50", NamedTextColor.WHITE)));
                    board.show();
                    swordPlayer.message(Component.text("Scoreboard shown. Click again to remove.", NamedTextColor.GREEN));
                }
            }
        );

        Gui gui = Gui.normal()
            .setStructure(
                "# # # # # # # # #",
                "# J N S F A L C #",
                "# P X B K D H E #",
                "# R I . T . M W #",
                "# # # < . > # # #")
            .addIngredient('#', BORDER)
            .addIngredient('T', toggles)
            .addIngredient('P', packetTests)
            .addIngredient('R', reloadInventoryButtons)
            .addIngredient('J', configEditor)
            .addIngredient('N', deuTools)
            .addIngredient('S', staticScene)
            .addIngredient('F', fakePlayerScene)
            .addIngredient('A', skillEquip)
            .addIngredient('L', itemLibrary)
            .addIngredient('X', joinCutscene)
            .addIngredient('B', bossBarTest)
            .addIngredient('K', scoreboardTest)
            .addIngredient('D', ctfDebug)
            .addIngredient('C', creativeInventory)
            .addIngredient('W', woodenAxe)
            .addIngredient('E', witherSkeletonEgg)
            .addIngredient('I', reloadProfile)
            .addIngredient('M', creativeMode)
            .addIngredient('H', weaponDisplay)
            .addIngredient('<', generatePreviousButtonOrDefault())
            .addIngredient('>', generateForwardPreviousButtonOrDefault())
            .build();

        Window window = Window.single()
            .setViewer(player)
            .setTitle("Dev Menu")
            .setGui(gui)
            .build();

        window.open();
    }

    /**
     * Builds a {@link SimpleItem} that gives the player one stack of the given material on click.
     *
     * @param material the material to give
     * @param label    display name for the item button
     * @return a {@link SimpleItem} that gives the item on click
     */
    private SimpleItem giveItem(Material material, String label) {
        return new SimpleItem(
            new ItemStackBuilder(material)
                .name(Component.text(label, NamedTextColor.WHITE))
                .build(),
            click -> click.getPlayer().getInventory().addItem(new ItemStack(material))
        );
    }
}
