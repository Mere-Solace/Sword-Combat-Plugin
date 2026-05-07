package btm.sword.action.throwing;

import btm.sword.entity.base.SwordEntity;

/**
 * Behavioral contract for {@link InteractiveItem}s that can become lodged in (or attached to) a
 * {@link SwordEntity} after a successful hit.
 * <p>
 * Extracted from {@link btm.sword.action.throwing.types.ThrownItem} so consumers such as
 * {@link InteractiveItemArbiter} can dispatch on behavior rather than concrete type — the
 * UmbralBlade and any future lodge-capable item can satisfy this contract without participating
 * in the {@code ThrownItem} inheritance chain.
 */
public interface Lodgeable extends InteractiveItem {

    /**
     * Returns the entity this item is currently lodged in, or {@code null} if it is not currently
     * impaling/attached to anything.
     *
     * @return the impaled entity, or {@code null} if not lodged
     */
    SwordEntity getImpaledEntity();

    /**
     * Marks this item as having been retrieved by an entity (typically via the dash-pickup flow).
     * Implementations are expected to debounce this flag — see existing usages for the convention.
     *
     * @param retrieved {@code true} when the item is being picked up
     */
    void setRetrieved(boolean retrieved);

    /**
     * Returns {@code true} if this item has been marked as retrieved. Used by the impalement
     * watchdog to decide when its bookkeeping should release the impaled entity.
     *
     * @return whether the item is currently flagged as retrieved
     */
    boolean isRetrieved();

    /**
     * Disposes this item and emits a fresh interactive world-item at its current location.
     * Used by impalement bookkeeping to drop the lodged item back into the world when the
     * impalement ends without the item being explicitly retrieved.
     */
    void disposeWithNewInteractiveItem();
}
