package btm.sword.system.inventory.menu.dev;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.bukkit.Material;

import btm.sword.system.attack.dev.AttackDevSession;
import btm.sword.system.attack.visuals.OriginAnchor;
import btm.sword.system.entity.impl.SwordPlayer;
import btm.sword.system.inventory.item.ForwardItem;
import btm.sword.system.inventory.item.PreviousItem;
import btm.sword.system.inventory.menu.Menu;
import btm.sword.system.item.ItemStackBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import xyz.xenondevs.invui.gui.PagedGui;
import xyz.xenondevs.invui.gui.structure.Markers;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.Window;

/**
 * Picker menu for selecting an {@link OriginAnchor}.
 *
 * <p>Presents the five anchor kinds: owning keyframe, fire-locked origin, raycast origin,
 * three body points on the attacker, and a paged list of keyframe indices. Picking any
 * entry calls the supplied {@link Consumer} with the chosen anchor and then runs the
 * {@code returnTo} runnable to restore the previous screen.</p>
 */
public class OriginAnchorPickerMenu extends Menu {

    private final Consumer<OriginAnchor> onPick;
    private final Runnable returnTo;

    /**
     * @param player   the player viewing the picker
     * @param onPick   consumer invoked with the selected anchor
     * @param returnTo runnable invoked after pick or when Back is clicked
     */
    public OriginAnchorPickerMenu(SwordPlayer player, Consumer<OriginAnchor> onPick, Runnable returnTo) {
        super(player);
        this.onPick = onPick;
        this.returnTo = returnTo;
    }

    @Override
    public void open() {
        AttackDevSession session = AttackDevSession.getOrCreate(swordPlayer.player());
        int kfCount = session.getEditKeyframes() != null ? session.getEditKeyframes().size() : 0;

        SimpleItem back = new SimpleItem(
            new ItemStackBuilder(Material.COPPER_TRAPDOOR)
                .name(Component.text("Back", NamedTextColor.GRAY))
                .build(),
            click -> returnTo.run()
        );

        SimpleItem owning = new SimpleItem(
            new ItemStackBuilder(Material.TARGET)
                .name(Component.text("Owning Keyframe", NamedTextColor.AQUA, TextDecoration.BOLD))
                .lore(List.of(
                    Component.text("Origin resolves to the keyframe", NamedTextColor.DARK_GRAY),
                    Component.text("that owns this display.", NamedTextColor.DARK_GRAY)))
                .build(),
            click -> {
                onPick.accept(OriginAnchor.owning());
                returnTo.run();
            }
        );

        SimpleItem locked = new SimpleItem(
            new ItemStackBuilder(Material.LODESTONE)
                .name(Component.text("Fire-Locked Origin", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD))
                .lore(List.of(
                    Component.text("Origin resolves to the centre", NamedTextColor.DARK_GRAY),
                    Component.text("captured when the attack fired.", NamedTextColor.DARK_GRAY)))
                .build(),
            click -> {
                onPick.accept(OriginAnchor.fireLocked());
                returnTo.run();
            }
        );

        SimpleItem raycastOriginButton = new SimpleItem(
            new ItemStackBuilder(Material.LEAD)
                .name(Component.text("Raycast Origin", NamedTextColor.GREEN, TextDecoration.BOLD))
                .lore(List.of(
                    Component.text("Resolves to the ray start point of a", NamedTextColor.DARK_GRAY),
                    Component.text("RAYCAST keyframe. Falls back to the", NamedTextColor.DARK_GRAY),
                    Component.text("keyframe tip for other types.", NamedTextColor.DARK_GRAY)))
                .build(),
            click -> {
                onPick.accept(OriginAnchor.raycastOrigin());
                returnTo.run();
            }
        );

        SimpleItem bodyEye = bodyButton(OriginAnchor.BodyPoint.EYE, Material.ENDER_EYE, "Attacker Eye");
        SimpleItem bodyChest = bodyButton(OriginAnchor.BodyPoint.CHEST, Material.IRON_CHESTPLATE, "Attacker Chest");
        SimpleItem bodyFeet = bodyButton(OriginAnchor.BodyPoint.FEET, Material.IRON_BOOTS, "Attacker Feet");

        List<Item> kfItems = new ArrayList<>();
        for (int i = 0; i < kfCount; i++) {
            final int idx = i;
            kfItems.add(new SimpleItem(
                new ItemStackBuilder(Material.ORANGE_STAINED_GLASS_PANE)
                    .name(Component.text("Keyframe #" + idx, NamedTextColor.GOLD, TextDecoration.BOLD))
                    .lore(List.of(Component.text("Click to pick this keyframe.", NamedTextColor.DARK_GRAY)))
                    .build(),
                click -> {
                    onPick.accept(OriginAnchor.keyframe(idx));
                    returnTo.run();
                }
            ));
        }

        PagedGui<Item> gui = PagedGui.items()
            .setStructure(
                "B . O R E C F . L",
                "x x x x x x x x x",
                "< # # # # # # # >")
            .addIngredient('#', BORDER)
            .addIngredient('B', back)
            .addIngredient('O', owning)
            .addIngredient('R', raycastOriginButton)
            .addIngredient('E', bodyEye)
            .addIngredient('C', bodyChest)
            .addIngredient('F', bodyFeet)
            .addIngredient('L', locked)
            .addIngredient('x', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
            .addIngredient('<', new PreviousItem())
            .addIngredient('>', new ForwardItem())
            .setContent(kfItems)
            .build();

        Window.single()
            .setViewer(swordPlayer.player())
            .setTitle("Pick Origin Anchor")
            .setGui(gui)
            .build()
            .open();
    }

    private SimpleItem bodyButton(OriginAnchor.BodyPoint point, Material mat, String label) {
        return new SimpleItem(
            new ItemStackBuilder(mat)
                .name(Component.text(label, NamedTextColor.YELLOW, TextDecoration.BOLD))
                .lore(List.of(Component.text("Origin follows the attacker's " + point.name().toLowerCase() + ".",
                    NamedTextColor.DARK_GRAY)))
                .build(),
            click -> {
                onPick.accept(OriginAnchor.body(point));
                returnTo.run();
            }
        );
    }

}
