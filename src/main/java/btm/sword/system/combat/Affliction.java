package btm.sword.system.combat;

import org.bukkit.scheduler.BukkitRunnable;

import btm.sword.Sword;
import btm.sword.system.entity.base.SwordEntity;

//STUNNED,        // Duration
//GROUNDED,       // Duration, Strength
//BLEEDING,       // Duration, Strength
//SLOWNESS,       // Duration, Strength
//ARMOR_BREAK     // Duration

/**
 * Represents a timed status effect or debuff applied to a {@link SwordEntity}.
 * <p>
 * Examples of afflictions include STUNNED, GROUNDED, BLEEDING, SLOWNESS, and ARMOR_BREAK.
 * Each affliction tracks its duration in ticks and may have an associated strength value.
 * Afflictions may be reapplied or extended depending on their configuration.
 */
public abstract class Affliction {
    private final boolean reapply;
    protected long tickDuration;
    protected double strength;

    protected int[] curTicks;
    private boolean shouldCancel;
    private boolean shouldReapply;

    /**
     * Constructs an affliction with a specified duration.
     *
     * @param reapply      Whether the affliction should reapply each tick.
     * @param tickDuration Total duration in ticks.
     */
    public Affliction(boolean reapply, long tickDuration) {
        this.reapply = reapply;
        this.tickDuration = tickDuration;
        this.strength = -1;

        curTicks = new int[]{0};
        shouldCancel = false;
        shouldReapply = false;
    }

    /**
     * Constructs an affliction with a specified duration and strength.
     *
     * @param reapply      Whether the affliction should reapply each tick.
     * @param tickDuration Total duration in ticks.
     * @param strength     Strength of the affliction.
     */
    public Affliction(boolean reapply, long tickDuration, double strength) {
        this(reapply, tickDuration);
        this.strength = strength;
    }

    /**
     * Returns the number of ticks that have elapsed since application.
     *
     * @return Current tick count.
     */
    public int getCurTicks() {
        return curTicks[0];
    }

    /**
     * Returns the remaining ticks before the affliction ends.
     *
     * @return Ticks left.
     */
    public long getTicksLeft() {
        return tickDuration - curTicks[0];
    }

    /**
     * Defines logic for a single application tick of the affliction.
     * <p>
     * This should only contain effects applied for one or two ticks.
     *
     * @param afflicted The entity affected by the affliction.
     */
    protected abstract void onApply(SwordEntity afflicted);

    /**
     * Called when the affliction ends, either naturally or via cancellation.
     *
     * @param afflicted The entity affected by the affliction.
     */
    protected abstract void end(SwordEntity afflicted);

    /**
     * Applies the affliction to the entity if it exists and is alive.
     *
     * @param afflicted The entity to apply the affliction to.
     */
    public void apply(SwordEntity afflicted) {
        if (!entityExists(afflicted))
            onApply(afflicted);
    }

    /**
     * Starts the affliction lifecycle, scheduling periodic application until
     * duration expires or cancelled.
     *
     * @param afflicted The entity to apply the affliction to.
     */
    public void start(SwordEntity afflicted) {
        if (preApplicationCheck(afflicted)) return;

        apply(afflicted);
        new BukkitRunnable() {
            @Override
            public void run() {
                curTicks[0] += 2;
                if (shouldCancel || curTicks[0] > tickDuration) {
                    end(afflicted);
                    cancel();
                }
                else if (reapply || shouldReapply) {
                    if (shouldReapply) shouldReapply = false;
                    apply(afflicted);
                }
            }
        }.runTaskTimer(Sword.getInstance(), 2L, 2L);
    }

    public void cancel() {
        this.shouldCancel = true;
    }

    /**
     * Extends the affliction duration and optionally triggers reapplication.
     *
     * @param tickExtension Number of ticks to extend the affliction.
     */
    protected void extend(long tickExtension) {
        tickDuration += tickExtension;
        if (!reapply) shouldReapply = true;
    }

    /**
     * Checks whether the entity exists and is alive. Cancels the affliction if not.
     *
     * @param afflicted The entity to check.
     * @return True if the entity is null or dead.
     */
    protected boolean entityExists(SwordEntity afflicted) {
        if (afflicted == null || afflicted.entity().isDead()) {
            cancel();
            return true;
        }

        return false;
    }

    /**
     * Checks if the entity already has an affliction of the same type and
     * extends its duration if so.
     *
     * @param afflicted The entity to check.
     * @return True if the affliction was already present and extended.
     */
    protected boolean preApplicationCheck(SwordEntity afflicted) {
        Affliction current = afflicted.getAffliction(this.getClass());
        if (current != null) {
            current.extend(tickDuration - current.getTicksLeft());
            return true;
        }
        return false;
    }
}
