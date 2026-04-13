package btm.sword.system.attack.dev;

import java.io.File;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;

import btm.sword.Sword;
import btm.sword.system.attack.def.AttackDef;
import btm.sword.system.attack.def.AttackDefSerializer;
import btm.sword.system.attack.def.AttackRegistry;
import btm.sword.system.attack.simulation.KeyframedTrajectory;
import btm.sword.system.entity.impl.DevSwordPlayer;
import btm.sword.system.item.ItemStackBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.Window;

/**
 * Presents a non-escapable save/discard confirmation dialog when the player
 * attempts to exit {@link AnimationMode} via the BARRIER button in slot 35.
 *
 * <h2>Dialog layout (1 row × 9)</h2>
 * <pre>
 * [ ][ ][ ] [Save &amp; Exit] [ ] [Discard &amp; Exit] [ ][ ][ ]
 * </pre>
 *
 * <ul>
 *   <li><b>Save &amp; Exit</b> (slot 3, {@code LIME_TERRACOTTA}) — writes the current
 *       {@link AttackDevSession#buildCurrentAttack()} result to disk, registers it in
 *       {@link AttackRegistry}, then calls {@link AnimationMode#exit}.</li>
 *   <li><b>Discard &amp; Exit</b> (slot 5, {@code RED_TERRACOTTA}) — reloads keyframes
 *       from the saved YAML file, resets the session's edit state, then calls
 *       {@link AnimationMode#exit}.</li>
 * </ul>
 *
 * <p>The window's close handler reopens the dialog if the player tries to dismiss it
 * by pressing Escape, making it effectively non-escapable.</p>
 */
public final class SaveConfirmDialog {

    private SaveConfirmDialog() {}

    /**
     * Opens the save/discard confirmation dialog for the given player and session.
     *
     * @param player  the {@link DevSwordPlayer} requesting to exit AnimationMode
     * @param session the active {@link AttackDevSession}, or {@code null} if none
     */
    public static void open(DevSwordPlayer player, AttackDevSession session) {
        int edits = session != null ? session.getEditCount() : 0;

        SimpleItem saveButton = new SimpleItem(
            new ItemStackBuilder(Material.LIME_TERRACOTTA)
                .name(Component.text("Save & Exit", NamedTextColor.GREEN, TextDecoration.BOLD))
                .lore(List.of(
                    Component.text("Writes changes to attacks/<id>.yml", NamedTextColor.DARK_GRAY),
                    Component.text("and exits Animation Mode.", NamedTextColor.GRAY)
                ))
                .build(),
            click -> {
                if (session != null) {
                    saveAndExit(player, session);
                } else {
                    AnimationMode.exit(player);
                }
            }
        );

        SimpleItem discardButton = new SimpleItem(
            new ItemStackBuilder(Material.RED_TERRACOTTA)
                .name(Component.text("Discard & Exit", NamedTextColor.RED, TextDecoration.BOLD))
                .lore(List.of(
                    Component.text("Reverts to last saved state", NamedTextColor.DARK_GRAY),
                    Component.text("and exits Animation Mode.", NamedTextColor.GRAY)
                ))
                .build(),
            click -> {
                if (session != null) {
                    discardAndExit(player, session);
                } else {
                    AnimationMode.exit(player);
                }
            }
        );

        Gui gui = Gui.normal()
            .setStructure("# # # S # D # # #")
            .addIngredient('#', new SimpleItem(new ItemStackBuilder(Material.BLACK_STAINED_GLASS_PANE)
                .name(Component.text(" "))
                .build()))
            .addIngredient('S', saveButton)
            .addIngredient('D', discardButton)
            .build();

        Window.single()
            .setViewer(player.player())
            .setTitle("Save changes? (" + edits + " edit" + (edits == 1 ? "" : "s") + " made)")
            .setGui(gui)
            .addCloseHandler(() -> {
                // If the player presses Escape without choosing, reopen the dialog
                if (player.player().isOnline() && player.isInAnimationMode()) {
                    open(player, session);
                }
            })
            .build()
            .open();
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private static void saveAndExit(DevSwordPlayer player, AttackDevSession session) {
        try {
            AttackDef def = session.buildCurrentAttack();
            AttackRegistry.register(def);

            File attacksDir = new File(Sword.getInstance().getDataFolder(), "attacks");
            attacksDir.mkdirs();
            File file = new File(attacksDir, def.getId() + ".yml");
            AttackDefSerializer.save(file, def);

            session.setEditCount(0);
            player.message(Component.text(
                "[Dev] Saved '" + def.getId() + "' — exiting Animation Mode.", NamedTextColor.AQUA));
        } catch (Exception e) {
            player.message(Component.text("[Dev] Save failed: " + e.getMessage(), NamedTextColor.RED));
        }
        AnimationMode.exit(player);
    }

    private static void discardAndExit(DevSwordPlayer player, AttackDevSession session) {
        String name = session.getCurrentAttackName();
        if (name != null) {
            File file = new File(new File(Sword.getInstance().getDataFolder(), "attacks"), name + ".yml");
            if (file.exists()) {
                YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
                var attacksSection = yaml.getConfigurationSection("attacks");
                if (attacksSection != null) {
                    var section = attacksSection.getConfigurationSection(name);
                    if (section != null) {
                        try {
                            AttackDef loaded = AttackDefSerializer.load(section, name);
                            if (loaded.getTrajectory() instanceof KeyframedTrajectory kt) {
                                session.setEditKeyframes(kt.getKeyframes());
                            }
                        } catch (Exception e) {
                            player.message(Component.text(
                                "[Dev] Could not restore from file: " + e.getMessage(), NamedTextColor.RED));
                        }
                    }
                }
            }
        }
        session.setEditCount(0);
        player.message(Component.text("[Dev] Changes discarded — exiting Animation Mode.", NamedTextColor.YELLOW));
        AnimationMode.exit(player);
    }
}
