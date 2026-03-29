package btm.sword.gamemode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import btm.sword.gamemode.type.CaptureTheFlag1v1;
import btm.sword.gamemode.type.Gamemode;
import btm.sword.gamemode.type.RoguelikeRun;
import btm.sword.system.entity.impl.SwordPlayer;

/**
 * Manages per-{@link Gamemode} matchmaking queues and tracks active matches.
 * <p>
 * Players are enqueued via {@link #enqueue} and matched when the queue has enough players.
 * Roguelike runs start solo (1 player); CTF requires 2.
 * </p>
 *
 * <p>Active CTF matches are registered in {@link #activeMatches} so that
 * {@link btm.sword.listeners.PlayerListener} can route death events to the correct match.</p>
 */
public class QueueManager {

    private static final Map<Class<? extends Gamemode>, Queue<SwordPlayer>> queueMap;

    /**
     * Maps each participating player's UUID to their active CTF match.
     * Populated on match start, cleaned up on match stop.
     */
    private static final Map<UUID, CaptureTheFlag1v1> activeMatches = new HashMap<>();

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
     * Immediately launches a CTF match for the given player without going through the queue.
     * Intended for developer testing and single-player debug sessions.
     *
     * @param swordPlayer the player to start the match for
     */
    public static void startCtfDebug(SwordPlayer swordPlayer) {
        startCtfMatch(List.of(swordPlayer));
    }

    /**
     * Attempts to start any matches where the queue meets the player threshold.
     * <p>
     * Roguelike runs start immediately with 1 player. CTF requires 2.
     * </p>
     */
    public static void tryStartNextMatch() {
        Queue<SwordPlayer> roguelikeQueue = queueMap.get(RoguelikeRun.class);
        if (roguelikeQueue != null && !roguelikeQueue.isEmpty()) {
            SwordPlayer player = roguelikeQueue.poll();
            new RoguelikeRun(List.of(player)).start();
        }

        Queue<SwordPlayer> ctfQueue = queueMap.get(CaptureTheFlag1v1.class);
        if (ctfQueue != null && ctfQueue.size() >= 2) {
            SwordPlayer p1 = ctfQueue.poll();
            SwordPlayer p2 = ctfQueue.poll();
            startCtfMatch(List.of(p1, p2));
        }
    }

    /**
     * Returns the active CTF match for the given player UUID, or {@code null} if they are not in one.
     *
     * @param uuid the player's UUID
     * @return the active {@link CaptureTheFlag1v1}, or {@code null}
     */
    public static CaptureTheFlag1v1 getActiveCtfMatch(UUID uuid) {
        return activeMatches.get(uuid);
    }

    /**
     * Removes the given player from all active match registrations.
     * Called automatically when a match ends.
     *
     * @param uuid the UUID to deregister
     */
    public static void deregisterFromMatch(UUID uuid) {
        activeMatches.remove(uuid);
    }

    private static void startCtfMatch(List<SwordPlayer> players) {
        CaptureTheFlag1v1 match = new CaptureTheFlag1v1(players);
        for (SwordPlayer sp : players) {
            activeMatches.put(sp.player().getUniqueId(), match);
        }
        match.start();
    }
}
