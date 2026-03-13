package btm.sword.system.inventory.menu;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

import btm.sword.system.entity.aspect.Aspect;
import btm.sword.system.entity.aspect.AspectType;
import btm.sword.system.entity.aspect.Resource;
import btm.sword.system.entity.base.EntityAspects;
import btm.sword.system.entity.impl.SwordPlayer;
import btm.sword.system.item.ItemStackBuilder;
import btm.sword.utility.ChatInputCapture;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.AbstractItem;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.Window;

/**
 * Developer-only menu for editing live {@link Aspect} and {@link Resource} values
 * directly on the player's {@link EntityAspects} without a server restart.
 * <p>
 * Each {@link AspectType} is shown as a single item:
 * <ul>
 *   <li><b>Resources</b> (SHARDS/TOUGHNESS/SOULFIRE/FORM):
 *       L-click sets base max via chat; R-click sets regen period; Shift+L sets regen amount.</li>
 *   <li><b>Aspects</b> (MIGHT/RESOLVE/…):
 *       Any click opens a chat prompt to set the base value.</li>
 * </ul>
 * Changes take effect on the live entity immediately and restart regen tasks as needed.
 * </p>
 */
public class DevStatEditorMenu extends Menu {

    public DevStatEditorMenu(SwordPlayer player) {
        super(player);
    }

    @Override
    public void open() {
        Player player = swordPlayer.player();
        EntityAspects aspects = swordPlayer.getAspects();

        List<AbstractItem> items = new ArrayList<>();
        for (AspectType type : AspectType.values()) {
            Aspect aspect = aspects.getAspect(type);
            items.add(buildAspectItem(aspect));
        }

        SimpleItem back = new SimpleItem(
            new ItemStackBuilder(Material.COPPER_TRAPDOOR)
                .name(Component.text("Back to Character", NamedTextColor.GRAY))
                .build(),
            click -> new CharacterMenu(swordPlayer).open()
        );

        // Layout: 2 rows of 9 — 12 aspects fit neatly
        Gui gui = Gui.normal()
            .setStructure(
                "# # # # # # # # #",
                "0 1 2 3 4 5 6 7 8",
                ". . . 9 A C . . .",
                "# # # # B # # # #")
            .addIngredient('#', BORDER)
            .addIngredient('B', back)
            .addIngredient('0', items.get(0))   // SHARDS
            .addIngredient('1', items.get(1))   // TOUGHNESS
            .addIngredient('2', items.get(2))   // SOULFIRE
            .addIngredient('3', items.get(3))   // FORM
            .addIngredient('4', items.get(4))   // MIGHT
            .addIngredient('5', items.get(5))   // RESOLVE
            .addIngredient('6', items.get(6))   // FINESSE
            .addIngredient('7', items.get(7))   // PROWESS
            .addIngredient('8', items.get(8))   // ARMOR
            .addIngredient('9', items.get(9))   // FORTITUDE
            .addIngredient('A', items.get(10))  // CELERITY
            .addIngredient('C', items.get(11))  // WILLPOWER
            .build();

        Window.single()
            .setViewer(player)
            .setTitle("Dev Stat Editor")
            .setGui(gui)
            .build()
            .open();
    }

