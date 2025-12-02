package btm.sword.gamemode;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

import btm.sword.gamemode.type.Gamemode;
import btm.sword.system.entity.types.SwordPlayer;

public class QueueManager {
    private static final Map<Class<? extends Gamemode>, Queue<SwordPlayer>> queueMap = new HashMap<>();

    public static void enqueue(Class<? extends Gamemode> gamemode, SwordPlayer swordPlayer) {
        Queue<SwordPlayer> currentPlayerQueue = queueMap.get(gamemode);
        if (currentPlayerQueue.contains(swordPlayer)) {
//            p.sendMessage("You are already queued.");
            return;
        }

        currentPlayerQueue.add(swordPlayer);
//        p.sendMessage("Joined the queue.");

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
