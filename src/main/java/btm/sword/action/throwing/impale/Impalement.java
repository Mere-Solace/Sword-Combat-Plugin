package btm.sword.action.throwing.impale;

import btm.sword.action.throwing.types.ThrownItem;
import btm.sword.entity.base.SwordEntity;
import btm.sword.runtime.scheduler.PredicateRunnablePair;
import btm.sword.runtime.scheduler.TimeArbiter;
import lombok.Getter;
import lombok.Setter;

/** Represents an active impalement state binding a thrown item to an impaled entity. */
public class Impalement {
    @Getter
    private final SwordEntity impaledEntity;
    @Setter
    private boolean shouldDispose = false;

    /** Constructs an impalement record for the given entity. */
    public Impalement(SwordEntity impaledEntity) {
        this.impaledEntity = impaledEntity;
    }

    /** Starts a recurring task that disposes the impalement once the item is retrieved or the entity dies. */
    public void startShouldDisposeCheckTask(SwordEntity hitEntity, ThrownItem impalingItem) {
        TimeArbiter.runTimeIndependentBukkitTaskOnTimer(
            null,
            null,
            0, 75,
            Impalement.class, "startShouldDisposeCheckTask",
            new PredicateRunnablePair(
                () -> shouldDispose || impalingItem.getHitEntity() == null ||
                    hitEntity.isDead() ||
                    impalingItem.getDisplay() == null,
                impalingItem::disposeWithNewInteractiveItem
            ),
            new PredicateRunnablePair(
                () -> impalingItem.getDisplay().isDead() || impalingItem.isRetrieved(),
                () -> hitEntity.removeImpalement(this)
            )
        );
    }
}
