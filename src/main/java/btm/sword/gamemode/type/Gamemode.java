package btm.sword.gamemode.type;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;

import btm.sword.system.control.PredicateRunnablePair;
import btm.sword.system.control.TimeArbiter;
import btm.sword.system.entity.types.SwordPlayer;

public abstract class Gamemode {

    protected final List<SwordPlayer> players;

    protected AtomicInteger durationSeconds = new AtomicInteger(180); // default
    private TimeArbiter.TaskHandle timerTask;

    private final BossBar bossBar;

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

    public void start() {
        onStart();
        startTimer();
    }

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
            20,
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
