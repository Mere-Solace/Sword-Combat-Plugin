package btm.sword.system.item;

import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public final class ItemUsageManager {

    private ItemUsageManager() {}

    public static boolean isUnbreakable(ItemStack itemStack) {
        var value = KeyRegistry.getKeyField(itemStack, KeyRegistry.BREAKABLE_WEAPON_KEY, PersistentDataType.BOOLEAN);
        return value != null && value;
    }

    public static void damageItemStack(ItemStack itemStack, int amount, LivingEntity user) {
        if (isUnbreakable(itemStack)) return;
        itemStack.damage(amount, user);
    }

    public static int currentItemDamage(ItemStack itemStack) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return -1;

        if (meta instanceof Damageable damageable) {
            if (!damageable.hasMaxDamage()) {
                damageable.setMaxDamage(100);
            }
            if (!damageable.hasDamageValue()) {
                damageable.setDamage(damageable.getMaxDamage());
            }
            return damageable.getDamage();
        }
        return -1;
    }

    public static int maxItemDamage(ItemStack itemStack) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return -1;

        if (meta instanceof Damageable damageable) {
            if (!damageable.hasMaxDamage()) {
                damageable.setMaxDamage(100);
            }
            if (!damageable.hasDamageValue()) {
                damageable.setDamage(damageable.getMaxDamage());
            }
            return damageable.getMaxDamage();
        }
        return -1;
    }
}
