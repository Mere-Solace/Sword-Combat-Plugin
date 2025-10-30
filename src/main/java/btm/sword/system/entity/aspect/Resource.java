package btm.sword.system.entity.aspect;

import btm.sword.Sword;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

/**
 * Represents a regenerating resource such as health, mana, or stamina.
 * <p>
 * Resources extend {@link Aspect} by adding a current value, maximum value,
 * and automatic regeneration over time. The regeneration rate and amount
 * can be modified through effectiveness percentages.
 * </p>
 * <p>
 * The resource automatically regenerates using a {@link BukkitTask} that runs
 * periodically, adding the effective regeneration amount until the resource
 * reaches its maximum effective value.
 * </p>
 *
 * @see Aspect
 * @see AspectType
 */
@Getter
@Setter
public class Resource extends Aspect {
    /** The current value of the resource. */
    private float curValue;

    /** The base regeneration period in ticks. */
    private int baseRegenPeriod;

    /** The base amount regenerated per period. */
    private float baseRegenAmount;

    /** The effectiveness modifier for the regeneration period. */
    private float effPeriodPercent;

    /** The effectiveness modifier for the regeneration amount. */
    private float effAmountPercent;

    /** The task handling automatic regeneration. */
    private BukkitTask regenTask;

    /**
     * Constructs a new Resource with the specified parameters.
     *
     * @param type the aspect type
     * @param maxValue the maximum value of the resource
     * @param baseRegenPeriod the base regeneration period in ticks
     * @param baseRegenAmount the base amount regenerated per period
     */
    public Resource(AspectType type, float maxValue, int baseRegenPeriod, float baseRegenAmount) {
        super(type, maxValue);
        this.baseRegenPeriod = baseRegenPeriod;
        this.baseRegenAmount = baseRegenAmount;
        curValue = baseValue;
    }

    /**
     * Starts the automatic regeneration task.
     * <p>
     * The task runs periodically (based on {@link #effectivePeriod()}) and adds
     * the effective regeneration amount until the current value reaches the maximum.
     * </p>
     */
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

    /**
     * Stops the automatic regeneration task.
     */
    public void stopRegenTask() {
        if (regenTask != null && !regenTask.isCancelled() && regenTask.getTaskId() != -1)
            regenTask.cancel();
    }

    /**
     * Restarts the automatic regeneration task.
     * <p>
     * This is useful when regeneration parameters have changed and the task
     * needs to be rescheduled with the new values.
     * </p>
     */
    public void restartRegenTask() {
        stopRegenTask();
        startRegenTask();
    }

    /**
     * Returns the current value of the resource.
     *
     * @return the current value
     */
    public float cur() {
        return curValue;
    }

    /**
     * Returns the current value as a percentage of the maximum.
     *
     * @return the current percentage (0.0 to 1.0)
     */
    public float curPercent() {
        return curValue/effectiveValue();
    }

    /**
     * Sets the current value, capped at the maximum effective value.
     *
     * @param value the new current value
     */
    public void setCur(float value) {
        curValue = Math.min(effectiveValue(), value);
    }

    /**
     * Sets the current value as a percentage of the maximum.
     *
     * @param percent the percentage (0.0 to 1.0)
     */
    public void setCurPercent(float percent) {
        curValue = percent * effectiveValue();
    }

    /**
     * Adds the specified amount to the current value.
     *
     * @param amount the amount to add
     */
    public void add(float amount) {
        curValue += amount;
    }

    /**
     * Removes the specified amount from the current value.
     *
     * @param amount the amount to remove
     * @return true if the value went to zero or below, false otherwise
     */
    public boolean remove(float amount) {
        curValue -= amount;
        if (curValue <= 0) {
            curValue = 0;
            return true;
        }
        return false;
    }

    /**
     * Resets the current value to the maximum effective value.
     */
    public void reset() {
        curValue = effectiveValue();
    }

    /**
     * Sets the base maximum value and adjusts the current value if it exceeds the new maximum.
     *
     * @param baseValue the new base maximum value
     */
    @Override
    public void setBaseValue(float baseValue) {
        this.baseValue = baseValue;
        curValue = Math.min(curValue, effectiveValue());
    }

    /**
     * Calculates the effective regeneration period in ticks.
     *
     * @return the effective period
     */
    public long effectivePeriod() {
        return (long)(baseRegenPeriod * effPeriodPercent);
    }

    /**
     * Calculates the effective regeneration amount per period.
     *
     * @return the effective regeneration amount
     */
    public float effectiveAmount() {
        return baseRegenAmount * effAmountPercent;
    }

    /**
     * Sets the effectiveness percentage for the regeneration period.
     * <p>
     * This restarts the regeneration task to apply the new period.
     * </p>
     *
     * @param effPeriodPercent the new period effectiveness percentage
     */
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
