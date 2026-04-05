package btm.sword.system.inventory.menu.dev;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import btm.sword.config.Config;
import btm.sword.gamemode.QueueManager;
import btm.sword.gamemode.type.CaptureTheFlag1v1;
import btm.sword.system.control.SwordScheduler;
import btm.sword.system.display.BossBarManager;
import btm.sword.system.display.ScoreboardManager;
import btm.sword.system.display.SwordBossBar;
import btm.sword.system.display.SwordScoreboard;
import btm.sword.system.entity.impl.SwordPlayer;
import btm.sword.system.inventory.menu.Menu;
import btm.sword.system.inventory.menu.PacketTestMenu;
import btm.sword.system.item.ItemStackBuilder;
import btm.sword.system.item.weapon.WeaponType;
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

public class TestingMenu extends Menu {

    /**
     * Creates a Menu instance bound to the given player.
     *
     * @param player the player this menu belongs to
     */
    public TestingMenu(SwordPlayer player) {
        super(player);
    }

    @Override
    public void open() {
        Player player = swordPlayer.player();

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

        CaptureTheFlag1v1 activeCtf =
            QueueManager.getActiveCtfMatch(player.getUniqueId());
        SimpleItem ctfStop = new SimpleItem(
            new ItemStackBuilder(activeCtf != null ? Material.RED_BANNER : Material.GRAY_BANNER)
                .name(Component.text("Stop CTF Match", NamedTextColor.RED, TextDecoration.BOLD))
                .lore(List.of(
                    activeCtf != null
                        ? Component.text("Force-end your active CTF match", NamedTextColor.DARK_GRAY)
                        : Component.text("No active CTF match", NamedTextColor.DARK_GRAY)
                ))
                .build(),
            click -> {
                CaptureTheFlag1v1 match =
                    QueueManager.getActiveCtfMatch(click.getPlayer().getUniqueId());
                if (match == null) {
                    swordPlayer.message(Component.text("No active CTF match to stop.", NamedTextColor.RED));
                    return;
                }
                match.stop();
                swordPlayer.message(Component.text("CTF match stopped.", NamedTextColor.YELLOW));
            }
        );

        SimpleItem bossBarTest = new SimpleItem(
            new ItemStackBuilder(Material.ENDER_DRAGON_SPAWN_EGG)
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
            new ItemStackBuilder(hasScoreboard ? Material.GLOW_ITEM_FRAME : Material.ITEM_FRAME)
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

        SimpleItem hudOverrideTest = new SimpleItem(
            new ItemStackBuilder(Material.GOLDEN_CARROT)
                .name(Component.text("HUD Effect Test", NamedTextColor.GOLD, TextDecoration.BOLD))
                .lore(List.of(
                    Component.text("Cycles: wither (5s) → poison (5s) → hunger (5s) → bubbles (5s)", NamedTextColor.DARK_GRAY)
                ))
                .build(),
            click -> {
                swordPlayer.testHudSequence();
                swordPlayer.message(Component.text("HUD effect cycle started (20s total).", NamedTextColor.GREEN));
            }
        );

        SimpleItem smithingInteractionTest = new SimpleItem(
            new ItemStackBuilder(Material.SMITHING_TABLE)
                .name(Component.text("Smithing Refit Test", NamedTextColor.AQUA, TextDecoration.BOLD))
                .lore(List.of(
                    Component.text("Gives you a Falchion and opens smithing", NamedTextColor.DARK_GRAY),
                    Component.text("Place only the Falchion in the base slot", NamedTextColor.DARK_GRAY)
                ))
                .build(),
            click -> {
                ItemStack falchion = WeaponType.FALCHION.buildItemStack();
                player.getInventory().addItem(falchion);
                player.openSmithingTable(null, true); // TODO: Deprecated!!!
                swordPlayer.message(Component.text("Smithing test ready: place the Falchion into the base slot.", NamedTextColor.GREEN));
            }
        );

        Gui gui = Gui.normal()
            .setStructure(
                "# # # . . . # # #",
                "# P X . F . B K #",
                ". D . H . M . V .",
                "# O . . . . . . #",
                "< > # . . . # # #")
            .addIngredient('#', BORDER)
            .addIngredient('P', packetTests)
            .addIngredient('S', staticScene)
            .addIngredient('F', fakePlayerScene)
            .addIngredient('X', joinCutscene)
            .addIngredient('B', bossBarTest)
            .addIngredient('K', scoreboardTest)
            .addIngredient('D', ctfDebug)
            .addIngredient('H', hudOverrideTest)
            .addIngredient('M', smithingInteractionTest)
            .addIngredient('O', ctfStop)
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
}
