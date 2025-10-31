package btm.sword.system.entity.aspect;

import btm.sword.Sword;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

@Getter
@Setter
public class Resource extends Aspect {
    private float curValue;
    private int baseRegenPeriod;
    private float baseRegenAmount;
    private float effPeriodPercent;
    private float effAmountPercent;

    private BukkitTask regenTask;

    public Resource(AspectType type, float maxValue, int baseRegenPeriod, float baseRegenAmount) {
        super(type, maxValue);
        this.baseRegenPeriod = baseRegenPeriod;
        this.baseRegenAmount = baseRegenAmount;
        curValue = baseValue;
    }

    public void startRegenTask() {
        regenTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (curValue < effectiveValue()) {
                    add(effectiveAmount());
                }
            }
        }.runTaskTimer(Sword.getInstance(), 0L, effectivePeriod());
    }

    public void stopRegenTask() {
        if (regenTask != null && !regenTask.isCancelled() && regenTask.getTaskId() != -1)
            regenTask.cancel();
    }

    public void restartRegenTask() {
        stopRegenTask();
        startRegenTask();
    }

    public float cur() {
        return curValue;
    }

    public float curPercent() {
        return curValue/effectiveValue();
    }

    public void setCur(float value) {
        curValue = Math.min(effectiveValue(), value);
    }

    public void setCurPercent(float percent) {
        curValue = percent * effectiveValue();
    }

    public void add(float amount) {
        curValue += amount;
    }

    // true if value goes below 0, false otherwise
    public boolean remove(float amount) {
        curValue -= amount;
        if (curValue <= 0) {
            curValue = 0;
            return true;
        }
        return false;
    }

    public void reset() {
        curValue = effectiveValue();
    }

    @Override
    public void setBaseValue(float baseValue) {
        this.baseValue = baseValue;
        curValue = Math.min(curValue, effectiveValue());
    }

    public long effectivePeriod() {
        return (long)(baseRegenPeriod * effPeriodPercent);
    }

    public float effectiveAmount() {
        return baseRegenAmount * effAmountPercent;
    }

    public void setEffPeriodPercent(float effPeriodPercent) {
        this.effPeriodPercent = effPeriodPercent;
        restartRegenTask();
    }

    public void addEffPeriodPercent(float percent) {
        effPeriodPercent += percent;
        restartRegenTask();
    }

    public void subEffPeriodPercent(float percent) {
        effPeriodPercent -= percent;
        restartRegenTask();
    }

    public void addEffAmountPercent(float percent) {
        effAmountPercent += percent;
    }

    public void subEffAmountPercent(float percent) {
        effAmountPercent -= percent;
    }
}
