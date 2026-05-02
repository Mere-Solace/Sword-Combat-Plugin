package btm.sword.runtime.scheduler;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;

import btm.sword.Sword;

/**
 * A cancellable, sequential chain of delayed tasks built on {@link SwordScheduler}.
 *
 * <p>Steps are enqueued via {@link #andThen} and execute in order, each step's delay
 * measured from when the <em>previous</em> step ran (not from chain creation).
 * Tasks started inside a step (e.g. looping animations) are independent and continue
 * running across step boundaries — only the pending <em>next</em> step is cancelled
 * when {@link #cancel()} is called.</p>
 *
 * <p>Obtain an instance via {@link SwordScheduler#after}:</p>
 * <pre>{@code
 * ScheduleChain chain = SwordScheduler.after(500, TimeUnit.MILLISECONDS, this::phaseOne)
 *     .andThen(1000, TimeUnit.MILLISECONDS, this::phaseTwo)
 *     .andThen(500,  TimeUnit.MILLISECONDS, this::phaseThree);
 *
 * // Later, if needed:
 * chain.cancel();
 * }</pre>
 */
public final class ScheduleChain {

    private record Step(int delay, TimeUnit unit, Runnable runnable) {}

    private final List<Step> steps = new ArrayList<>();
    private volatile ScheduledFuture<?> pendingFuture;
    private volatile boolean cancelled = false;

    ScheduleChain() {}

    /**
     * Adds a step that runs after the previous step completes, delayed by the given duration.
     *
     * @param delay    how long to wait after the previous step runs
     * @param unit     the time unit of the delay
     * @param runnable the task to execute on the main thread
     * @return this chain, for fluent chaining
     */
    public ScheduleChain andThen(int delay, TimeUnit unit, Runnable runnable) {
        steps.add(new Step(delay, unit, runnable));
        return this;
    }

    /**
     * Cancels any pending (not-yet-started) step in this chain.
     * Steps already executing and any independent looping tasks they started are unaffected.
     */
    public void cancel() {
        cancelled = true;
        ScheduledFuture<?> future = pendingFuture;
        if (future != null) {
            future.cancel(false);
        }
    }

    /**
     * Adds the first step and starts the chain. Called by {@link SwordScheduler#after}.
     */
    ScheduleChain start(int delay, TimeUnit unit, Runnable runnable) {
        steps.add(new Step(delay, unit, runnable));
        scheduleStep(0);
        return this;
    }

    private void scheduleStep(int index) {
        Step step = steps.get(index);
        pendingFuture = Sword.getScheduler().schedule(() -> {
            if (cancelled) return;
            Bukkit.getScheduler().runTask(Sword.getInstance(), () -> {
                if (cancelled) return;
                step.runnable().run();
                int next = index + 1;
                if (next < steps.size()) {
                    scheduleStep(next);
                }
            });
        }, step.delay(), step.unit());
    }
}
