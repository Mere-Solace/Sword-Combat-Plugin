package btm.sword.menu.dev;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import btm.sword.entity.display.WeaponDisplayRegistry;
import btm.sword.entity.display.WeaponDisplayTransform;
import btm.sword.entity.player.SwordPlayer;
import btm.sword.item.core.ItemStackBuilder;
import btm.sword.menu.Menu;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.ItemWrapper;
import xyz.xenondevs.invui.item.impl.AbstractItem;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.Window;

/**
 * Developer menu for tweaking per-material {@link WeaponDisplayTransform} values in-game.
 *
 * <p>Opens for the material of the item held in the player's main hand.
 * Changes are applied to {@link WeaponDisplayRegistry} immediately; click <b>Save</b>
 * to write them to {@code weapon_display.yml}.</p>
 *
 * <ul>
 *   <li>L-click / R-click: decrement / increment by the normal step</li>
 *   <li>Shift + L/R-click: decrement / increment by the large step</li>
 * </ul>
 */
public class WeaponDisplayEditorMenu extends Menu {

    private static final float OFFSET_STEP        = 0.05f;
    private static final float OFFSET_SHIFT_STEP  = 0.5f;
    private static final float ROT_STEP            = 5.0f;
    private static final float ROT_SHIFT_STEP      = 45.0f;
    private static final float SCALE_STEP          = 0.05f;
    private static final float SCALE_SHIFT_STEP    = 0.5f;

    private final Material material;

    /**
     * Creates a new WeaponDisplayEditorMenu for the given player.
     * The edited material is taken from the player's main-hand item at construction time.
     *
     * @param player the player opening this menu
     */
    public WeaponDisplayEditorMenu(SwordPlayer player) {
        super(player);
        ItemStack held = player.player().getInventory().getItemInMainHand();
        this.material = held.getType().isAir() ? null : held.getType();
    }

    @Override
    public void open() {
        Player player = swordPlayer.player();

        if (material == null) {
            swordPlayer.message(Component.text("Hold an item to edit its weapon display transform.", NamedTextColor.RED));
            return;
        }

        AbstractItem offsetRight   = buildField("Offset Right",   material, "offsetRight",   OFFSET_STEP, OFFSET_SHIFT_STEP);
        AbstractItem offsetUp      = buildField("Offset Up",      material, "offsetUp",      OFFSET_STEP, OFFSET_SHIFT_STEP);
        AbstractItem offsetForward = buildField("Offset Forward", material, "offsetForward", OFFSET_STEP, OFFSET_SHIFT_STEP);
        AbstractItem rotX          = buildField("Rot X (deg)",    material, "rotX",          ROT_STEP,    ROT_SHIFT_STEP);
        AbstractItem rotY          = buildField("Rot Y (deg)",    material, "rotY",          ROT_STEP,    ROT_SHIFT_STEP);
        AbstractItem rotZ          = buildField("Rot Z (deg)",    material, "rotZ",          ROT_STEP,    ROT_SHIFT_STEP);
        AbstractItem scale         = buildField("Scale",          material, "scale",         SCALE_STEP,  SCALE_SHIFT_STEP);

        SimpleItem save = new SimpleItem(
            new ItemStackBuilder(Material.EMERALD)
                .name(Component.text("Save to weapon_display.yml", NamedTextColor.GREEN, TextDecoration.BOLD))
                .lore(List.of(Component.text("Writes all current transforms to disk", NamedTextColor.DARK_GRAY)))
                .build(),
            click -> {
                WeaponDisplayRegistry.save();
                swordPlayer.message(Component.text("Saved weapon_display.yml.", NamedTextColor.GREEN));
            }
        );

        SimpleItem back = new SimpleItem(
            new ItemStackBuilder(Material.COPPER_TRAPDOOR)
                .name(Component.text("Back to Dev Menu", NamedTextColor.GRAY))
                .build(),
            click -> new DevMenu(swordPlayer).open()
        );

        Gui gui = Gui.normal()
            .setStructure(
                "# # # # # # # # #",
                "# A B C . D E F #",
                "# . . . G . . . #",
                "# # # S K . # # #")
            .addIngredient('#', BORDER)
            .addIngredient('A', offsetRight)
            .addIngredient('B', offsetUp)
            .addIngredient('C', offsetForward)
            .addIngredient('D', rotX)
            .addIngredient('E', rotY)
            .addIngredient('F', rotZ)
            .addIngredient('G', scale)
            .addIngredient('S', save)
            .addIngredient('K', back)
            .build();

        Window.single()
            .setViewer(player)
            .setTitle("Weapon Display: " + material.name())
            .setGui(gui)
            .build()
            .open();
    }

