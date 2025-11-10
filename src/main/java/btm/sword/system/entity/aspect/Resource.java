package btm.sword.system.entity.aspect;

import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import btm.sword.Sword;
import lombok.Getter;
import lombok.Setter;

/**
 * Represents an {@link Aspect} that behaves as a regenerating resource.
 * <p>
 * A {@code Resource} has a current value that automatically regenerates over time
 * according to its base regeneration period and amount. The regeneration process
 * is managed by a repeating Bukkit task that periodically restores the resource,
 * up to its effective maximum.
 * </p>
 *
 * <p>
 * The regeneration rate and amount can be dynamically modified using
 * percentage-based multipliers ({@code effPeriodPercent}, {@code effAmountPercent}),
 * allowing for buffs, debuffs, or temporary effects to affect recovery behavior.
 * </p>
 */
@Getter
@Setter
public class Resource extends Aspect {
    /** The current value of the resource. */
    private float curValue;

    /** The base period (in ticks) between regeneration events. */
    private int baseRegenPeriod;

    /** The base amount of resource to regenerate per period. */
    private float baseRegenAmount;

    /** The effective period as a percentage multiplier of base (default 1, lower is faster). */
    private float effPeriodPercent;

    /** The effective amount as a percentage multiplier of base (default 1). */
    private float effAmountPercent;

    /** The scheduled Bukkit task responsible for ticking resource regeneration. */
    private BukkitTask regenTask;

    /**
     * Creates a new regenerating resource aspect.
     * @param type the resource type
     * @param maxValue the maximum value of the resource
     * @param baseRegenPeriod base period between regeneration attempts (ticks)
     * @param baseRegenAmount base resource regained per period
     */
    public Resource(AspectType type, float maxValue, int baseRegenPeriod, float baseRegenAmount) {
        super(type, maxValue);
        this.baseRegenPeriod = baseRegenPeriod;
        this.baseRegenAmount = baseRegenAmount;
        curValue = baseValue;

        effPeriodPercent = 1.0f;
        effAmountPercent = 1.0f;
    }

    /**
     * Starts a scheduled task that automatically regenerates the resource over time.
     * <p>
     * If the resource is already full, it will wait until the next cycle before checking again.
     * The task runs indefinitely until manually stopped via {@link #stopRegenTask()}.
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
     * Stops the ongoing regeneration task, if one is active.
     * <p>
     * Safe to call even if no task is running.
     * </p>
     */
    public void stopRegenTask() {
        if (regenTask != null && !regenTask.isCancelled() && regenTask.getTaskId() != -1)
            regenTask.cancel();
    }

    /**
     * Restarts the regeneration task, reapplying any modified regeneration parameters.
     * <p>
     * Useful after changing period or amount multipliers.
     * </p>
     */
    public void restartRegenTask() {
        stopRegenTask();
        startRegenTask();
    }

    /**
     * Gets the current value of the resource.
     * @return the current resource value
     */
    public float cur() {
        return curValue;
    }

    /**
     * Gets the current value as a percentage [0, 1] of the effective maximum.
     * @return current resource / effective cap
     */
    public float curPercent() {
        return curValue/effectiveValue();
    }

    /**
     * Sets the current resource value; capped by the effective maximum.
     * @param value new value to set
     */
    public void setCur(float value) {
        curValue = Math.min(effectiveValue(), value);
    }

    /**
     * Sets the current resource value as a percentage of the effective maximum.
     * @param percent fraction of maximum [0,1]
     */
    public void setCurPercent(float percent) {
        curValue = percent * effectiveValue();
    }

    /**
     * Adds the specified amount to the resource.
     * @param amount the value to add
     */
    public void add(float amount) {
        curValue += amount;
    }

    /**
     * Subtracts the specified amount. Returns true if resource is depleted.
     * @param amount value to remove
     * @return true if resource fell to or below zero, false otherwise
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
     * Resets the resource to its maximum effective value.
     */
    public void reset() {
        curValue = effectiveValue();
    }

    /**
     * Sets the base value, ensuring current does not exceed new cap.
     * @param baseValue the new base value
     */
    @Override
    public void setBaseValue(float baseValue) {
        this.baseValue = baseValue;
        curValue = Math.min(curValue, effectiveValue());
    }

    /**
     * Gets the effective regeneration period (in ticks), factoring in modifiers.
     * @return effective period (ticks)
     */
    public long effectivePeriod() {
        return (long)(baseRegenPeriod * effPeriodPercent);
    }

    /**
     * Gets the effective amount of resource regenerated per period.
     * @return effective regeneration amount
     */
    public float effectiveAmount() {
        return baseRegenAmount * effAmountPercent;
    }

    /**
     * Sets the period modifier and restarts task for updated period.
     * @param effPeriodPercent new period multiplier
     */
    public void setEffPeriodPercent(float effPeriodPercent) {
        this.effPeriodPercent = effPeriodPercent;
        restartRegenTask();
    }

    /**
     * Adds to the period percent modifier, then restarts task.
     * @param percent value to add
     */
    public void addEffPeriodPercent(float percent) {
        effPeriodPercent += percent;
        restartRegenTask();
    }

    /**
     * Subtracts from the period percent modifier, then restarts task.
     * @param percent value to subtract
     */
    public void subEffPeriodPercent(float percent) {
        effPeriodPercent -= percent;
        restartRegenTask();
    }

    /**
     * Adds to the amount percent modifier.
     * @param percent value to add
     */
    public void addEffAmountPercent(float percent) {
        effAmountPercent += percent;
    }

    /**
     * Subtracts from the amount percent modifier.
     * @param percent value to subtract
     */
    public void subEffAmountPercent(float percent) {
        effAmountPercent -= percent;
    }
}
