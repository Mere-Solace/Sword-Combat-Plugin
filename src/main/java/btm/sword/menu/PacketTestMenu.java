package btm.sword.menu;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import btm.sword.entity.player.SwordPlayer;
import btm.sword.item.core.ItemStackBuilder;
import btm.sword.scene.fake.FakePlayerPacketTester;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.Window;

/**
 * Developer sub-menu with one looping test button per {@link FakePlayerPacketTester} packet type.
 *
 * <p>Clicking an individual test button starts a repeating loop (every 2 seconds) that
 * sends that packet to a fake player NPC spawned 2 blocks in front of the viewer.
 * If a loop is already running for that test, clicking it again stops it.
 * The "Stop" button cancels any active loop and despawns the NPC immediately.</p>
 *
 * <p>The "Run All" button runs all six packets once in sequence and despawns the NPC
 * when finished — useful for a quick sanity check.</p>
 *
 * <p>All results and stack traces are logged to the server console.</p>
 */
public class PacketTestMenu extends Menu {

    /**
     * Creates a new PacketTestMenu for the given player.
     *
     * @param player the player opening this menu
     */
    public PacketTestMenu(SwordPlayer player) {
        super(player);
    }

    @Override
    public void open() {
        Player player = swordPlayer.player();
        boolean looping = FakePlayerPacketTester.hasActiveLoop(swordPlayer);

        SimpleItem moveRelative = loopButton(
            Material.SLIME_BLOCK,
            "REL_ENTITY_MOVE",
            "Move NPC +0.5 on X axis (relative)",
            () -> FakePlayerPacketTester.loopMoveRelative(swordPlayer)
        );
        SimpleItem teleport = loopButton(
            Material.ENDER_PEARL,
            "ENTITY_TELEPORT",
            "Snap NPC +1 block upward (absolute)",
            () -> FakePlayerPacketTester.loopTeleport(swordPlayer)
        );
        SimpleItem rotate = loopButton(
            Material.COMPASS,
            "ENTITY_LOOK",
            "Rotate NPC to 90\u00b0 yaw, 20\u00b0 pitch",
            () -> FakePlayerPacketTester.loopRotate(swordPlayer)
        );
        SimpleItem animate = loopButton(
            Material.STICK,
            "ANIMATION",
            "Trigger main-hand swing every 2s",
            () -> FakePlayerPacketTester.loopAnimate(swordPlayer)
        );
        SimpleItem velocity = loopButton(
            Material.FEATHER,
            "ENTITY_VELOCITY",
            "Send upward velocity impulse every 2s",
            () -> FakePlayerPacketTester.loopVelocity(swordPlayer)
        );
        SimpleItem flags = loopButton(
            Material.SHIELD,
            "ENTITY_DATA (flags)",
            "Re-apply blocking pose every 2s",
            () -> FakePlayerPacketTester.loopEntityFlags(swordPlayer)
        );

        SimpleItem stop = new SimpleItem(
            new ItemStackBuilder(looping ? Material.BARRIER : Material.GRAY_DYE)
                .name(looping
                    ? Component.text("Stop Loop", NamedTextColor.RED, TextDecoration.BOLD)
                    : Component.text("Stop Loop", NamedTextColor.DARK_GRAY))
                .lore(List.of(Component.text(
                    looping ? "Click to cancel the active loop" : "No loop is running",
                    NamedTextColor.DARK_GRAY)))
                .build(),
            click -> {
                FakePlayerPacketTester.stopLoop(swordPlayer);
                swordPlayer.message(Component.text("[PacketTester] Loop stopped.", NamedTextColor.YELLOW));
                open(); // refresh to update stop button state
            }
        );

        SimpleItem runAll = new SimpleItem(
            new ItemStackBuilder(Material.REDSTONE_TORCH)
                .name(Component.text("Run All (once)", NamedTextColor.YELLOW, TextDecoration.BOLD))
                .lore(List.of(Component.text("Send all 6 packets once, then despawn NPC", NamedTextColor.DARK_GRAY)))
                .build(),
            click -> {
                boolean allPassed = FakePlayerPacketTester.runAll(swordPlayer);
                swordPlayer.message(allPassed
                    ? Component.text("[PacketTester] All 6 passed. See console.", NamedTextColor.GREEN)
                    : Component.text("[PacketTester] Some tests failed. See console.", NamedTextColor.RED));
            }
        );

        Gui gui = Gui.normal()
            .setStructure(
                "# # # # # # # # #",
                "# M T R A V F . #",
                "# # S X < # # # #")
            .addIngredient('#', BORDER)
            .addIngredient('M', moveRelative)
            .addIngredient('T', teleport)
            .addIngredient('R', rotate)
            .addIngredient('A', animate)
            .addIngredient('V', velocity)
            .addIngredient('F', flags)
            .addIngredient('S', stop)
            .addIngredient('X', runAll)
            .addIngredient('<', generatePreviousButtonOrDefault())
            .build();

        Window window = Window.single()
            .setViewer(player)
            .setTitle("Packet Tests")
            .setGui(gui)
            .build();

        window.open();
    }

    /**
     * Builds a loop toggle button. Clicking it starts the loop if none is active,
     * or stops the current loop (regardless of which test is running) if one is active.
     *
     * @param icon    the item material used as the button icon
     * @param label   short packet/test name shown in the item name and result message
     * @param desc    one-line description of what the test does, shown in the lore
     * @param startFn the action to run to start this test's loop
     * @return a ready-to-use {@link SimpleItem}
     */
    private SimpleItem loopButton(Material icon, String label, String desc, Runnable startFn) {
        return new SimpleItem(
            new ItemStackBuilder(icon)
                .name(Component.text(label, NamedTextColor.YELLOW, TextDecoration.BOLD))
                .lore(List.of(Component.text(desc, NamedTextColor.DARK_GRAY)))
                .build(),
            click -> {
                if (FakePlayerPacketTester.hasActiveLoop(swordPlayer)) {
                    FakePlayerPacketTester.stopLoop(swordPlayer);
                    swordPlayer.message(Component.text(
                        "[PacketTester] Loop stopped.", NamedTextColor.YELLOW));
                } else {
                    startFn.run();
                    swordPlayer.message(Component.text(
                        "[PacketTester] Loop started: " + label, NamedTextColor.GREEN));
                }
                open(); // refresh stop button state
            }
        );
    }
}
