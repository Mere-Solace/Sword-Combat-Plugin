package btm.sword.system.action.throwing.impale;

import btm.sword.system.action.throwing.ThrownItem;
import btm.sword.system.control.PredicateRunnablePair;
import btm.sword.system.control.TimeArbiter;
import btm.sword.system.entity.base.SwordEntity;
import lombok.Getter;
import lombok.Setter;



public class Impalement {
    @Getter
    private final SwordEntity impaledEntity;
    @Setter
    private boolean shouldDispose = false;

    public Impalement(SwordEntity impaledEntity) {
        this.impaledEntity = impaledEntity;
    }

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
