package btm.sword.gamemode.type;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.bukkit.entity.Player;

import btm.sword.system.entity.impl.SwordPlayer;

/**
 * A 1v1 Capture-the-Flag game mode.
 * <p>
 * Two players are teleported to opposing spawn offsets at match start. The player with the
 * higher {@link #captureFlag} count when the 3-minute timer expires wins. Flag captures must
 * be driven externally by calling {@link #captureFlag(SwordPlayer)}.
 * </p>
 *
 * <p>Title display integration is pending — see inline TODOs.</p>
 */
public class CaptureTheFlag1v1 extends Gamemode {

    private int p1Score = 0;
    private int p2Score = 0;

    /**
     * Creates a new 1v1 CTF match for the two given players with a 3-minute timer.
     *
     * @param players exactly two players participating in the match
     */
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
        // TODO: display result title to both players
    }

    @Override
    protected void onTick(int secondsLeft) {
        // Optional: update scoreboard, etc.
    }

    @Override
    protected String getTitle() {
        return "Capture The Flag 1v1";
    }

    /**
     * Records a flag capture for the given player, incrementing their score.
     *
     * @param p the player who captured the flag
     */
    public void captureFlag(SwordPlayer p) {
        if (p.equals(players.getFirst())) p1Score++;
        else p2Score++;
    }
}