    /**
     * Builds an {@link AbstractItem} that displays and edits one float field of the
     * {@link WeaponDisplayTransform} for the given material.
     *
     * @param label     display name shown in the item lore
     * @param mat       the material whose transform is being edited
     * @param field     one of: offsetRight, offsetUp, offsetForward, rotX, rotY, rotZ, scale
     * @param step      normal click step
     * @param shiftStep shift-click step
     */
    private AbstractItem buildField(String label, Material mat, String field, float step, float shiftStep) {
        return new AbstractItem() {
            @Override
            public ItemProvider getItemProvider() {
                WeaponDisplayTransform t = WeaponDisplayRegistry.get(mat);
                float value = getField(t, field);
                List<Component> lore = new ArrayList<>();
                lore.add(Component.text("Value: ", NamedTextColor.GRAY)
                    .append(Component.text(String.format("%.3f", value), NamedTextColor.YELLOW)));
                lore.add(Component.empty());
                lore.add(Component.text("L/R: ±" + step, NamedTextColor.DARK_GRAY));
                lore.add(Component.text("Shift L/R: ±" + shiftStep, NamedTextColor.DARK_GRAY));
                return new ItemWrapper(new ItemStackBuilder(fieldMaterial(field))
                    .name(Component.text(label, NamedTextColor.AQUA, TextDecoration.BOLD))
                    .lore(lore)
                    .build());
            }

            @Override
            public void handleClick(ClickType clickType, Player p, InventoryClickEvent event) {
                float delta;
                if (clickType == ClickType.LEFT) {
                    delta = -step;
                } else if (clickType == ClickType.RIGHT) {
                    delta = step;
                } else if (clickType == ClickType.SHIFT_LEFT) {
                    delta = -shiftStep;
                } else if (clickType == ClickType.SHIFT_RIGHT) {
                    delta = shiftStep;
                } else {
                    return;
                }
                WeaponDisplayTransform current = WeaponDisplayRegistry.get(mat);
                WeaponDisplayRegistry.set(mat, withField(current, field, getField(current, field) + delta));
                notifyWindows();
            }
        };
    }

    // -----------------------------------------------------------------------
    // Field accessors by name

    private static float getField(WeaponDisplayTransform t, String field) {
        return switch (field) {
            case "offsetRight"   -> t.offsetRight();
            case "offsetUp"      -> t.offsetUp();
            case "offsetForward" -> t.offsetForward();
            case "rotX"          -> t.rotX();
            case "rotY"          -> t.rotY();
            case "rotZ"          -> t.rotZ();
            case "scale"         -> t.scale();
            default -> 0f;
        };
    }

    private static WeaponDisplayTransform withField(WeaponDisplayTransform t, String field, float value) {
        return switch (field) {
            case "offsetRight"   -> new WeaponDisplayTransform(value,           t.offsetUp(), t.offsetForward(), t.rotX(), t.rotY(), t.rotZ(), t.scale());
            case "offsetUp"      -> new WeaponDisplayTransform(t.offsetRight(),  value,        t.offsetForward(), t.rotX(), t.rotY(), t.rotZ(), t.scale());
            case "offsetForward" -> new WeaponDisplayTransform(t.offsetRight(),  t.offsetUp(), value,             t.rotX(), t.rotY(), t.rotZ(), t.scale());
            case "rotX"          -> new WeaponDisplayTransform(t.offsetRight(),  t.offsetUp(), t.offsetForward(), value,    t.rotY(), t.rotZ(), t.scale());
            case "rotY"          -> new WeaponDisplayTransform(t.offsetRight(),  t.offsetUp(), t.offsetForward(), t.rotX(), value,    t.rotZ(), t.scale());
            case "rotZ"          -> new WeaponDisplayTransform(t.offsetRight(),  t.offsetUp(), t.offsetForward(), t.rotX(), t.rotY(), value,    t.scale());
            case "scale"         -> new WeaponDisplayTransform(t.offsetRight(),  t.offsetUp(), t.offsetForward(), t.rotX(), t.rotY(), t.rotZ(), value);
            default -> t;
        };
    }

    private static Material fieldMaterial(String field) {
        return switch (field) {
            case "offsetRight"   -> Material.ARROW;
            case "offsetUp"      -> Material.FEATHER;
            case "offsetForward" -> Material.COMPASS;
            case "rotX"          -> Material.RED_DYE;
            case "rotY"          -> Material.LIME_DYE;
            case "rotZ"          -> Material.BLUE_DYE;
            case "scale"         -> Material.QUARTZ;
            default -> Material.PAPER;
        };
    }
}
