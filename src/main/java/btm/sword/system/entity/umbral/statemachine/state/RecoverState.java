package btm.sword.system.entity.umbral.statemachine.state;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import btm.sword.Sword;
import btm.sword.system.entity.umbral.UmbralBlade;
import btm.sword.system.entity.umbral.statemachine.UmbralStateFacade;

public class RecoverState extends UmbralStateFacade {
    private boolean recoveringBlade;
    private UmbralBlade blade;
    private final Runnable recoverBlade = new BukkitRunnable() {
        @Override
        public void run() {
            try {
                if (blade.getDisplay() != null) blade.getDisplay().remove();
                blade.setDisplay(null);
                blade.resetWeaponDisplay();
                recoveringBlade = false;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    };
    private BukkitTask recoverTask;

    @Override
    public String name() {
        return "RECOVERY";
    }

    @Override
    public void onEnter(UmbralBlade blade) {
        this.blade = blade;
        recoveringBlade = true;
        recoverTask = Bukkit.getScheduler().runTaskTimer(
            Sword.getInstance(), recoverBlade, 0, 4L);
    }

    @Override
    public void onExit(UmbralBlade blade) {
        if (recoverTask.getTaskId() != -1 && recoverTask != null && !recoverTask.isCancelled()) recoverTask.cancel();
    }

    @Override
    public void onTick(UmbralBlade blade) {

    }
}
