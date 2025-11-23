package btm.sword.gamemode.type;

import btm.sword.gamemode.Gamemode;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class CaptureTheFlag1v1 extends Gamemode {

    private int p1Score = 0;
    private int p2Score = 0;

    public CaptureTheFlag1v1(JavaPlugin plugin, List<Player> players) {
        super(plugin, players);
        this.durationSeconds = 180; // 3 minutes
    }

    @Override
    protected void onStart() {
        Player p1 = players.get(0);
        Player p2 = players.get(1);

        p1.teleport(p1.getWorld().getSpawnLocation().add(10, 0, 0));
        p2.teleport(p2.getWorld().getSpawnLocation().add(-10, 0, 0));

        p1.sendMessage(ChatColor.GREEN + "CTF Match Started!");
        p2.sendMessage(ChatColor.GREEN + "CTF Match Started!");
    }

    @Override
    protected void onStop() {
        Player p1 = players.get(0);
        Player p2 = players.get(1);

        String result;
        if (p1Score > p2Score) result = p1.getName() + " wins!";
        else if (p2Score > p1Score) result = p2.getName() + " wins!";
        else result = "Tie!";

        p1.sendMessage(ChatColor.GOLD + "Match Over: " + result);
        p2.sendMessage(ChatColor.GOLD + "Match Over: " + result);
    }

    @Override
    protected void onTick(int secondsLeft) {
        // Optional: update scoreboard, etc.
    }

    @Override
    protected String getTitle() {
        return ChatColor.RED + "Capture The Flag 1v1";
    }

    public void captureFlag(Player p) {
        if (p.equals(players.get(0))) p1Score++;
        else p2Score++;

        p.sendMessage(ChatColor.AQUA + "Flag Captured!");
    }
}