    /**
     * Builds an {@link AbstractItem} for the given {@link Aspect}.
     * Resources show current/max and regen stats; regular aspects show base value.
     */
    private AbstractItem buildAspectItem(Aspect aspect) {
        boolean isResource = aspect instanceof Resource;

        return new AbstractItem() {
            @Override
            public ItemProvider getItemProvider() {
                Material mat = aspectMaterial(aspect.type);
                ItemBuilder builder = new ItemBuilder(mat)
                    .setDisplayName("§e" + aspect.type.name());

                if (isResource) {
                    Resource r = (Resource) aspect;
                    builder.addLoreLines(
                        String.format("§7Base max: §f%.1f", r.getBaseValue()),
                        String.format("§7Regen period: §f%d ticks", r.getBaseRegenPeriod()),
                        String.format("§7Regen amount: §f%.2f", r.getBaseRegenAmount()),
                        "",
                        "§8L-click: set base max",
                        "§8R-click: set regen period",
                        "§8Shift+L: set regen amount"
                    );
                } else {
                    builder.addLoreLines(
                        String.format("§7Base value: §f%.1f", aspect.getBaseValue()),
                        "",
                        "§8Click: set base value"
                    );
                }
                return builder;
            }

            @Override
            public void handleClick(ClickType clickType, Player p, InventoryClickEvent event) {
                if (isResource) {
                    Resource r = (Resource) aspect;
                    if (clickType == ClickType.LEFT) {
                        promptFloat(p, "base max for " + aspect.type.name(), val -> {
                            r.setBaseValue(val);
                            notifyWindows();
                            swordPlayer.message("§aSet " + aspect.type.name() + " base max = §e" + val);
                        });
                    } else if (clickType == ClickType.RIGHT) {
                        promptInt(p, "regen period (ticks) for " + aspect.type.name(), val -> {
                            r.stopRegenTask();
                            r.setBaseRegenPeriod(val);
                            r.startRegenTask();
                            notifyWindows();
                            swordPlayer.message("§aSet " + aspect.type.name() + " regen period = §e" + val + " ticks");
                        });
                    } else if (clickType == ClickType.SHIFT_LEFT) {
                        promptFloat(p, "regen amount for " + aspect.type.name(), val -> {
                            r.setBaseRegenAmount(val);
                            notifyWindows();
                            swordPlayer.message("§aSet " + aspect.type.name() + " regen amount = §e" + val);
                        });
                    }
                } else {
                    promptFloat(p, "base value for " + aspect.type.name(), val -> {
                        aspect.setBaseValue(val);
                        notifyWindows();
                        swordPlayer.message("§aSet " + aspect.type.name() + " base value = §e" + val);
                    });
                }
            }
        };
    }

    private void promptFloat(Player p, String label, java.util.function.Consumer<Float> onValue) {
        ChatInputCapture.prompt(p,
            Component.text("Enter " + label + ":", NamedTextColor.YELLOW),
            input -> {
                if ("cancel".equalsIgnoreCase(input)) return;
                try {
                    float val = Float.parseFloat(input);
                    onValue.accept(val);
                } catch (NumberFormatException ignored) {
                    swordPlayer.message("§cInvalid number: " + input);
                }
                open();
            }
        );
    }

    private void promptInt(Player p, String label, java.util.function.Consumer<Integer> onValue) {
        ChatInputCapture.prompt(p,
            Component.text("Enter " + label + ":", NamedTextColor.YELLOW),
            input -> {
                if ("cancel".equalsIgnoreCase(input)) return;
                try {
                    int val = Integer.parseInt(input.trim());
                    onValue.accept(val);
                } catch (NumberFormatException ignored) {
                    swordPlayer.message("§cInvalid integer: " + input);
                }
                open();
            }
        );
    }

    /** Maps each {@link AspectType} to a representative display material. */
    private static Material aspectMaterial(AspectType type) {
        return switch (type) {
            case SHARDS    -> Material.DIAMOND;
            case TOUGHNESS -> Material.IRON_CHESTPLATE;
            case SOULFIRE  -> Material.SOUL_LANTERN;
            case FORM      -> Material.EXPERIENCE_BOTTLE;
            case MIGHT     -> Material.GOLDEN_SWORD;
            case RESOLVE   -> Material.SHIELD;
            case FINESSE   -> Material.FEATHER;
            case PROWESS   -> Material.BOW;
            case ARMOR     -> Material.IRON_HELMET;
            case FORTITUDE -> Material.OBSIDIAN;
            case CELERITY  -> Material.SUGAR;
            case WILLPOWER -> Material.AMETHYST_SHARD;
        };
    }
}
