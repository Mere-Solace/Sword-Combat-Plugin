package btm.sword.gamemode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import btm.sword.gamemode.type.CaptureTheFlag1v1;
import btm.sword.gamemode.type.Gamemode;
import btm.sword.gamemode.type.RoguelikeRun;
import btm.sword.system.entity.impl.SwordPlayer;

/**
 * Manages per-{@link Gamemode} matchmaking queues.
 * <p>
 * Players are enqueued via {@link #enqueue} and matched when the queue has
 * enough players. Roguelike runs start solo (1 player); CTF requires 2.
 * </p>
 */
public class QueueManager {

    private static final Map<Class<? extends Gamemode>, Queue<SwordPlayer>> queueMap;

    static {
        queueMap = new HashMap<>();
        queueMap.put(CaptureTheFlag1v1.class, new ConcurrentLinkedQueue<>());
        queueMap.put(RoguelikeRun.class, new ConcurrentLinkedQueue<>());
    }

    /**
     * Adds the given player to the queue for the specified game mode.
     * No-ops (with a message) if the player is already queued.
     *
     * @param gamemode    the game mode class to queue for
     * @param swordPlayer the player requesting to join the queue
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
     * Attempts to start any matches where the queue meets the player threshold.
     * <p>
     * Roguelike runs start immediately with 1 player. CTF arena integration is
     * pending — see inline TODO.
     * </p>
     */
    public static void tryStartNextMatch() {
        Queue<SwordPlayer> roguelikeQueue = queueMap.get(RoguelikeRun.class);
        if (roguelikeQueue != null && !roguelikeQueue.isEmpty()) {
            SwordPlayer player = roguelikeQueue.poll();
            new RoguelikeRun(List.of(player)).start();
        }

        // TODO: integrate ArenaManager once arena lifecycle is implemented
        // Queue<SwordPlayer> ctfQueue = queueMap.get(CaptureTheFlag1v1.class);
        // if (ctfQueue != null && ctfQueue.size() >= 2) { ... }
    }
}
