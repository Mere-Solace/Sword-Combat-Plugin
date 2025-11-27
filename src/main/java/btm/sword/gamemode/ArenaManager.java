package btm.sword.gamemode;

import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import btm.sword.gamemode.type.CaptureTheFlag1v1;

public class ArenaManager {

    private final JavaPlugin plugin;
    private Gamemode currentGame = null;

    public ArenaManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isBusy() {
        return currentGame != null;
    }

    public void startGame(List<Player> players) {
        if (isBusy()) return;

        currentGame = new CaptureTheFlag1v1(plugin, players);
        currentGame.start();
    }

    public void endGame() {
        if (currentGame != null) {
            currentGame.stop();
            currentGame = null;
        }
    }
}
