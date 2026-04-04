package btm.sword.system.attack.simulation;

import java.util.concurrent.ConcurrentLinkedQueue;

import org.bukkit.util.Vector;

import btm.sword.system.entity.SwordEntityArbiter;
import btm.sword.system.entity.base.SwordEntity;
import btm.sword.system.entity.impl.Combatant;

/**
 * Thread-safe bridge delivering {@link CollisionEvent}s from the off-thread
 * {@code VolumeSimulation} to the Bukkit main thread.
 * <p>
 * The simulation thread enqueues events via {@link #post(CollisionEvent)}.
 * A main-thread repeating task calls {@link #drainToMain()} every server tick (50ms)
 * to resolve UUIDs and dispatch hits.
 * </p>
 */
public final class CollisionEventBridge {

    /** Global singleton instance. */
    public static final CollisionEventBridge INSTANCE = new CollisionEventBridge();

    private static final Vector ZERO_KNOCKBACK = new Vector(0, 0, 0);

    private final ConcurrentLinkedQueue<CollisionEvent> queue = new ConcurrentLinkedQueue<>();

    private CollisionEventBridge() {}

    /**
     * Enqueues a collision event for main-thread processing.
     * Safe to call from the simulation thread.
     *
     * @param event the collision event to post
     */
    public void post(CollisionEvent event) {
        queue.add(event);
    }

    /**
     * Drains all queued events and dispatches hits on the main thread.
     * Must be called from the Bukkit main thread.
     * <p>
     * For each event: resolves both UUIDs via {@link SwordEntityArbiter},
     * skips the event if either entity is no longer registered or the attacker
     * is not a {@link Combatant}, then calls {@code victim.hit()}.
     * </p>
     */
    public void drainToMain() {
        CollisionEvent event;
        while ((event = queue.poll()) != null) {
            SwordEntity attacker = SwordEntityArbiter.getByUuid(event.attackerUuid());
            SwordEntity victim = SwordEntityArbiter.getByUuid(event.victimUuid());

            if (attacker == null || victim == null) continue;
            if (!(attacker instanceof Combatant combatant)) continue;

            victim.hit(combatant, event.hitValue(), ZERO_KNOCKBACK);
        }
    }
}
