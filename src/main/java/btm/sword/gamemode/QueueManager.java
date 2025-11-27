package btm.sword.gamemode;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.bukkit.entity.Player;

public class QueueManager {

    private final ArenaManager arena;
    private final Queue<Player> queue = new LinkedList<>();

    public QueueManager(ArenaManager arena) {
        this.arena = arena;
    }

    public void enqueue(Player p) {
        if (queue.contains(p)) {
            p.sendMessage("You are already queued.");
            return;
        }

        queue.add(p);
        p.sendMessage("Joined the queue.");

        tryStartNextMatch();
    }

    public void tryStartNextMatch() {
        if (arena.isBusy()) return;
        if (queue.size() < 2) return;

        Player p1 = queue.poll();
        Player p2 = queue.poll();

        arena.startGame(List.of(p1, p2));
    }
}
