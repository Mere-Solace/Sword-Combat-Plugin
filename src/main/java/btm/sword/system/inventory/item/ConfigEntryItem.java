package btm.sword.system.inventory.item;

import java.util.List;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import btm.sword.config.Config;
import btm.sword.config.ConfigManager;
import btm.sword.system.entity.impl.SwordPlayer;
import btm.sword.system.inventory.menu.EnumSelectionMenu;
import btm.sword.system.item.ItemStackBuilder;
import btm.sword.utility.ChatInputCapture;
import btm.sword.utility.sound.SoundType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.ItemWrapper;
import xyz.xenondevs.invui.item.impl.AbstractItem;

/**
 * A dynamic InvUI item representing a single {@link Config.ConfigEntry}.
 * <p>
 * Displays the entry's YAML path, type, current value, and default value.
 * Editable types can be modified in-place; the item re-renders immediately via
 * {@link #notifyWindows()} without resetting the current page of the parent
 * {@link xyz.xenondevs.invui.gui.PagedGui}.
 * </p>
 *
 * <h3>Interaction</h3>
 * <ul>
 *   <li><b>Boolean</b> — any click toggles the value.</li>
 *   <li><b>Integer / Long</b> — left-click decrements, right-click increments.
 *       Shift multiplies the step by 10. Shift+left-click prompts for a typed value.</li>
 *   <li><b>Double / Float</b> — same as Integer/Long with a decimal step.
 *       Shift+left-click prompts for a typed value.</li>
 *   <li><b>Color / TextColor</b> — any click prompts for typed {@code r, g, b} input.</li>
 *   <li>All other types are displayed read-only.</li>
 * </ul>
 */
public class ConfigEntryItem extends AbstractItem {

    private static final double DOUBLE_STEP = 0.1;
    private static final double DOUBLE_SHIFT_STEP = 1.0;
    private static final int INT_STEP = 1;
    private static final int INT_SHIFT_STEP = 10;
    private static final long LONG_STEP = 1L;
    private static final long LONG_SHIFT_STEP = 10L;

    private final Config.ConfigEntry<?> entry;
    private final SwordPlayer swordPlayer;
    private final Runnable reopenMenu;

    /**
     * Constructs a {@code ConfigEntryItem} for the given entry.
     *
     * @param entry       the config entry to represent; must not be null
     * @param swordPlayer the viewing player, used for chat-input prompts
     * @param reopenMenu  called on the main thread to reopen the parent menu
     *                    after a typed-input session completes or is cancelled
     */
    public ConfigEntryItem(Config.ConfigEntry<?> entry, SwordPlayer swordPlayer, Runnable reopenMenu) {
        this.entry = entry;
        this.swordPlayer = swordPlayer;
        this.reopenMenu = reopenMenu;
    }

