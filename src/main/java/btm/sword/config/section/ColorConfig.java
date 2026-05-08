package btm.sword.config.section;

import static btm.sword.config.Config.register;

import org.bukkit.Color;
import org.jetbrains.annotations.Nullable;

import btm.sword.config.Config;
import net.kyori.adventure.text.format.TextColor;

/**
 * All configurable Adventure {@link net.kyori.adventure.text.format.TextColor} and Bukkit
 * {@link org.bukkit.Color} values used for UI text, particle effects, and glow tints.
 * Hot-reloaded via {@code /sword reload}.
 */
public final class ColorConfig {

    private ColorConfig() {}

    public static TextColor TEXT_RESOURCE_COLOR = TextColor.color(222, 222, 222);
    static { register(
        "color.text_resource_color",
        TEXT_RESOURCE_COLOR, TextColor.class,
        v -> TEXT_RESOURCE_COLOR = v,
        Config::loadTextColor
    ); }

    public static TextColor TEXT_ASPECT_COLOR = TextColor.color(0, 159, 255);
    static { register(
        "color.text_aspect_color",
        TEXT_ASPECT_COLOR, TextColor.class,
        v -> TEXT_ASPECT_COLOR = v,
        Config::loadTextColor
    ); }

    public static TextColor TITLE_INPUT_STRING = TextColor.color(151, 0, 0);
    static { register(
        "color.title_input_string",
        TITLE_INPUT_STRING, TextColor.class,
        v -> TITLE_INPUT_STRING = v,
        Config::loadTextColor
    ); }

    public static TextColor TEXT_ITEM_NAME = TextColor.color(0, 110, 151);
    static { register(
        "color.text_item_name",
        TEXT_ITEM_NAME, TextColor.class,
        v -> TEXT_ITEM_NAME = v,
        Config::loadTextColor
    ); }

    public static TextColor TEXT_ITEM_CONTROLS = TextColor.color(173, 109, 255);
    static { register(
        "color.text_item_controls",
        TEXT_ITEM_CONTROLS, TextColor.class,
        v -> TEXT_ITEM_CONTROLS = v,
        Config::loadTextColor
    ); }

    public static TextColor TEXT_ITEM_HEADER = TextColor.color(225, 225, 225);
    static { register(
        "color.text_item_header",
        TEXT_ITEM_HEADER, TextColor.class,
        v -> TEXT_ITEM_HEADER = v,
        Config::loadTextColor
    ); }

    public static TextColor TEXT_ITEM_BASE = TextColor.color(121, 142, 168);
    static { register(
        "color.text_item_base",
        TEXT_ITEM_BASE, TextColor.class,
        v -> TEXT_ITEM_BASE = v,
        Config::loadTextColor
    ); }

    public static TextColor TEXT_COOL = TextColor.color(240, 161, 12);
    static { register(
        "color.text_cool",
        TEXT_COOL, TextColor.class,
        v -> TEXT_COOL = v,
        Config::loadTextColor
    ); }

    public static TextColor TEXT_COOL_DARK = TextColor.color(51, 60, 75);
    static { register(
        "color.text_cool_dark",
        TEXT_COOL_DARK, TextColor.class,
        v -> TEXT_COOL_DARK = v,
        Config::loadTextColor
    ); }

    public static Color UMBRAL_GLOW = Color.fromRGB(36, 8, 8);
    static { register("color.umbral_glow",
        UMBRAL_GLOW, Color.class,
        v -> UMBRAL_GLOW = v,
        Config::loadColor
    ); }

    public static @Nullable Color FEROCIOUS_SWEEP = Color.fromRGB(255, 0, 0);
    static { register("color.ferocious_sweep",
        FEROCIOUS_SWEEP, Color.class,
        v -> FEROCIOUS_SWEEP = v,
        Config::loadColor
    ); }

    public static @Nullable Color STANDBY_GLOW = Color.fromRGB(255, 255, 255);
    static { register("color.standby_glow",
        STANDBY_GLOW, Color.class,
        v -> STANDBY_GLOW = v,
        Config::loadColor
    ); }

    public static @Nullable Color ATTACK_QUICK_GLOW = Color.fromRGB(181, 121, 27);
    static { register("color.attack_quick_glow",
        ATTACK_QUICK_GLOW, Color.class,
        v -> ATTACK_QUICK_GLOW = v,
        Config::loadColor
    ); }

    public static @Nullable Color LUNGE_GLOW = Color.fromRGB(255, 0, 0);
    static { register("color.lunge_glow",
        LUNGE_GLOW, Color.class,
        v -> LUNGE_GLOW = v,
        Config::loadColor
    ); }

    public static @Nullable Color LODGED_GLOW = Color.fromRGB(0, 0, 0);
    static { register("color.lodged_glow",
        LODGED_GLOW, Color.class,
        v -> LODGED_GLOW = v,
        Config::loadColor
    ); }

    public static @Nullable Color GRAB_IMPALE_GLOW = Color.fromRGB(255, 80, 0);
    static { register("color.grab_impale_glow",
        GRAB_IMPALE_GLOW, Color.class,
        v -> GRAB_IMPALE_GLOW = v,
        Config::loadColor
    ); }

    public static @Nullable Color RECALL_GLOW = Color.fromRGB(255, 118, 135);
    static { register("color.recall_glow",
        RECALL_GLOW, Color.class,
        v -> RECALL_GLOW = v,
        Config::loadColor
    ); }
}
