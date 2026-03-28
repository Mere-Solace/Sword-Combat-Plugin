package btm.sword.gamemode;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import btm.sword.gamemode.type.CaptureTheFlag1v1;
import btm.sword.gamemode.type.Gamemode;
import btm.sword.system.entity.impl.SwordPlayer;

/**
 * Manages per-{@link Gamemode} matchmaking queues.
 * <p>
 * Players are enqueued via {@link #enqueue} and matched when the arena is available and
 * the queue has at least two players. Arena integration is not yet implemented — see
 * {@link #tryStartNextMatch()}.
 * </p>
 */
public class QueueManager {

    private static final Map<Class<? extends Gamemode>, Queue<SwordPlayer>> queueMap;

    static {
        queueMap = new HashMap<>();
        queueMap.put(CaptureTheFlag1v1.class, new ConcurrentLinkedQueue<>());
    }

    /**
     * Adds the given player to the queue for the specified game mode.
     * No-ops (with a message) if the player is already queued.
     *
     * @param gamemode     the game mode class to queue for
     * @param swordPlayer  the player requesting to join the queue
     */
    public static void enqueue(Class<? extends Gamemode> gamemode, SwordPlayer swordPlayer) {
        Queue<SwordPlayer> currentPlayerQueue = queueMap.get(gamemode);
        if (currentPlayerQueue.contains(swordPlayer)) {
            swordPlayer.message("You are already queued.");
            return;
        }

        currentPlayerQueue.add(swordPlayer);
        swordPlayer.message("Joined the queue.");

        tryStartNextMatch();
    }

    /**
     * Attempts to start the next match if the arena is free and at least two players are queued.
     * Arena integration is pending — this method is currently a no-op.
     */
    public static void tryStartNextMatch() {
        // TODO: integrate ArenaManager once arena lifecycle is implemented
    }
}