    @Override
    public ItemProvider getItemProvider() {
        FileConfiguration config = ConfigManager.getInstance().getConfig();
        Class<?> type = entry.type;
        String path = entry.path;

        if (type == Boolean.class) {
            boolean val = config.getBoolean(path, defaultBoolean());
            return new ItemWrapper(new ItemStackBuilder(val ? Material.LIME_DYE : Material.GRAY_DYE)
                .name(Component.text(path, NamedTextColor.WHITE))
                .lore(List.of(
                    Component.text("Type: ", NamedTextColor.GRAY).append(Component.text("Boolean", NamedTextColor.WHITE)),
                    Component.text("Value: ", NamedTextColor.GRAY).append(val
                        ? Component.text("ON", NamedTextColor.GREEN)
                        : Component.text("OFF", NamedTextColor.RED)),
                    Component.text("Default: ", NamedTextColor.GRAY).append(Component.text(String.valueOf(entry.defaultValue), NamedTextColor.DARK_GRAY)),
                    Component.text("Click to toggle", NamedTextColor.DARK_GRAY)
                ))
                .build());
        }

        if (type == Integer.class) {
            int val = config.getInt(path, defaultInt());
            return new ItemWrapper(new ItemStackBuilder(Material.COMPARATOR)
                .name(Component.text(path, NamedTextColor.WHITE))
                .lore(List.of(
                    Component.text("Type: ", NamedTextColor.GRAY).append(Component.text("Integer", NamedTextColor.WHITE)),
                    Component.text("Value: ", NamedTextColor.GRAY).append(Component.text(String.valueOf(val), NamedTextColor.YELLOW)),
                    Component.text("Default: ", NamedTextColor.GRAY).append(Component.text(String.valueOf(entry.defaultValue), NamedTextColor.DARK_GRAY)),
                    Component.text("L/R: \u00b1" + INT_STEP + "   Shift \u00d7" + INT_SHIFT_STEP, NamedTextColor.DARK_GRAY),
                    Component.text("Shift+L: type a value", NamedTextColor.DARK_GRAY)
                ))
                .build());
        }

        if (type == Long.class) {
            long val = config.getLong(path, defaultLong());
            return new ItemWrapper(new ItemStackBuilder(Material.CLOCK)
                .name(Component.text(path, NamedTextColor.WHITE))
                .lore(List.of(
                    Component.text("Type: ", NamedTextColor.GRAY).append(Component.text("Long", NamedTextColor.WHITE)),
                    Component.text("Value: ", NamedTextColor.GRAY).append(Component.text(String.valueOf(val), NamedTextColor.YELLOW)),
                    Component.text("Default: ", NamedTextColor.GRAY).append(Component.text(String.valueOf(entry.defaultValue), NamedTextColor.DARK_GRAY)),
                    Component.text("L/R: \u00b1" + LONG_STEP + "   Shift \u00d7" + LONG_SHIFT_STEP, NamedTextColor.DARK_GRAY),
                    Component.text("Shift+L: type a value", NamedTextColor.DARK_GRAY)
                ))
                .build());
        }

        if (type == Double.class) {
            double val = config.getDouble(path, defaultDouble());
            return new ItemWrapper(new ItemStackBuilder(Material.REPEATER)
                .name(Component.text(path, NamedTextColor.WHITE))
                .lore(List.of(
                    Component.text("Type: ", NamedTextColor.GRAY).append(Component.text("Double", NamedTextColor.WHITE)),
                    Component.text("Value: ", NamedTextColor.GRAY).append(Component.text(String.format("%.4f", val), NamedTextColor.YELLOW)),
                    Component.text("Default: ", NamedTextColor.GRAY).append(Component.text(String.valueOf(entry.defaultValue), NamedTextColor.DARK_GRAY)),
                    Component.text("L/R: \u00b1" + DOUBLE_STEP + "   Shift \u00d7" + (int)(DOUBLE_SHIFT_STEP / DOUBLE_STEP), NamedTextColor.DARK_GRAY),
                    Component.text("Shift+L: type a value", NamedTextColor.DARK_GRAY)
                ))
                .build());
        }

        if (type == Float.class) {
            float val = (float) config.getDouble(path, defaultFloat());
            return new ItemWrapper(new ItemStackBuilder(Material.REPEATER)
                .name(Component.text(path, NamedTextColor.WHITE))
                .lore(List.of(
                    Component.text("Type: ", NamedTextColor.GRAY).append(Component.text("Float", NamedTextColor.WHITE)),
                    Component.text("Value: ", NamedTextColor.GRAY).append(Component.text(String.format("%.4f", val), NamedTextColor.YELLOW)),
                    Component.text("Default: ", NamedTextColor.GRAY).append(Component.text(String.valueOf(entry.defaultValue), NamedTextColor.DARK_GRAY)),
                    Component.text("L/R: \u00b1" + DOUBLE_STEP + "   Shift \u00d7" + (int)(DOUBLE_SHIFT_STEP / DOUBLE_STEP), NamedTextColor.DARK_GRAY),
                    Component.text("Shift+L: type a value", NamedTextColor.DARK_GRAY)
                ))
                .build());
        }

        if (type.isEnum()) {
            String stored = config.getString(entry.path);
            String currentName = stored != null ? stored : (entry.defaultValue instanceof Enum<?> e ? e.name() : "?");
            String defName = entry.defaultValue instanceof Enum<?> e ? e.name() : "?";
            Material mat = type == SoundType.class ? Material.NOTE_BLOCK : Material.PAPER;
            Component hint = type == SoundType.class
                ? Component.text("Click to browse  (R-click previews)", NamedTextColor.DARK_GRAY)
                : Component.text("Click to browse", NamedTextColor.DARK_GRAY);
            return new ItemWrapper(new ItemStackBuilder(mat)
                .name(Component.text(path, NamedTextColor.WHITE))
                .lore(List.of(
                    Component.text("Type: ", NamedTextColor.GRAY).append(Component.text(type.getSimpleName(), NamedTextColor.WHITE)),
                    Component.text("Value: ", NamedTextColor.GRAY).append(Component.text(currentName, NamedTextColor.YELLOW)),
                    Component.text("Default: ", NamedTextColor.GRAY).append(Component.text(defName, NamedTextColor.DARK_GRAY)),
                    hint
                ))
                .build());
        }

        if (type == Color.class) {
            Color val = Config.loadColor(config, path, defaultBukkitColor());
            return new ItemWrapper(new ItemStackBuilder(Material.MAGENTA_DYE)
                .name(Component.text(path, NamedTextColor.WHITE))
                .lore(List.of(
                    Component.text("Type: ", NamedTextColor.GRAY).append(Component.text("Color", NamedTextColor.WHITE)),
                    Component.text("Value: ", NamedTextColor.GRAY).append(Component.text(colorToString(val), NamedTextColor.YELLOW)),
                    Component.text("Default: ", NamedTextColor.GRAY).append(Component.text(colorToString(defaultBukkitColor()), NamedTextColor.DARK_GRAY)),
                    Component.text("Click to edit  (format: ", NamedTextColor.DARK_GRAY)
                        .append(Component.text("r, g, b", NamedTextColor.WHITE))
                        .append(Component.text(")", NamedTextColor.DARK_GRAY))
                ))
                .build());
        }

        if (type == TextColor.class) {
            TextColor val = Config.loadTextColor(config, path, defaultTextColor());
            return new ItemWrapper(new ItemStackBuilder(Material.PAPER)
                .name(Component.text(path, NamedTextColor.WHITE))
                .lore(List.of(
                    Component.text("Type: ", NamedTextColor.GRAY).append(Component.text("TextColor", NamedTextColor.WHITE)),
                    Component.text("Value: ", NamedTextColor.GRAY).append(Component.text(textColorToString(val), NamedTextColor.YELLOW)),
                    Component.text("Default: ", NamedTextColor.GRAY).append(Component.text(textColorToString(defaultTextColor()), NamedTextColor.DARK_GRAY)),
                    Component.text("Click to edit  (format: ", NamedTextColor.DARK_GRAY)
                        .append(Component.text("r, g, b", NamedTextColor.WHITE))
                        .append(Component.text(")", NamedTextColor.DARK_GRAY))
                ))
                .build());
        }

        if (type == Vector.class) {
            Vector val = Config.loadVector(config, path, defaultVector());
            return new ItemWrapper(new ItemStackBuilder(Material.END_ROD)
                .name(Component.text(path, NamedTextColor.WHITE))
                .lore(List.of(
                    Component.text("Type: ", NamedTextColor.GRAY).append(Component.text("Vector", NamedTextColor.WHITE)),
                    Component.text("Value: ", NamedTextColor.GRAY).append(Component.text(vectorToString(val), NamedTextColor.YELLOW)),
                    Component.text("Default: ", NamedTextColor.GRAY).append(Component.text(vectorToString(defaultVector()), NamedTextColor.DARK_GRAY)),
                    Component.text("Click to edit  (format: ", NamedTextColor.DARK_GRAY)
                        .append(Component.text("x y z", NamedTextColor.WHITE))
                        .append(Component.text(")", NamedTextColor.DARK_GRAY))
                ))
                .build());
        }

        // Read-only fallback for complex types
        String defStr = entry.defaultValue.toString();
        String defPreview = defStr.length() > 40 ? defStr.substring(0, 37) + "..." : defStr;
        return new ItemWrapper(new ItemStackBuilder(Material.KNOWLEDGE_BOOK)
            .name(Component.text(path, NamedTextColor.GRAY))
            .lore(List.of(
                Component.text("Type: ", NamedTextColor.GRAY).append(Component.text(type.getSimpleName(), NamedTextColor.WHITE)),
                Component.text("Default: ", NamedTextColor.GRAY).append(Component.text(defPreview, NamedTextColor.DARK_GRAY)),
                Component.text("Read-only", NamedTextColor.RED)
            ))
            .build());
    }

