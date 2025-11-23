package btm.sword.gamemode;

import org.bukkit.Bukkit;
import org.bukkit.boss.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;

public abstract class Gamemode {

    protected final JavaPlugin plugin;
    protected final List<Player> players;

    protected int durationSeconds = 180; // default
    private BukkitTask timerTask;

    private final BossBar bossBar;

    public Gamemode(JavaPlugin plugin, List<Player> players) {
        this.plugin = plugin;
        this.players = players;

        bossBar = Bukkit.createBossBar(
            getTitle(),
            BarColor.RED,
            BarStyle.SEGMENTED_20
        );

        for (Player p : players) {
            bossBar.addPlayer(p);
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
        timerTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            durationSeconds--;

            bossBar.setProgress(Math.max(0, (double) durationSeconds / getMaxDuration()));
            bossBar.setTitle(getTitle() + " — " + durationSeconds + "s");

            onTick(durationSeconds);

            if (durationSeconds <= 0) {
                stop();
            }
        }, 20L, 20L);
    }

    protected int getMaxDuration() {
        return 180;
    }

    protected abstract void onStart();
    protected abstract void onStop();
    protected abstract void onTick(int secondsLeft);
    protected abstract String getTitle();
}

