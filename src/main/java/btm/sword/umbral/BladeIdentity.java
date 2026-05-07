package btm.sword.umbral;

import java.util.Objects;

import org.bukkit.inventory.ItemStack;

import btm.sword.entity.base.Combatant;
import btm.sword.item.special.SoulLinkItem;

/**
 * Immutable identity record for an UmbralBlade.
 *
 * <p>Collects the four pieces of "what this blade is" that were previously scattered across
 * {@link UmbralBlade}'s fields: the {@link Combatant} that owns the blade, the original
 * {@link ItemStack weapon} the blade was unsheathed from, the {@link SoulLinkItem link} anchor
 * displayed in the inventory while the blade is active, and the visual {@link ItemStack blade}
 * item displayed when the blade is wielded.</p>
 *
 * <p>Pure data — no behavior beyond record accessors. All four components are non-null;
 * the compact constructor enforces this structurally so an invalid identity cannot exist.
 * Once constructed, an instance never mutates.</p>
 *
 * <p>This record carries no lifecycle of its own. Its lifetime is bounded by the
 * {@link UmbralBlade} that holds it.</p>
 *
 * @param thrower the combatant that owns the blade
 * @param weapon  the original held weapon the blade was created from
 * @param link    the soul-link anchor occupying slot 0 while the blade is unwielded
 * @param blade   the visual blade item occupying slot 0 while the blade is wielded
 */
public record BladeIdentity(Combatant thrower, ItemStack weapon, SoulLinkItem link, ItemStack blade) {

    /** Compact constructor enforcing non-null invariants on all components. */
    public BladeIdentity {
        Objects.requireNonNull(thrower, "thrower");
        Objects.requireNonNull(weapon, "weapon");
        Objects.requireNonNull(link, "link");
        Objects.requireNonNull(blade, "blade");
    }
}
