package btm.sword.menu.dev;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;

import btm.sword.Sword;
import btm.sword.combat.def.AttackDefSerializer;
import btm.sword.combat.def.AttackInstance;
import btm.sword.combat.def.AttackRegistry;
import btm.sword.combat.dev.AnimationMode;
import btm.sword.combat.dev.AttackDevSession;
import btm.sword.combat.dev.DevMode;
import btm.sword.combat.simulation.ControlPointSequence;
import btm.sword.combat.simulation.KeyframedSequence;
import btm.sword.combat.simulation.SweepSequence;
import btm.sword.config.section.DebugConfig;
import btm.sword.entity.player.DevSwordPlayer;
import btm.sword.entity.player.SwordPlayer;
import btm.sword.item.core.ItemStackBuilder;
import btm.sword.menu.Menu;
import btm.sword.menu.button.ForwardItem;
import btm.sword.menu.button.PreviousItem;
import btm.sword.util.misc.ChatInputCapture;
import btm.sword.util.prefab.Prefab;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import xyz.xenondevs.invui.gui.PagedGui;
import xyz.xenondevs.invui.gui.structure.Markers;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.Window;

/**
 * Dev-only attack browser menu.
 *
 * <p>Lists all attacks currently registered in {@link btm.sword.combat.def.AttackRegistry}.
 * Clicking a VOLUME attack opens {@link AttackEditorMenu} with that attack loaded into the editing
 * session. SWEEP attacks are shown but cannot be edited via the menu.</p>
 *
 * <p>A "New Recording" button closes the menu and prompts the player to use the volume-attack
 * wand (SHIFT+LEFT) to begin capturing a new sweep path.</p>
 */
public class AttackBrowserMenu extends Menu {

    /**
     * Creates a new browser for the given player.
     *
     * @param player the player opening this menu
     */
    public AttackBrowserMenu(SwordPlayer player) {
        super(player);
    }

