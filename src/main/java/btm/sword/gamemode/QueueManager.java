package btm.sword.gamemode;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import btm.sword.gamemode.type.CaptureTheFlag1v1;
import btm.sword.gamemode.type.Gamemode;
import btm.sword.system.entity.types.SwordPlayer;

public class QueueManager {
    private static final Map<Class<? extends Gamemode>, Queue<SwordPlayer>> queueMap;

    static {
        queueMap = new HashMap<>();
        queueMap.put(CaptureTheFlag1v1.class, new ConcurrentLinkedQueue<>());
    }

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

    public static void tryStartNextMatch() {
//        if (arena.isBusy()) return;
//        if (queue.size() < 2) return;
//
//        SwordPlayer p1 = queue.poll();
//        SwordPlayer p2 = queue.peek();
//
//        if (p1 == null) return;
//
//        arena.startGame(List.of(p1, p2));
    }
}
