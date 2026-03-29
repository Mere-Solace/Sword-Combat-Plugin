package btm.sword.gamemode.ctf;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;

import btm.sword.config.Config;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

/**
 * Represents one of the two teams in a Capture the Flag match.
 * <p>
 * Each team owns a banner colour, a spawn location (read from {@link Config.Ctf}), and a
 * flag spawn location (same point — the base zone center). Call {@link #createFlagItem()} to
 * produce a patterned banner {@link ItemStack} suitable for a player helmet or an
 * {@link org.bukkit.entity.ArmorStand} helmet.
 * </p>
 */
public enum CtfTeam {

    /** The red team. Spawn located at {@code Config.Ctf.RED_SPAWN_*}. */
    RED(DyeColor.RED, Material.RED_BANNER, NamedTextColor.RED),

    /** The blue team. Spawn located at {@code Config.Ctf.BLUE_SPAWN_*}. */
    BLUE(DyeColor.BLUE, Material.BLUE_BANNER, NamedTextColor.BLUE);

    private final DyeColor dyeColor;
    private final Material bannerMaterial;
    private final NamedTextColor textColor;

    CtfTeam(DyeColor dyeColor, Material bannerMaterial, NamedTextColor textColor) {
        this.dyeColor = dyeColor;
        this.bannerMaterial = bannerMaterial;
        this.textColor = textColor;
    }

    /**
     * Returns the DyeColor associated with this team.
     *
     * @return team dye colour
     */
    public DyeColor getDyeColor() {
        return dyeColor;
    }

    /**
     * Returns the Adventure text colour for this team, used in chat/title messages.
     *
     * @return team text colour
     */
    public NamedTextColor getTextColor() {
        return textColor;
    }

    /**
     * Returns the opposing team.
     *
     * @return the other {@link CtfTeam}
     */
    public CtfTeam enemy() {
        return this == RED ? BLUE : RED;
    }

    /**
     * Returns the team's spawn location, read live from {@link Config.Ctf}.
     * <p>
     * Falls back to the server's first world if the configured world name is not loaded.
     * </p>
     *
     * @return team spawn {@link Location}
     */
    public Location getSpawnLocation() {
        org.bukkit.World world = Bukkit.getWorld(Config.Ctf.SPAWN_WORLD);
        if (world == null) world = Bukkit.getWorlds().get(0);

        if (this == RED) {
            return new Location(world, Config.Ctf.RED_SPAWN_X, Config.Ctf.RED_SPAWN_Y, Config.Ctf.RED_SPAWN_Z);
        } else {
            return new Location(world, Config.Ctf.BLUE_SPAWN_X, Config.Ctf.BLUE_SPAWN_Y, Config.Ctf.BLUE_SPAWN_Z);
        }
    }

    /**
     * Creates a team-coloured banner {@link ItemStack} with distinctive patterns.
     * <p>
     * RED team:  base red  + white CROSS (+) + white BORDER.<br>
     * BLUE team: base blue + white STRAIGHT_CROSS (×) + white BORDER.
     * </p>
     *
     * @return a named, patterned banner item
     */
    public ItemStack createFlagItem() {
        ItemStack banner = new ItemStack(bannerMaterial);
        BannerMeta meta = (BannerMeta) banner.getItemMeta();

        if (this == RED) {
            meta.setPatterns(List.of(
                new Pattern(DyeColor.WHITE, PatternType.CROSS),
                new Pattern(DyeColor.WHITE, PatternType.BORDER)
            ));
        } else {
            meta.setPatterns(List.of(
                new Pattern(DyeColor.WHITE, PatternType.STRAIGHT_CROSS),
                new Pattern(DyeColor.WHITE, PatternType.BORDER)
            ));
        }

        meta.displayName(Component.text(name() + " FLAG", textColor, TextDecoration.BOLD)
            .decoration(TextDecoration.ITALIC, false));
        banner.setItemMeta(meta);
        return banner;
    }
}
