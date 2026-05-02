package btm.sword.gamemode.type;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;

import btm.sword.entity.player.SwordPlayer;
import btm.sword.runtime.scheduler.PredicateRunnablePair;
import btm.sword.runtime.scheduler.TimeArbiter;

/**
 * Abstract base for all Sword game modes.
 * <p>
 * Manages a boss-bar countdown timer and delegates lifecycle events ({@link #onStart},
 * {@link #onTick}, {@link #onStop}) to concrete subclasses. Call {@link #start()} to
 * begin the match and {@link #stop()} to end it (also called automatically when the timer
 * expires).
 * </p>
 */
public abstract class Gamemode {

    /** Players participating in this match. */
    protected final List<SwordPlayer> players;

    /** Remaining duration in seconds. Decremented once per second by the internal timer. */
    protected AtomicInteger durationSeconds = new AtomicInteger(180); // default
    private TimeArbiter.TaskHandle timerTask;

    private final BossBar bossBar;

    /**
     * Creates a game mode instance for the given players and initialises the boss bar.
     *
     * @param players the players participating in this match
     */
    public Gamemode(List<SwordPlayer> players) {
        this.players = players;

        bossBar = Bukkit.createBossBar(
            getTitle(),
            BarColor.RED,
            BarStyle.SEGMENTED_20
        );

        for (SwordPlayer p : players) {
            bossBar.addPlayer(p.player());
        }
    }

    /** Starts the match: invokes {@link #onStart()} and begins the countdown timer. */
    public void start() {
        onStart();
        startTimer();
    }

    /** Ends the match: cancels the timer, removes the boss bar, and invokes {@link #onStop()}. */
    public void stop() {
        if (timerTask != null && !timerTask.isCancelled()) {
            timerTask.cancel();
        }

        bossBar.removeAll();
        onStop();
    }

    private void startTimer() {
        timerTask = TimeArbiter.runTimeBoundBukkitTaskOnTimer(
            () -> {},
            () -> {
                durationSeconds.decrementAndGet();

                bossBar.setProgress(Math.max(0, (double) durationSeconds.get() / getMaxDuration()));
                bossBar.setTitle(getTitle() + " - " + durationSeconds + "s");

                onTick(durationSeconds.get());
            },
            () -> {},
            0,
            1000,
            Gamemode.class, "startTimer",
            new PredicateRunnablePair(
                () -> durationSeconds.get() <= 0,
                this::stop
            ));
    }

    protected int getMaxDuration() {
        return durationSeconds.get();
    }

    protected abstract void onStart();
    protected abstract void onStop();
    protected abstract void onTick(int secondsLeft);
    protected abstract String getTitle();
}
