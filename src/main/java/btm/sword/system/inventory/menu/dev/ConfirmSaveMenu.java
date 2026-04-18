package btm.sword.system.inventory.menu.dev;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import btm.sword.system.entity.impl.SwordPlayer;
import btm.sword.system.inventory.menu.Menu;
import btm.sword.system.item.ItemStackBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.Window;

/**
 * Single-row confirmation menu shown when a player presses Back with unsaved changes.
 *
 * <p>Three options:</p>
 * <ul>
 *   <li><b>Save &amp; Back</b> — commits the draft, then returns to the parent.</li>
 *   <li><b>Discard &amp; Back</b> — discards the draft, then returns to the parent.</li>
 *   <li><b>Cancel</b> — dismisses this menu and returns to the editor (no change).</li>
 * </ul>
 */
public class ConfirmSaveMenu extends Menu {

    private final String contextLabel;
    private final Runnable onSave;
    private final Runnable onDiscard;
    private final Runnable onCancel;

    /**
     * @param player      the player viewing this confirmation
     * @param contextLabel short description shown in the window title (e.g. "ParticleDisplay")
     * @param onSave      runs when the player chooses Save &amp; Back
     * @param onDiscard   runs when the player chooses Discard &amp; Back
     * @param onCancel    runs when the player chooses Cancel (return to editor)
     */
    public ConfirmSaveMenu(SwordPlayer player, String contextLabel,
                           Runnable onSave, Runnable onDiscard, Runnable onCancel) {
        super(player);
        this.contextLabel = contextLabel;
        this.onSave = onSave;
        this.onDiscard = onDiscard;
        this.onCancel = onCancel;
    }

    @Override
    public void open() {
        Player player = swordPlayer.player();

        SimpleItem saveAndBack = new SimpleItem(
            new ItemStackBuilder(Material.LIME_CONCRETE)
                .name(Component.text("Save & Back", NamedTextColor.GREEN, TextDecoration.BOLD))
                .lore(List.of(Component.text("Commit changes and return", NamedTextColor.GRAY)))
                .build(),
            click -> onSave.run()
        );

        SimpleItem discardAndBack = new SimpleItem(
            new ItemStackBuilder(Material.RED_CONCRETE)
                .name(Component.text("Discard & Back", NamedTextColor.RED, TextDecoration.BOLD))
                .lore(List.of(Component.text("Drop changes and return", NamedTextColor.GRAY)))
                .build(),
            click -> onDiscard.run()
        );

        SimpleItem cancel = new SimpleItem(
            new ItemStackBuilder(Material.YELLOW_CONCRETE)
                .name(Component.text("Cancel", NamedTextColor.YELLOW, TextDecoration.BOLD))
                .lore(List.of(Component.text("Stay in the editor", NamedTextColor.GRAY)))
                .build(),
            click -> onCancel.run()
        );

        Gui gui = Gui.normal()
            .setStructure("# # S # D # C # #")
            .addIngredient('#', BORDER)
            .addIngredient('S', saveAndBack)
            .addIngredient('D', discardAndBack)
            .addIngredient('C', cancel)
            .build();

        Window.single()
            .setViewer(player)
            .setTitle("Unsaved changes — " + contextLabel)
            .setGui(gui)
            .build()
            .open();
    }
}
