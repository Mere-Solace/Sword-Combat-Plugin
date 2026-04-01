package btm.sword.system.interaction.smithing;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import btm.sword.system.action.throwing.ItemThrowStyle;
import btm.sword.system.interaction.CustomInteractionContext;
import btm.sword.system.interaction.CustomInventoryInteraction;
import btm.sword.system.item.KeyRegistry;
import btm.sword.system.item.weapon.WeaponType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

/**
 * Smithing-table interaction that rewrites a Falchion's throw style to a rotating toss.
 */
public final class RotateThrowStyleSmithingInteraction implements CustomInventoryInteraction {

    @Override
    public InventoryType inventoryType() {
        return InventoryType.SMITHING;
    }

    @Override
    public boolean matches(CustomInteractionContext context) {
        ItemStack template = context.topItem(CustomInteractionContext.SMITHING_TEMPLATE_SLOT);
        ItemStack base = context.topItem(CustomInteractionContext.SMITHING_BASE_SLOT);
        ItemStack addition = context.topItem(CustomInteractionContext.SMITHING_ADDITION_SLOT);

        if (!template.isEmpty() || !addition.isEmpty()) {
            return false;
        }
        if (base.isEmpty() || WeaponType.fromItem(base) != WeaponType.FALCHION) {
            return false;
        }

        String existing = KeyRegistry.getKeyField(base, KeyRegistry.THROW_STYLE_KEY, PersistentDataType.STRING);
        return !ItemThrowStyle.ROTATE.name().equals(existing);
    }

    @Override
    public ItemStack createResult(CustomInteractionContext context) {
        ItemStack result = context.topItem(CustomInteractionContext.SMITHING_BASE_SLOT).clone();
        KeyRegistry.setKeyField(result, KeyRegistry.THROW_STYLE_KEY, PersistentDataType.STRING, ItemThrowStyle.ROTATE.name());

        ItemMeta meta = result.getItemMeta();
        List<Component> lore = meta.lore();
        List<Component> updatedLore = lore == null ? new ArrayList<>() : new ArrayList<>(lore);
        updatedLore.add(Component.text("Smithing Refit: Rotating Throw", NamedTextColor.GOLD)
            .decoration(TextDecoration.ITALIC, false));
        meta.lore(updatedLore);
        result.setItemMeta(meta);
        return result;
    }

    @Override
    public void consumeInputs(CustomInteractionContext context) {
        context.setTopItem(CustomInteractionContext.SMITHING_BASE_SLOT, ItemStack.of(Material.AIR));
        context.setTopItem(CustomInteractionContext.SMITHING_RESULT_SLOT, ItemStack.of(Material.AIR));
    }
}
