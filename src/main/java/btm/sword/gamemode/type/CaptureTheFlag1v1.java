package btm.sword.gamemode.type;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.bukkit.Location;

import btm.sword.config.Config;
import btm.sword.control.SwordScheduler;
import btm.sword.gamemode.ctf.CtfTeam;
import btm.sword.gamemode.ctf.FlagEntity;
import btm.sword.system.entity.impl.SwordPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;

/**
 * A 1v1 Capture the Flag game mode.
 *
 * <h3>Rules</h3>
 * <ul>
 *   <li>Each team has a flag at their base. Carry the enemy flag back to your base to score.</li>
 *   <li>First team to {@link Config.Ctf#CAPTURES_TO_WIN} captures wins. If {@code CAPTURES_TO_WIN}
 *       is 0, the match runs for its full duration and the team with more captures at expiry wins.</li>
 *   <li>On death: the flag is dropped at the death location and a
 *       {@link Config.Ctf#RESPAWN_DELAY_SECONDS}-second respawn timer fires before the player
 *       is returned to their team spawn.</li>
 * </ul>
 *
 * <h3>Capture detection</h3>
 * <p>Checked each tick: a carrier of the <em>enemy</em> flag standing within
 * {@link Config.Ctf#CAPTURE_RADIUS} blocks of their <em>own</em> team's spawn location scores.</p>
 *
 * <h3>Solo / debug mode</h3>
 * <p>Supports a single player (RED team only) for testing flag mechanics without a second player.</p>
 */
public class CaptureTheFlag1v1 extends Gamemode {

    private final Map<UUID, CtfTeam> playerTeams = new HashMap<>();
    private final Map<CtfTeam, Integer> scores = new HashMap<>();
    private final FlagEntity redFlag = new FlagEntity(CtfTeam.RED);
    private final FlagEntity blueFlag = new FlagEntity(CtfTeam.BLUE);

    private static final int MATCH_DURATION_SECONDS = 180;

    /**
     * Creates a new CTF match. Accepts 1 or 2 players.
     * With 1 player they are assigned to RED; BLUE has no player (solo/debug mode).
     *
     * @param players 1 or 2 players participating in the match
     */
    public CaptureTheFlag1v1(List<SwordPlayer> players) {
        super(players);
        this.durationSeconds = new java.util.concurrent.atomic.AtomicInteger(MATCH_DURATION_SECONDS);
        scores.put(CtfTeam.RED, 0);
        scores.put(CtfTeam.BLUE, 0);

        playerTeams.put(players.get(0).player().getUniqueId(), CtfTeam.RED);
        if (players.size() >= 2) {
            playerTeams.put(players.get(1).player().getUniqueId(), CtfTeam.BLUE);
        }
    }

    @Override
    protected void onStart() {
        for (SwordPlayer sp : players) {
            CtfTeam team = getTeam(sp);
            sp.player().teleport(team.getSpawnLocation());
            sp.message(Component.text("Match started! Carry the enemy flag to your base.",
                NamedTextColor.YELLOW));
        }

        redFlag.spawnIdle();
        blueFlag.spawnIdle();
    }

    @Override
    protected void onStop() {
        redFlag.cleanup();
        blueFlag.cleanup();

        int redScore = scores.get(CtfTeam.RED);
        int blueScore = scores.get(CtfTeam.BLUE);

        CtfTeam winner;
        if (redScore > blueScore) winner = CtfTeam.RED;
        else if (blueScore > redScore) winner = CtfTeam.BLUE;
        else winner = null;

        String resultText = winner != null
            ? winner.name() + " WINS! (" + scores.get(winner) + " captures)"
            : "DRAW! (" + redScore + " - " + blueScore + ")";

        NamedTextColor resultColor = winner != null ? winner.getTextColor() : NamedTextColor.YELLOW;

        for (SwordPlayer sp : players) {
            sp.player().showTitle(Title.title(
                Component.text(resultText, resultColor, TextDecoration.BOLD),
                Component.text("Match over", NamedTextColor.GRAY)
            ));
        }
    }

