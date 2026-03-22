package btm.sword.system.playerdata;

import java.util.EnumMap;
import java.util.Map;

import btm.sword.system.item.material.MaterialType;
import lombok.Getter;

/**
 * Session-scoped storage container for a player's economy-related data:
 * crafting materials, steel credits, and per-category auto-pickup preferences.
 * <p>
 * This object is initialized fresh each login and is not persisted — it lives on
 * {@link btm.sword.system.entity.impl.SwordPlayer} for the duration of the session.
 * Persistence will be wired in once the database hook is in place.
 * </p>
 *
 * <p>All material counts are keyed by {@link MaterialType}; missing entries are treated
 * as zero. Credits start at a default value; toggles default to {@code false}.</p>
 */
public class PlayerStorage {

    private static final int STARTING_CREDITS = 100;

    private final Map<MaterialType, Integer> materialCounts = new EnumMap<>(MaterialType.class);

    @Getter
    private int steelCredits = STARTING_CREDITS;

    @Getter
    private boolean autoPickupMaterials = false;

    @Getter
    private boolean autoPickupCredits = false;

    // ── Materials ──────────────────────────────────────────────────────────────

    /**
     * Returns the stored count for the given material type, or {@code 0} if none stored.
     *
     * @param type the material type to query
     * @return the current count in storage
     */
    public int getMaterialCount(MaterialType type) {
        return materialCounts.getOrDefault(type, 0);
    }

    /**
     * Returns the sum of all stored material counts across every type.
     * Used to drive the material pouch lore display.
     *
     * @return total items across all material types
     */
    public int getTotalMaterialSlots() {
        return materialCounts.values().stream().mapToInt(Integer::intValue).sum();
    }

    /**
     * Adds the given amount to the stored count for a material type.
     *
     * @param type   the material type to add
     * @param amount the positive quantity to add
     */
    public void addMaterial(MaterialType type, int amount) {
        if (amount <= 0) return;
        materialCounts.merge(type, amount, Integer::sum);
    }

    /**
     * Attempts to remove the given amount from the stored count for a material type.
     * Returns {@code false} and makes no change if insufficient stock.
     *
     * @param type   the material type to remove
     * @param amount the quantity to remove
     * @return {@code true} if successful, {@code false} if not enough stock
     */
    public boolean removeMaterial(MaterialType type, int amount) {
        int current = getMaterialCount(type);
        if (current < amount) return false;
        if (current == amount) {
            materialCounts.remove(type);
        } else {
            materialCounts.put(type, current - amount);
        }
        return true;
    }

    // ── Credits ────────────────────────────────────────────────────────────────

    /**
     * Adds the given amount to the player's steel credit balance.
     *
     * @param amount the positive quantity to add
     */
    public void addCredits(int amount) {
        if (amount <= 0) return;
        steelCredits += amount;
    }

    /**
     * Attempts to remove the given amount from the player's steel credit balance.
     * Returns {@code false} and makes no change if insufficient balance.
     *
     * @param amount the quantity to spend
     * @return {@code true} if successful, {@code false} if not enough credits
     */
    public boolean removeCredits(int amount) {
        if (steelCredits < amount) return false;
        steelCredits -= amount;
        return true;
    }

    // ── Toggles ────────────────────────────────────────────────────────────────

    /** Flips the auto-pickup-materials toggle. */
    public void toggleAutoPickupMaterials() {
        autoPickupMaterials = !autoPickupMaterials;
    }

    /** Flips the auto-pickup-credits toggle. */
    public void toggleAutoPickupCredits() {
        autoPickupCredits = !autoPickupCredits;
    }
}
