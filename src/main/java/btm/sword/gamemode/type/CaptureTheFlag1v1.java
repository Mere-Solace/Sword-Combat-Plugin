package btm.sword.gamemode.type;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.bukkit.entity.Player;

import btm.sword.system.entity.types.SwordPlayer;

public class CaptureTheFlag1v1 extends Gamemode {

    private int p1Score = 0;
    private int p2Score = 0;

    public CaptureTheFlag1v1(List<SwordPlayer> players) {
        super(players);
        this.durationSeconds = new AtomicInteger(180); // 3 minutes
    }

    @Override
    protected void onStart() {
        SwordPlayer sp1 = players.getFirst();
        Player p1 = sp1.player();
        SwordPlayer sp2 = players.getLast();
        Player p2 = sp2.player();

        p1.teleport(p1.getWorld().getSpawnLocation().add(10, 0, 0));
        p2.teleport(p2.getWorld().getSpawnLocation().add(-10, 0, 0));

//        sp1.displayTitle();
    }

    @Override
    protected void onStop() {
        SwordPlayer sp1 = players.getFirst();
        Player p1 = sp1.player();
        SwordPlayer sp2 = players.getLast();
        Player p2 = sp2.player();


        String result;
        if (p1Score > p2Score) result = p1.getName() + " wins!";
        else if (p2Score > p1Score) result = p2.getName() + " wins!";
        else result = "Tie!";

//        sp1.displayTitle();
    }

    @Override
    protected void onTick(int secondsLeft) {
        // Optional: update scoreboard, etc.
    }

    @Override
    protected String getTitle() {
        return null; // ChatColor.RED + "Capture The Flag 1v1";
    }

    public void captureFlag(SwordPlayer p) {
        if (p.equals(players.getFirst())) p1Score++;
        else p2Score++;

//        sp1.displayTitle();
    }
}