    @Override
    protected void onTick(int secondsLeft) {
        checkFlagPickups();
        checkCaptures();
    }

    @Override
    protected int getMaxDuration() {
        return MATCH_DURATION_SECONDS;
    }

    @Override
    protected String getTitle() {
        if (scores == null) return "Capture The Flag 1v1";
        int r = scores.get(CtfTeam.RED);
        int b = scores.get(CtfTeam.BLUE);
        return "† CTF †  - RED " + r + " — " + b + " BLUE -  † CTF †";
    }

    /**
     * Called by {@link btm.sword.gamemode.QueueManager} (via {@link btm.sword.listeners.PlayerListener})
     * when a participant dies during the match.
     * <p>
     * Drops the flag if the dead player was carrying one, then schedules a delayed respawn
     * at their team spawn.
     * </p>
     *
     * @param dead the player who died
     */
    public void onPlayerDeath(SwordPlayer dead) {
        Location deathLoc = dead.player().getLocation();

        if (redFlag.isCarriedBy(dead)) redFlag.drop(deathLoc);
        if (blueFlag.isCarriedBy(dead)) blueFlag.drop(deathLoc);

        int delaySeconds = Config.Ctf.RESPAWN_DELAY_SECONDS;
        SwordScheduler.runBukkitTaskLater(() -> {
            dead.player().spigot().respawn();
            SwordScheduler.runBukkitTask(() -> {
                CtfTeam team = getTeam(dead);
                dead.player().teleport(team.getSpawnLocation());
                dead.message(Component.text("Respawned at " + team.name() + " base.", team.getTextColor()));
            });
        }, delaySeconds, TimeUnit.SECONDS);
    }

    /**
     * Returns the {@link CtfTeam} assigned to the given player. Defaults to RED for unknown players.
     *
     * @param player the player to look up
     * @return the player's team
     */
    public CtfTeam getTeam(SwordPlayer player) {
        return playerTeams.getOrDefault(player.player().getUniqueId(), CtfTeam.RED);
    }

    // --- private tick logic ---

    private void checkFlagPickups() {
        for (SwordPlayer sp : players) {
            CtfTeam team = getTeam(sp);
            FlagEntity enemyFlag = getFlagFor(team.enemy());

            if (enemyFlag.getState() == FlagEntity.State.IDLE
                    && sp.player().getLocation().distance(team.enemy().getSpawnLocation())
                        <= Config.Ctf.CAPTURE_RADIUS) {
                enemyFlag.pickup(sp);
            }
        }
    }

    private void checkCaptures() {
        for (SwordPlayer sp : players) {
            CtfTeam team = getTeam(sp);
            FlagEntity enemyFlag = getFlagFor(team.enemy());

            if (enemyFlag.isCarriedBy(sp)) {
                Location ownBase = team.getSpawnLocation();
                if (sp.player().getLocation().distance(ownBase) <= Config.Ctf.CAPTURE_RADIUS) {
                    score(team, sp, enemyFlag);
                }
            }
        }
    }

    private void score(CtfTeam team, SwordPlayer scorer, FlagEntity capturedFlag) {
        scores.merge(team, 1, Integer::sum);
        int newScore = scores.get(team);

        capturedFlag.returnToBase();

        Component captureMsg = Component.text(scorer.player().getName() + " captured the flag! ", team.getTextColor())
            .append(Component.text("(" + team.name() + ": " + newScore + ")", NamedTextColor.YELLOW));

        for (SwordPlayer sp : players) {
            sp.message(captureMsg);
        }

        int threshold = Config.Ctf.CAPTURES_TO_WIN;
        if (threshold > 0 && newScore >= threshold) {
            stop();
        }
    }

    private FlagEntity getFlagFor(CtfTeam team) {
        return team == CtfTeam.RED ? redFlag : blueFlag;
    }
}
