package btm.sword.system.entity.umbral.statemachine.state;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.util.Vector;

import btm.sword.system.control.TimeArbiter;
import btm.sword.system.entity.umbral.UmbralBlade;
import btm.sword.system.entity.umbral.input.BladeRequest;
import btm.sword.system.entity.umbral.statemachine.UmbralStateFacade;
import btm.sword.utility.display.DisplayUtil;

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
        blade.getDisplay().setGlowing(true);
        blade.getDisplay().setGlowColorOverride(Color.fromRGB(1, 1, 1));

        AtomicReference<Location> currentBladeLoc = new AtomicReference<>(blade.getDisplay().getLocation());
        AtomicReference<Location> previousBladeLoc = new AtomicReference<>(blade.getDisplay().getLocation());
        final int[] stationaryCount = {0};

        Supplier<Boolean> stationaryCheck = () -> {
            currentBladeLoc.set(blade.getDisplay().getLocation());
            Vector difference = currentBladeLoc.get().toVector().subtract(previousBladeLoc.get().toVector());
            if (difference.lengthSquared() < EPSILON_SQUARED) {
                stationaryCount[0]++;
            } else {
                stationaryCount[0] = 0;
            }
            previousBladeLoc.set(currentBladeLoc.get());
            return stationaryCount[0] > MAX_STATIONARY_ITERATIONS;
        };

        returnTask = DisplayUtil.displaySlerpToOffset(
            blade.getThrower(), blade.getDisplay(),
            blade.getThrower().getChestVector(),
            1.5, 5, 100, 1.5,
            false,
            80,
            stationaryCheck,
            () -> blade.request(BladeRequest.STANDBY)
        );
    }

    @Override
    public void onExit(UmbralBlade blade) {
        blade.getDisplay().setGlowing(false);
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
