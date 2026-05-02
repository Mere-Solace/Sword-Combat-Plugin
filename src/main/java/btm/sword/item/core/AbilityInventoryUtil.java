package btm.sword.item.core;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import btm.sword.action.skill.SkillId;

/**
 * Utility methods for locating and consuming physical ability items in a player's inventory.
 *
 * <p>Ability items are identified by the {@link KeyRegistry#ABILITY_ID_KEY} PDC tag
 * stamped on them by {@link AbilityItemBuilder}.
 */
public final class AbilityInventoryUtil {

    private AbilityInventoryUtil() {}

    /**
     * Finds the first {@link ItemStack} in the player's inventory that matches the given ability id.
     *
     * @param player the player whose inventory to scan
     * @param id     the ability id to match against
     * @return the matching item, or {@code null} if none found
     */
    @Nullable
    public static ItemStack findAbilityItem(Player player, SkillId id) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.isEmpty()) continue;
            SkillId found = AbilityItemReader.readId(item);
            if (id.equals(found)) return item;
        }
        return null;
    }

    /**
     * Returns {@code true} if the player has at least one physical item for the given ability id.
     *
     * @param player the player to check
     * @param id     the ability id to match
     * @return {@code true} if found
     */
    public static boolean hasAbilityItem(Player player, SkillId id) {
        return findAbilityItem(player, id) != null;
    }

    /**
     * Removes one ability item matching the given id from the player's inventory.
     * If the stack has more than one item, the count is decremented. Otherwise the
     * slot is cleared.
     *
     * @param player the player whose inventory to modify
     * @param id     the ability id to match
     * @return {@code true} if an item was found and removed
     */
    public static boolean removeAbilityItem(Player player, SkillId id) {
        ItemStack item = findAbilityItem(player, id);
        if (item == null) return false;

        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            item.setAmount(0); // Bukkit removes the slot when set to 0
        }
        return true;
    }
}
