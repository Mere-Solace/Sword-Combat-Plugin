package btm.sword.menu.dev;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedChatComponent;

import btm.sword.Sword;
import btm.sword.config.Config;
import btm.sword.config.section.AnimationConfig;
import btm.sword.entity.player.SwordPlayer;
import btm.sword.item.core.ItemStackBuilder;
import btm.sword.menu.Menu;
import btm.sword.menu.button.ConfigEntryItem;
import btm.sword.menu.button.ForwardItem;
import btm.sword.menu.button.PreviousItem;
import btm.sword.runtime.scheduler.SwordScheduler;
import btm.sword.scene.animation.AnimationDef;
import btm.sword.scene.animation.AnimationRegistry;
import btm.sword.scene.animation.DEUAnimationController;
import btm.sword.scene.animation.WorldAnimationController;
import btm.sword.scene.camera.CameraSystem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import xyz.xenondevs.invui.gui.PagedGui;
import xyz.xenondevs.invui.gui.structure.Markers;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.Window;

/**
 * Paged browser for all registered {@link AnimationDef} entries.
 * <p>
 * Each animation is presented as a clickable item with the following controls:
 * </p>
 * <ul>
 *   <li><b>Left-click</b> — play the animation without camera attachment.</li>
 *   <li><b>Right-click</b> — toggle the loop flag for this animation
 *       (persists to {@code animations.yml} immediately).</li>
 *   <li><b>Shift+Right-click</b> — play the animation with the DEU camera track attached.</li>
 * </ul>
 *
 * <p>The Stop button halts any active controller for the player.</p>
 *
 * <h2>Layout</h2>
 * <pre>
 *   # # # # S C # # #    (S = stop, C = convert &amp; register all)
 *   x x x x x x x x x
 *   x x x x x x x x x
 *   x x x x x x x x x
 *   B # # &lt; . &gt; # # #
 * </pre>
 */
public class AnimationBrowserMenu extends Menu {

    private static final String DEU_SAVE_CONFIRM = "Successfully saved display entity group locally";

    /**
     * Creates the animation browser for the given player.
     *
     * @param player the owning sword player
     */
    public AnimationBrowserMenu(SwordPlayer player) {
        super(player);
    }

