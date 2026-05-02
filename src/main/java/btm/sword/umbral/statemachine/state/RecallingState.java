package btm.sword.umbral.statemachine.state;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import btm.sword.runtime.scheduler.TimeArbiter;
import btm.sword.umbral.UmbralBlade;
import btm.sword.umbral.input.BladeRequest;
import btm.sword.umbral.statemachine.UmbralStateFacade;
import btm.sword.util.display.DisplayUtil;

/**
 * State where the UmbralBlade is being recalled to the wielder.
 * <p>
 * In this state, the blade is traveling back to the player, typically after
 * being thrown, lodged in an enemy, or left in a waiting state.
 * </p>
 * <p>
 * <b>Entry Actions:</b>
 * <ul>
 *   <li>Set display transformation for returning animation</li>
 *   <li>Stop idle movement</li>
 *   <li>Begin lerp movement back to wielder</li>
 * </ul>
 * </p>
 * <p>
 * <b>Typical Transitions:</b>
 * <ul>
 *   <li>RECALLING → SHEATHED (when blade arrives)</li>
 * </ul>
 * </p>
 *
 */
public class RecallingState extends UmbralStateFacade {
    private static final int MAX_STATIONARY_ITERATIONS = 4;
    private static final double EPSILON_SQUARED = 0.004;

    private TimeArbiter.TaskHandle returnTask;

    @Override
    public String name() {
        return "RECALLING";
    }

    @Override
    public void onEnter(UmbralBlade blade) {
        AtomicReference<Location> currentBladeLoc = new AtomicReference<>(blade.getDisplay().getLocation());
        AtomicReference<Location> previousBladeLoc = new AtomicReference<>(blade.getDisplay().getLocation());
        final Supplier<Boolean> stationaryCheck = getBooleanSupplier(blade, currentBladeLoc, previousBladeLoc);

        returnTask = DisplayUtil.displaySlerpToOffset(
            blade.getThrower(), blade.getDisplay(),
            blade.getThrower().getChestVector(),
            1.5, 5, 100, 1.5,
            false,
            80,
            () -> blade.getThrower().isDashing(),
            stationaryCheck,
            () -> blade.request(BladeRequest.STANDBY)
        );
    }

    private static @NotNull Supplier<Boolean> getBooleanSupplier(UmbralBlade blade, AtomicReference<Location> currentBladeLoc, AtomicReference<Location> previousBladeLoc) {
        AtomicInteger stationaryCount = new AtomicInteger(0);
        return () -> {
            currentBladeLoc.set(blade.getDisplay().getLocation());
            Vector difference = currentBladeLoc.get().toVector().subtract(previousBladeLoc.get().toVector());
            if (difference.lengthSquared() < EPSILON_SQUARED) {
                stationaryCount.incrementAndGet();
            } else {
                stationaryCount.set(0);
            }
            previousBladeLoc.set(currentBladeLoc.get());
            return stationaryCount.get() > MAX_STATIONARY_ITERATIONS;
        };
    }

    @Override
    public void onExit(UmbralBlade blade) {
        if (returnTask != null && !returnTask.isCancelled()) {
            returnTask.cancel();
        }
    }

    @Override
    public void onTick(UmbralBlade blade) {
        if (returnTask != null && returnTask.isCancelled()) {
            blade.request(BladeRequest.STANDBY);
        }
    }
}
