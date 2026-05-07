package btm.sword.umbral.statemachine.state;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import btm.sword.umbral.UmbralBlade;
import btm.sword.umbral.input.BladeRequest;
import btm.sword.umbral.motion.drivers.SlerpToOffsetDriver;
import btm.sword.umbral.statemachine.UmbralStateFacade;
import btm.sword.util.misc.Debug;

/**
 * State where the UmbralBlade is being recalled to the wielder.
 * <p>
 * In this state, the blade is traveling back to the player, typically after
 * being thrown, lodged in an enemy, or left in a waiting state.
 * </p>
 * <p>
 * <b>Entry Actions:</b>
 * <ul>
 *   <li>Install a {@link SlerpToOffsetDriver} on the blade's {@link btm.sword.umbral.motion.BladeMotion}
 *       targeting the wielder's chest offset.</li>
 *   <li>Compose a stationary-with-not-dashing end predicate to short-circuit the slerp when
 *       the blade has stopped moving (and the wielder is not dashing the target away).</li>
 * </ul>
 * </p>
 * <p>
 * <b>Typical Transitions:</b>
 * <ul>
 *   <li>RECALLING → STANDBY (on arrival, on stationary-without-dashing, or on timeout)</li>
 * </ul>
 * </p>
 */
public class RecallingState extends UmbralStateFacade {

    private static final int MAX_STATIONARY_ITERATIONS = 4;
    private static final double EPSILON_SQUARED = 0.004;

    private static final double SLERP_SPEED = 0.45;
    private static final double SLERP_ARRIVAL_DISTANCE = 1.5;
    private static final long SLERP_TIMEOUT_TICKS = 200;
    // Short duration so consecutive per-tick yaw aims don't pile up — keeps body rotation
    // tracking the player smoothly instead of chasing a stale target.
    private static final int SLERP_TELEPORT_DURATION = 2;

    @Override
    public String name() {
        return "RECALLING";
    }

    @Override
    public void onEnter(UmbralBlade blade) {
        Location displayLocation = blade.getDisplay().getLocation();
        AtomicReference<Location> currentBladeLoc = new AtomicReference<>(displayLocation);
        AtomicReference<Location> previousBladeLoc = new AtomicReference<>(displayLocation);
        Supplier<Boolean> stationaryCheck = stationaryCheckSupplier(blade, currentBladeLoc, previousBladeLoc);

        // The driver ends on arrival, on timeout, or on this composed end predicate. The
        // dashing-suppress is composed here at the call site instead of being a driver param,
        // since the driver should remain generic and not know about FSM-level concerns.
        Supplier<Boolean> endWhenStationaryAndNotDashing =
            () -> stationaryCheck.get() && !blade.getThrower().isDashing();

        Debug.umbral("RECALL install: speed=" + SLERP_SPEED + " arrival=" + SLERP_ARRIVAL_DISTANCE
            + " timeout=" + SLERP_TIMEOUT_TICKS + " teleportDuration=" + SLERP_TELEPORT_DURATION);

        blade.getBladeMotion().install(new SlerpToOffsetDriver(
            blade.getThrower(),
            blade.getThrower().getChestVector(),
            SLERP_SPEED,
            SLERP_ARRIVAL_DISTANCE,
            SLERP_TIMEOUT_TICKS,
            endWhenStationaryAndNotDashing,
            () -> {
                Debug.umbral("RECALL onArrive fired -> request STANDBY");
                blade.request(BladeRequest.STANDBY);
            },
            SLERP_TELEPORT_DURATION
        ));
    }

    private static @NotNull Supplier<Boolean> stationaryCheckSupplier(UmbralBlade blade,
                                                                      AtomicReference<Location> currentBladeLoc,
                                                                      AtomicReference<Location> previousBladeLoc) {
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
    public void onTick(UmbralBlade blade) {
        // Defensive: if the motion ended for any reason without the onArrive callback firing
        // (e.g. external dispose mid-flight), still request STANDBY so the FSM transitions out.
        if (blade.getBladeMotion().isEnded()) {
            blade.request(BladeRequest.STANDBY);
        }
    }

    @Override
    public void onExit(UmbralBlade blade) {
        blade.getBladeMotion().stop();
    }
}