    @Override
    public void open() {
        Player player = swordPlayer.player();

        List<Item> animItems = new ArrayList<>();
        for (AnimationDef def : AnimationRegistry.all()) {
            animItems.add(new SimpleItem(
                buildAnimItem(def),
                click -> {
                    ClickType type = click.getClickType();
                    if (type == ClickType.RIGHT) {
                        AnimationRegistry.setLoop(def.key(), !AnimationRegistry.isLooping(def.key()));
                        open();
                    } else if (type == ClickType.SHIFT_RIGHT) {
                        new DEUAnimationController(def, true, AnimationRegistry.isLooping(def.key())).start(swordPlayer);
                    } else if (type == ClickType.LEFT || type == ClickType.SHIFT_LEFT) {
                        new WorldAnimationController(def, AnimationRegistry.isLooping(def.key())).start(swordPlayer);
                    }
                }
            ));
        }

        if (animItems.isEmpty()) {
            animItems.add(new SimpleItem(
                new ItemStackBuilder(Material.BARRIER)
                    .name(Component.text("No animations registered", NamedTextColor.RED))
                    .lore(List.of(Component.text("Add entries to animations.yml", NamedTextColor.DARK_GRAY)))
                    .build()
            ));
        }

        SimpleItem stopButton = new SimpleItem(
            new ItemStackBuilder(Material.BARRIER)
                .name(Component.text("Stop", NamedTextColor.RED, TextDecoration.BOLD))
                .lore(List.of(Component.text("Stops the active animation / camera", NamedTextColor.DARK_GRAY)))
                .build(),
            click -> CameraSystem.stopController(swordPlayer)
        );

        List<String> unconverted = AnimationRegistry.getUnconvertedZipBases();
        SimpleItem convertButton = new SimpleItem(
            new ItemStackBuilder(Material.NETHER_STAR)
                .name(Component.text("Convert & Register All", NamedTextColor.YELLOW, TextDecoration.BOLD))
                .lore(unconverted.isEmpty()
                    ? List.of(Component.text("No unconverted packs found", NamedTextColor.DARK_GRAY))
                    : buildConvertLore(unconverted))
                .build(),
            click -> startConvertFlow(player, new ArrayList<>(AnimationRegistry.getUnconvertedZipBases()))
        );

        ConfigEntryItem conversionTimeout = new ConfigEntryItem(
            Config.entries().stream()
                .filter(e -> e.path().equals("animation.conversion_step_timeout_seconds"))
                .findFirst().orElseThrow(),
            swordPlayer,
            this::open
        );

        SimpleItem back = new SimpleItem(
            new ItemStackBuilder(Material.COPPER_TRAPDOOR)
                .name(Component.text("Back", NamedTextColor.GRAY))
                .build(),
            click -> new DEUBDEMenu(swordPlayer).open()
        );

        PagedGui<Item> gui = PagedGui.items()
            .setStructure(
                "S C V # # # # # #",
                "x x x x x x x x x",
                "x x x x x x x x x",
                "x x x x x x x x x",
                "B # # < . > # # #")
            .addIngredient('#', BORDER)
            .addIngredient('x', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
            .addIngredient('S', stopButton)
            .addIngredient('C', convertButton)
            .addIngredient('V', conversionTimeout)
            .addIngredient('B', back)
            .addIngredient('<', new PreviousItem())
            .addIngredient('>', new ForwardItem())
            .setContent(animItems)
            .build();

        Window window = Window.single()
            .setViewer(player)
            .setTitle("Animations  (" + animItems.size() + " registered)")
            .setGui(gui)
            .build();

        window.open();
    }

    /**
     * Converts packs sequentially: dispatches the first command, waits for DEU's chat
     * confirmation, then recurses for the next pack. A 15-second per-step timeout advances
     * to the next pack automatically if DEU does not confirm in time.
     *
     * @param player    the player to run commands as and notify
     * @param remaining mutable list of base names still to convert (consumed as we go)
     */
    private void startConvertFlow(Player player, List<String> remaining) {
        if (remaining.isEmpty()) {
            player.sendMessage(Component.text("[Sword] No unconverted packs found.", NamedTextColor.GRAY));
            return;
        }
        List<String> converted = new ArrayList<>();
        player.sendMessage(Component.text(
            "[Sword] Starting sequential conversion of " + remaining.size() + " pack(s)...",
            NamedTextColor.YELLOW));
        convertNext(player, remaining, converted);
    }

    /**
     * Fires the conversion command for the next pack in {@code remaining}, then installs a
     * one-shot ProtocolLib listener that advances to the following pack on DEU's
     * {@value #DEU_SAVE_CONFIRM} message. A 15-second timeout per step ensures forward
     * progress even if the message is never received.
     *
     * @param player    the player running the commands
     * @param remaining mutable list of base names still to convert
     * @param converted accumulator of successfully processed base names
     */
    private void convertNext(Player player, List<String> remaining, List<String> converted) {
        if (remaining.isEmpty()) {
            finishConversion(player, converted);
            return;
        }

        String base = remaining.removeFirst();
        converted.add(base);

        AtomicBoolean stepDone = new AtomicBoolean(false);
        ProtocolManager pm = ProtocolLibrary.getProtocolManager();

        PacketAdapter listener = new PacketAdapter(
            Sword.getInstance(), ListenerPriority.MONITOR, PacketType.Play.Server.SYSTEM_CHAT) {
            @Override
            public void onPacketSending(PacketEvent event) {
                if (stepDone.get()) return;
                if (!event.getPlayer().getUniqueId().equals(player.getUniqueId())) return;
                if (!extractMessage(event).contains(DEU_SAVE_CONFIRM)) return;

                if (stepDone.compareAndSet(false, true)) {
                    pm.removePacketListener(this);
                    SwordScheduler.runBukkitTask(() -> convertNext(player, remaining, converted));
                }
            }
        };

        pm.addPacketListener(listener);

        // Timeout per step — advance even if DEU never confirms.
        SwordScheduler.runBukkitTaskLater(() -> {
            if (stepDone.compareAndSet(false, true)) {
                pm.removePacketListener(listener);
                convertNext(player, remaining, converted);
            }
        }, AnimationConfig.ANIMATION_CONVERSION_STEP_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        player.sendMessage(Component.text(
            "[Sword] Converting: " + base + " (" + (converted.size()) + "/" + (converted.size() + remaining.size()) + ")...",
            NamedTextColor.YELLOW));
        player.performCommand("deu bdengine convertdp " + base + " " + base + " " + base);
    }

    /**
     * Scans the DEU animations directory, registers newly created entries, reloads the
     * registry, and refreshes this menu. Must be called on the main thread.
     *
     * @param player    the player to notify
     * @param converted base names that were processed
     */
    private void finishConversion(Player player, List<String> converted) {
        int count = 0;
        for (String base : converted) {
            count += AnimationRegistry.registerFromAnimDir(base).size();
        }
        AnimationRegistry.reload();
        player.sendMessage(Component.text(
            "[Sword] Done. Registered " + count + " animation(s) from " + converted.size() + " pack(s).",
            NamedTextColor.GREEN));
        open();
    }

    /**
     * Extracts the plain-text content of a {@code SYSTEM_CHAT} packet.
     * Tries {@link WrappedChatComponent} first; falls back to the raw string field.
     *
     * @param event the outbound packet event
     * @return the message content, or an empty string if unreadable
     */
    private static String extractMessage(PacketEvent event) {
        try {
            WrappedChatComponent comp = event.getPacket().getChatComponents().read(0);
            if (comp != null) return comp.getJson();
        } catch (Exception ignored) {}
        try {
            String s = event.getPacket().getStrings().read(0);
            if (s != null) return s;
        } catch (Exception ignored) {}
        return "";
    }

    private static List<Component> buildConvertLore(List<String> bases) {
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(bases.size() + " unconverted pack(s):", NamedTextColor.YELLOW));
        for (String base : bases) {
            lore.add(Component.text("  " + base, NamedTextColor.WHITE));
        }
        lore.add(Component.empty());
        lore.add(Component.text("Click to convert & register all", NamedTextColor.DARK_GRAY));
        return lore;
    }

    private static org.bukkit.inventory.ItemStack buildAnimItem(AnimationDef def) {
        boolean looping = def.defaultLoop();
        return new ItemStackBuilder(Material.RECOVERY_COMPASS)
            .name(Component.text(def.key(), NamedTextColor.AQUA, TextDecoration.BOLD))
            .lore(List.of(
                Component.text("Group: ", NamedTextColor.GRAY)
                    .append(Component.text(def.groupTag(), NamedTextColor.WHITE)),
                Component.text("Anim:  ", NamedTextColor.GRAY)
                    .append(Component.text(def.animTag(), NamedTextColor.WHITE)),
                Component.text("Loop:  ", NamedTextColor.GRAY)
                    .append(looping
                        ? Component.text("ON", NamedTextColor.GREEN, TextDecoration.BOLD)
                        : Component.text("OFF", NamedTextColor.RED)),
                Component.empty(),
                Component.text("Left-click        » play in world", NamedTextColor.DARK_GRAY),
                Component.text("Right-click       » toggle loop", NamedTextColor.DARK_GRAY),
                Component.text("Shift+Right-click » play + camera", NamedTextColor.DARK_GRAY)
            ))
            .build();
    }
}