    @Override
    public void open() {
        // Sync registry with the attacks directory so the list is always up-to-date
        AttackRegistry.syncDirectory(new File(Sword.getInstance().getDataFolder(), "attacks"));

        // Stop any active viewing session so the player can re-open the browser cleanly
        AttackDevSession existing = AttackDevSession.get(swordPlayer.player().getUniqueId());
        if (existing != null && existing.getMode() == DevMode.VIEWING) {
            existing.stopViewing();
        }

        List<Item> attackItems = buildAttackItems();

        SimpleItem back = new SimpleItem(
            new ItemStackBuilder(Material.COPPER_TRAPDOOR)
                .name(Component.text("Back", NamedTextColor.GRAY))
                .build(),
            click -> new DevMenu(swordPlayer).open()
        );

        SimpleItem newRecording = new SimpleItem(
            new ItemStackBuilder(Material.BLAZE_ROD)
                .name(Component.text("New Recording", NamedTextColor.GREEN, TextDecoration.BOLD))
                .lore(List.of(
                    Component.text("Hold the volume wand and press", NamedTextColor.DARK_GRAY),
                    Component.text("SHIFT+LEFT to start recording.", NamedTextColor.DARK_GRAY)
                ))
                .build(),
            click -> {
                swordPlayer.player().closeInventory();
                swordPlayer.message(Component.text(
                    "[Dev] Hold the volume-attack wand and press SHIFT+LEFT to start recording.",
                    NamedTextColor.YELLOW));
            }
        );

        SimpleItem giveWand = giveItem(Prefab.Items.testVolumeWand());

        SimpleItem showHitboxes = toggle(
            "Hitbox Outlines (live attacks)",
            () -> DebugConfig.VISUALIZATION_SHOW_HITBOXES,
            () -> DebugConfig.VISUALIZATION_SHOW_HITBOXES = !DebugConfig.VISUALIZATION_SHOW_HITBOXES
        );

        PagedGui<Item> gui = PagedGui.items()
            .setStructure(
                "P # # # # # # # #",
                "x x x x x x x x x",
                "x x x x x x x x x",
                "N G H < # > # # #")
            .addIngredient('#', BORDER)
            .addIngredient('P', back)
            .addIngredient('N', newRecording)
            .addIngredient('G', giveWand)
            .addIngredient('H', showHitboxes)
            .addIngredient('<', new PreviousItem())
            .addIngredient('>', new ForwardItem())
            .addIngredient('x', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
            .setContent(attackItems)
            .build();

        Window.single()
            .setViewer(swordPlayer.player())
            .setTitle("Attack Browser")
            .setGui(gui)
            .build()
            .open();
    }

    private void openRenamePrompt(AttackInstance def) {
        ChatInputCapture.prompt(
            swordPlayer.player(),
            Component.text("[Dev] Enter new name for '" + def.getId() + "':", NamedTextColor.YELLOW),
            input -> {
                if (input.equalsIgnoreCase("cancel")) {
                    new AttackBrowserMenu(swordPlayer).open();
                    return;
                }
                String newId = input.trim().toLowerCase().replace(' ', '_');
                if (newId.isEmpty() || newId.equals(def.getId())) {
                    new AttackBrowserMenu(swordPlayer).open();
                    return;
                }
                if (AttackRegistry.contains(newId)) {
                    swordPlayer.message(Component.text("[Dev] '" + newId + "' is already taken.", NamedTextColor.RED));
                    new AttackBrowserMenu(swordPlayer).open();
                    return;
                }
                performRename(def, newId);
            });
    }

    private void performRename(AttackInstance old, String newId) {
        File attacksDir = new File(Sword.getInstance().getDataFolder(), "attacks");
        attacksDir.mkdirs();
        try {
            AttackInstance renamed = rebuildWithId(old, newId);

            File newFile = new File(attacksDir, newId + ".yml");
            AttackDefSerializer.save(newFile, renamed);

            File oldFile = new File(attacksDir, old.getId() + ".yml");
            if (oldFile.exists() && !oldFile.getCanonicalPath().equals(newFile.getCanonicalPath())) {
                oldFile.delete();
            }

            AttackRegistry.unregister(old.getId());
            AttackRegistry.register(renamed);

            AttackDevSession session = AttackDevSession.get(swordPlayer.player().getUniqueId());
            if (session != null && old.getId().equals(session.getCurrentAttackName())) {
                session.renameCurrentAttack(newId);
            }

            swordPlayer.message(Component.text(
                "[Dev] Renamed '" + old.getId() + "' → '" + newId + "'.", NamedTextColor.GREEN));
        } catch (Exception e) {
            swordPlayer.message(Component.text("[Dev] Rename failed: " + e.getMessage(), NamedTextColor.RED));
        }
        new AttackBrowserMenu(swordPlayer).open();
    }

    private static AttackInstance rebuildWithId(AttackInstance old, String newId) {
        AttackInstance.Builder b = new AttackInstance.Builder(newId)
            .duration(old.getDurationMs())
            .onHit(old.getHitValue())
            .orientWithPitch(old.isOrientWithPitch())
            .lockOriginOnFire(old.isLockOriginOnFire());
        switch (old.getType()) {
            case VOLUME -> b.keyframes(((KeyframedSequence) old.getTrajectory()).keyframes());
            case SWEEP -> b.sweep(((SweepSequence) old.getTrajectory()).getCurve());
            case CTRL_POINT -> {
                ControlPointSequence cps = (ControlPointSequence) old.getTrajectory();
                b.controlPoints(cps.getPoints(), cps.getMode());
            }
        }
        return b.build();
    }

    private List<Item> buildAttackItems() {
        List<AttackInstance> sorted = AttackRegistry.getAll().stream()
            .sorted(Comparator.comparing(AttackInstance::getId))
            .toList();

        List<Item> items = new ArrayList<>(sorted.size());
        for (AttackInstance def : sorted) {
            items.add(buildAttackItem(def));
        }
        return items;
    }

    private Item buildAttackItem(AttackInstance def) {
        boolean editable = def.getTrajectory() instanceof KeyframedSequence;
        int frameCount = editable
            ? ((KeyframedSequence) def.getTrajectory()).keyframes().size()
            : -1;

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Type: ", NamedTextColor.DARK_GRAY)
            .append(Component.text(def.getType().name(), NamedTextColor.GRAY)));
        lore.add(Component.text("Duration: ", NamedTextColor.DARK_GRAY)
            .append(Component.text(def.getDurationMs() + "ms", NamedTextColor.GRAY)));
        if (frameCount >= 0) {
            lore.add(Component.text("Keyframes: ", NamedTextColor.DARK_GRAY)
                .append(Component.text(String.valueOf(frameCount), NamedTextColor.GRAY)));
        }
        lore.add(Component.empty());
        if (editable) {
            lore.add(Component.text("Left-click to edit", NamedTextColor.YELLOW));
            lore.add(Component.text("Right-click to visualize", NamedTextColor.AQUA));
            lore.add(Component.text("Shift-click to enter Animation Mode", NamedTextColor.GOLD));
        } else {
            lore.add(Component.text("SWEEP attacks cannot be edited here", NamedTextColor.RED));
        }
        lore.add(Component.text("Swap-click to rename", NamedTextColor.LIGHT_PURPLE));

        Material icon = editable ? Material.PAPER : Material.BOOK;
        ItemStackBuilder builder = new ItemStackBuilder(icon)
            .name(Component.text(def.getId(), NamedTextColor.GOLD, TextDecoration.BOLD))
            .lore(lore);

        if (!editable) {
            return new SimpleItem(builder.build(), click -> {
                if (click.getClickType() == ClickType.SWAP_OFFHAND) {
                    openRenamePrompt(def);
                } else {
                    swordPlayer.message(Component.text("SWEEP attacks cannot be edited via the menu.", NamedTextColor.RED));
                }
            });
        }

        return new SimpleItem(builder.build(), click -> {
            KeyframedSequence kt = (KeyframedSequence) def.getTrajectory();
            AttackDevSession session = AttackDevSession.getOrCreate(swordPlayer.player());
            if (click.getClickType() == ClickType.SWAP_OFFHAND) {
                openRenamePrompt(def);
            } else if (click.getClickType() == ClickType.SHIFT_LEFT) {
                if (!(swordPlayer instanceof DevSwordPlayer dev)) {
                    swordPlayer.message(Component.text("[Dev] Not a dev player — cannot enter Animation Mode.", NamedTextColor.RED));
                    return;
                }
                session.startEditing(def.getId(), kt.keyframes(), def.getDurationMs(), def.getHitValue());
                swordPlayer.player().closeInventory();
                AnimationMode.enter(dev, session);
            } else if (click.getClickType().isRightClick()) {
                session.startViewing(def.getId(), kt.keyframes());
                swordPlayer.player().closeInventory();
                swordPlayer.message(Component.text(
                    "[Dev] Visualizing '" + def.getId() + "' — close your inventory to stop.",
                    NamedTextColor.YELLOW));
            } else {
                session.startEditing(def.getId(), kt.keyframes(), def.getDurationMs(), def.getHitValue());
                new AttackEditorMenu(swordPlayer).open();
            }
        });
    }
}
