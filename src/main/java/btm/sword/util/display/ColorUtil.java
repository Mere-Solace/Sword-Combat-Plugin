package btm.sword.util.display;

import org.bukkit.Color;

public final class ColorUtil {

    private ColorUtil() {}

    /**
     * Converts a hex string (e.g. "#RRGGBB") into a Bukkit Color.
     * Accepts leading '#' or without it. Returns black if invalid.
     */
    public static Color fromHex(String hex) {
        if (hex == null || hex.isEmpty()) {
            return Color.fromRGB(0, 0, 0);
        }

        try {
            String clean = hex.startsWith("#") ? hex.substring(1) : hex;
            int rgb = Integer.parseInt(clean, 16);
            int r = (rgb >> 16) & 0xFF;
            int g = (rgb >> 8) & 0xFF;
            int b = rgb & 0xFF;
            return Color.fromRGB(r, g, b);
        } catch (NumberFormatException e) {
            return Color.fromRGB(0, 0, 0);
        }
    }
}