    @Override
    @SuppressWarnings("unchecked")
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        ConfigManager mgr = ConfigManager.getInstance();
        FileConfiguration config = mgr.getConfig();
        Class<?> type = entry.type;
        String path = entry.path;
        boolean decrease = clickType == ClickType.LEFT;

        if (type == Boolean.class) {
            boolean current = config.getBoolean(path, defaultBoolean());
            mgr.setValue((Config.ConfigEntry<Boolean>) entry, !current);
            notifyWindows();
            return;
        }

        // Enum types: any click → browse + select menu
        if (type.isEnum()) {
            new EnumSelectionMenu(swordPlayer, entry, reopenMenu).open();
            return;
        }

        // Color types: any click → typed input
        if (type == Color.class) {
            promptColorInput(player, mgr, config, false);
            return;
        }

        if (type == TextColor.class) {
            promptColorInput(player, mgr, config, true);
            return;
        }

        if (type == Vector.class) {
            promptVectorInput(player, mgr, config);
            return;
        }

        // Shift+left for numeric types → typed input
        if (clickType == ClickType.SHIFT_LEFT && isNumeric(type)) {
            promptNumericInput(player, mgr, config, type, path);
            return;
        }

        if (type == Integer.class) {
            boolean shift = clickType.isShiftClick();
            int delta = shift ? INT_SHIFT_STEP : INT_STEP;
            int current = config.getInt(path, defaultInt());
            mgr.setValue((Config.ConfigEntry<Integer>) entry, current + (decrease ? -delta : delta));
            notifyWindows();
            return;
        }

