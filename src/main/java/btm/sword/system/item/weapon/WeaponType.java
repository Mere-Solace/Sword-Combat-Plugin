package btm.sword.system.item.weapon;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import btm.sword.system.action.throwing.ImpactType;
import btm.sword.system.attack.style.WeaponAttackStyle;
import btm.sword.system.item.ItemStackBuilder;
import btm.sword.system.item.KeyRegistry;
import btm.sword.system.item.SwordItemType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

/**
 * Enumeration of all weapon types available in the Sword plugin.
 *
 * <p>Each constant encapsulates the weapon's identity (id, display name, icon) and
 * provides a canonical {@link #buildItemStack()} factory. Items are tagged with
 * {@link KeyRegistry#WEAPON_TYPE_KEY} so they can be identified at runtime via
 * {@link #fromItem(ItemStack)}.</p>
 */
public enum WeaponType {

    /** A fast, versatile one-handed sword. */
    FALCHION("falchion", "Falchion", Material.STONE_SWORD) {
        @Override
        public ItemStack buildItemStack() {
            return new ItemStackBuilder(Material.STONE_SWORD)
                .hideAll()
                .name(Component.text("Falchion", TextColor.color(254, 56, 0), TextDecoration.BOLD)
                    .decoration(TextDecoration.ITALIC, false))
                .lore(List.of(
                    Component.text().content("Veracity").color(TextColor.color(160, 160, 160)).build(),
                    Component.text().content("&").color(TextColor.color(89, 89, 89)).build(),
                    Component.text().content("Assiduity").color(TextColor.color(160, 160, 160)).build()))
                .tagSwordItem(SwordItemType.BROAD_SWORD)
                .tagAttackStyle(WeaponAttackStyle.SLASH)
                .tagImpactType(ImpactType.IMPALE)
                .toughnessDamageAdder(5)
                .unbreakable(false)
                .setDamageValues(200, 1)
                .tag(KeyRegistry.WEAPON_TYPE_KEY, PersistentDataType.STRING, this.id())
                .build();
        }
    },

    /** A precision ranged weapon styled as a shovel. */
    INTRUSION_ENDER("intrusion_ender", "Intrusion Ender", Material.IRON_SHOVEL) {
        @Override
        public ItemStack buildItemStack() {
            return new ItemStackBuilder(Material.IRON_SHOVEL)
                .hideAll()
                .name(Component.text("Intrusion Ender", TextColor.color(0, 174, 200), TextDecoration.BOLD)
                    .decoration(TextDecoration.ITALIC, false))
                .lore(List.of(
                    Component.text().content("Veracity").color(TextColor.color(160, 160, 160)).build(),
                    Component.text().content("&").color(TextColor.color(89, 89, 89)).build(),
                    Component.text().content("Assiduity").color(TextColor.color(160, 160, 160)).build()))
                .tagSwordItem(SwordItemType.PISTOL)
                .toughnessDamageAdder(7)
                .unbreakable(false)
                .tag(KeyRegistry.WEAPON_TYPE_KEY, PersistentDataType.STRING, this.id())
                .build();
        }
    };

    private final String id;
    private final String displayName;
    private final Material icon;

    WeaponType(String id, String displayName, Material icon) {
        this.id = id;
        this.displayName = displayName;
        this.icon = icon;
    }

    /** Returns the unique string identifier for this weapon type. */
    public String id() {
        return id;
    }

    /** Returns the icon {@link Material} used to represent this weapon in menus. */
    public Material icon() {
        return icon;
    }

    /**
     * Returns a styled {@link Component} display name with the weapon's signature color,
     * bold weight, and italic decoration suppressed.
     *
     * @return the display name component
     */
    public Component displayName() {
        TextColor color = switch (this) {
            case FALCHION -> TextColor.color(254, 56, 0);
            case INTRUSION_ENDER -> TextColor.color(0, 174, 200);
        };
        return Component.text(displayName, color, TextDecoration.BOLD)
            .decoration(TextDecoration.ITALIC, false);
    }

    /**
     * Builds and returns a fresh {@link ItemStack} representing this weapon,
     * pre-tagged with all relevant persistent data keys.
     *
     * @return the configured weapon ItemStack
     */
    public abstract ItemStack buildItemStack();

    /**
     * Resolves the {@link WeaponType} from an {@link ItemStack} by reading the
     * {@link KeyRegistry#WEAPON_TYPE_KEY} persistent data tag.
     *
     * @param item the item to inspect
     * @return the matching WeaponType, or {@code null} if no match is found
     */
    public static WeaponType fromItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        String value = KeyRegistry.getKeyField(item, KeyRegistry.WEAPON_TYPE_KEY, PersistentDataType.STRING);
        if (value == null) return null;
        for (WeaponType type : values()) {
            if (type.id.equals(value)) return type;
        }
        return null;
    }
}