        if (type == Long.class) {
            boolean shift = clickType.isShiftClick();
            long delta = shift ? LONG_SHIFT_STEP : LONG_STEP;
            long current = config.getLong(path, defaultLong());
            mgr.setValue((Config.ConfigEntry<Long>) entry, current + (decrease ? -delta : delta));
            notifyWindows();
            return;
        }

        if (type == Double.class) {
            boolean shift = clickType.isShiftClick();
            double delta = shift ? DOUBLE_SHIFT_STEP : DOUBLE_STEP;
            double current = config.getDouble(path, defaultDouble());
            double next = Math.round((current + (decrease ? -delta : delta)) * 100000.0) / 100000.0;
            mgr.setValue((Config.ConfigEntry<Double>) entry, next);
            notifyWindows();
            return;
        }

        if (type == Float.class) {
            boolean shift = clickType.isShiftClick();
            double delta = shift ? DOUBLE_SHIFT_STEP : DOUBLE_STEP;
            double current = config.getDouble(path, (double) defaultFloat());
            float next = (float)(Math.round((current + (decrease ? -delta : delta)) * 100000.0) / 100000.0);
            mgr.setValue((Config.ConfigEntry<Float>) entry, next);
            notifyWindows();
        }
    }

    // -------------------------------------------------------------------------
    //  Typed-input via chat — numerics
    // -------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private void promptNumericInput(Player player, ConfigManager mgr, FileConfiguration config,
                                    Class<?> type, String path) {
        String current;
        if (type == Integer.class) {
            current = String.valueOf(config.getInt(path, defaultInt()));
        } else if (type == Long.class) {
            current = String.valueOf(config.getLong(path, defaultLong()));
        } else if (type == Double.class) {
            current = String.format("%.4f", config.getDouble(path, defaultDouble()));
        } else {
            current = String.format("%.4f", (float) config.getDouble(path, defaultFloat()));
        }

        Component prompt = Component.text("Enter value for ", NamedTextColor.YELLOW)
            .append(Component.text(path, NamedTextColor.WHITE))
            .append(Component.text(" (current: ", NamedTextColor.YELLOW))
            .append(Component.text(current, NamedTextColor.GREEN))
            .append(Component.text("):", NamedTextColor.YELLOW));

        ChatInputCapture.prompt(player, prompt, input -> {
            if (input.equalsIgnoreCase("cancel")) {
                swordPlayer.message(Component.text("Cancelled.", NamedTextColor.GRAY));
                reopenMenu.run();
                return;
            }
            try {
                if (type == Integer.class) {
                    mgr.setValue((Config.ConfigEntry<Integer>) entry, Integer.parseInt(input));
                } else if (type == Long.class) {
                    mgr.setValue((Config.ConfigEntry<Long>) entry, Long.parseLong(input));
                } else if (type == Double.class) {
                    mgr.setValue((Config.ConfigEntry<Double>) entry, Double.parseDouble(input));
                } else {
                    mgr.setValue((Config.ConfigEntry<Float>) entry, Float.parseFloat(input));
                }
                swordPlayer.message(Component.text("Set ", NamedTextColor.GREEN)
                    .append(Component.text(path, NamedTextColor.WHITE))
                    .append(Component.text(" = ", NamedTextColor.GREEN))
                    .append(Component.text(input, NamedTextColor.YELLOW)));
            } catch (NumberFormatException ex) {
                swordPlayer.message(Component.text("Invalid number: ", NamedTextColor.RED)
                    .append(Component.text(input, NamedTextColor.WHITE)));
            }
            reopenMenu.run();
        });
    }

    // -------------------------------------------------------------------------
    //  Typed-input via chat — colors
    // -------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private void promptColorInput(Player player, ConfigManager mgr, FileConfiguration config, boolean isTextColor) {
        String path = entry.path;
        String current = isTextColor
            ? textColorToString(Config.loadTextColor(config, path, defaultTextColor()))
            : colorToString(Config.loadColor(config, path, defaultBukkitColor()));

        Component prompt = Component.text("Enter ", NamedTextColor.YELLOW)
            .append(Component.text("r, g, b", NamedTextColor.WHITE))
            .append(Component.text(" (0\u2013255) for ", NamedTextColor.YELLOW))
            .append(Component.text(path, NamedTextColor.WHITE))
            .append(Component.text(" (current: ", NamedTextColor.YELLOW))
            .append(Component.text(current, NamedTextColor.GREEN))
            .append(Component.text("):", NamedTextColor.YELLOW));

        ChatInputCapture.prompt(player, prompt, input -> {
            if (input.equalsIgnoreCase("cancel")) {
                swordPlayer.message(Component.text("Cancelled.", NamedTextColor.GRAY));
                reopenMenu.run();
                return;
            }
            try {
                String[] parts = input.split("[,\\s_]+");
                if (parts.length != 3) {
                    swordPlayer.message(Component.text("Expected format: ", NamedTextColor.RED)
                        .append(Component.text("r, g, b", NamedTextColor.WHITE))
                        .append(Component.text(" \u2014 got: ", NamedTextColor.RED))
                        .append(Component.text(input, NamedTextColor.WHITE)));
                    reopenMenu.run();
                    return;
                }
                int r = Integer.parseInt(parts[0].trim());
                int g = Integer.parseInt(parts[1].trim());
                int b = Integer.parseInt(parts[2].trim());
                if (r < 0 || r > 255 || g < 0 || g > 255 || b < 0 || b > 255) {
                    swordPlayer.message(Component.text("Each channel must be 0\u2013255.", NamedTextColor.RED));
                    reopenMenu.run();
                    return;
                }
                if (isTextColor) {
                    mgr.setValue((Config.ConfigEntry<TextColor>) entry, TextColor.color(r, g, b));
                } else {
                    mgr.setValue((Config.ConfigEntry<Color>) entry, Color.fromRGB(r, g, b));
                }
                swordPlayer.message(Component.text("Set ", NamedTextColor.GREEN)
                    .append(Component.text(path, NamedTextColor.WHITE))
                    .append(Component.text(" = ", NamedTextColor.GREEN))
                    .append(Component.text("r=" + r + " g=" + g + " b=" + b, NamedTextColor.YELLOW)));
            } catch (NumberFormatException ex) {
                swordPlayer.message(Component.text("Invalid number in: ", NamedTextColor.RED)
                    .append(Component.text(input, NamedTextColor.WHITE)));
            }
            reopenMenu.run();
        });
    }

    // -------------------------------------------------------------------------
    //  Typed-input via chat — vectors
    // -------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private void promptVectorInput(Player player, ConfigManager mgr, FileConfiguration config) {
        String path = entry.path;
        Vector current = Config.loadVector(config, path, defaultVector());

        Component prompt = Component.text("Enter ", NamedTextColor.YELLOW)
            .append(Component.text("x y z", NamedTextColor.WHITE))
            .append(Component.text(" for ", NamedTextColor.YELLOW))
            .append(Component.text(path, NamedTextColor.WHITE))
            .append(Component.text(" (current: ", NamedTextColor.YELLOW))
            .append(Component.text(vectorToString(current), NamedTextColor.GREEN))
            .append(Component.text("):", NamedTextColor.YELLOW));

        ChatInputCapture.prompt(player, prompt, input -> {
            if (input.equalsIgnoreCase("cancel")) {
                swordPlayer.message(Component.text("Cancelled.", NamedTextColor.GRAY));
                reopenMenu.run();
                return;
            }
            try {
                String[] parts = input.split("[,\\s]+");
                if (parts.length != 3) {
                    swordPlayer.message(Component.text("Expected format: ", NamedTextColor.RED)
                        .append(Component.text("x y z", NamedTextColor.WHITE))
                        .append(Component.text(" \u2014 got: ", NamedTextColor.RED))
                        .append(Component.text(input, NamedTextColor.WHITE)));
                    reopenMenu.run();
                    return;
                }
                double x = Double.parseDouble(parts[0].trim());
                double y = Double.parseDouble(parts[1].trim());
                double z = Double.parseDouble(parts[2].trim());
                Vector next = new Vector(x, y, z);
                mgr.setValue((Config.ConfigEntry<Vector>) entry, next);
                swordPlayer.message(Component.text("Set ", NamedTextColor.GREEN)
                    .append(Component.text(path, NamedTextColor.WHITE))
                    .append(Component.text(" = ", NamedTextColor.GREEN))
                    .append(Component.text(vectorToString(next), NamedTextColor.YELLOW)));
            } catch (NumberFormatException ex) {
                swordPlayer.message(Component.text("Invalid number in: ", NamedTextColor.RED)
                    .append(Component.text(input, NamedTextColor.WHITE)));
            }
            reopenMenu.run();
        });
    }

    // -------------------------------------------------------------------------
    //  Helpers
    // -------------------------------------------------------------------------

    private static boolean isNumeric(Class<?> type) {
        return type == Integer.class || type == Long.class || type == Double.class || type == Float.class;
    }

    private static String vectorToString(Vector v) {
        return String.format("%.4f %.4f %.4f", v.getX(), v.getY(), v.getZ());
    }

    private static String colorToString(Color c) {
        return "r=" + c.getRed() + " g=" + c.getGreen() + " b=" + c.getBlue();
    }

    private static String textColorToString(TextColor tc) {
        if (tc == null) return "null";
        int r = (tc.value() >> 16) & 0xFF;
        int g = (tc.value() >> 8) & 0xFF;
        int b = tc.value() & 0xFF;
        return "r=" + r + " g=" + g + " b=" + b;
    }

    // -------------------------------------------------------------------------
    //  Default value helpers — safe casts with fallback
    // -------------------------------------------------------------------------

    private boolean defaultBoolean() {
        return entry.defaultValue instanceof Boolean b ? b : false;
    }

    private int defaultInt() {
        return entry.defaultValue instanceof Integer i ? i : 0;
    }

    private long defaultLong() {
        return entry.defaultValue instanceof Long l ? l : 0L;
    }

    private double defaultDouble() {
        return entry.defaultValue instanceof Double d ? d : 0.0;
    }

    private float defaultFloat() {
        return entry.defaultValue instanceof Float f ? f : 0.0f;
    }

    private Vector defaultVector() {
        return entry.defaultValue instanceof Vector v ? v : new Vector();
    }

    private Color defaultBukkitColor() {
        return entry.defaultValue instanceof Color c ? c : Color.WHITE;
    }

    private TextColor defaultTextColor() {
        return entry.defaultValue instanceof TextColor tc ? tc : TextColor.color(255, 255, 255);
    }
}
