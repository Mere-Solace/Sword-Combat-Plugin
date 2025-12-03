package btm.sword.system.entity.umbral.statemachine.state;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.util.Vector;

import btm.sword.system.control.TimeArbiter;
import btm.sword.system.entity.umbral.UmbralBlade;
import btm.sword.system.entity.umbral.input.BladeRequest;
import btm.sword.system.entity.umbral.statemachine.UmbralStateFacade;

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
    private Location previousBladeLocation;
    private int t = 0;
    private int stationaryCount = 0;
    private static final double EPS_SQ = 0.0004; // tune: 0.02^2  (very small)
    private static final int REQUIRED_STATIONARY_TICKS = 3;

    private TimeArbiter.TaskHandle returnTask;

    @Override
    public String name() {
        return "RECALLING";
    }

    @Override
    public void onEnter(UmbralBlade blade) {
        blade.getDisplay().setGlowing(true);
        blade.getDisplay().setGlowColorOverride(Color.fromRGB(1, 1, 1));

        previousBladeLocation = blade.getDisplay().getLocation();
        returnTask = blade.returnToWielderAndRequestState(BladeRequest.STANDBY);
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
        t++;

        // wait initial grace period for return animation to run
        if (t <= 15) return;

        Location cur = blade.getDisplay().getLocation();

        if (previousBladeLocation == null) {
            previousBladeLocation = cur.clone();
            stationaryCount = 0;
            return;
        }

        Vector delta = cur.toVector().clone().subtract(previousBladeLocation.toVector());
        if (delta.lengthSquared() < EPS_SQ) {
            stationaryCount++;
            if (stationaryCount >= REQUIRED_STATIONARY_TICKS) {
                blade.request(BladeRequest.STANDBY);
            }
        } else {
            stationaryCount = 0;
        }

        previousBladeLocation = cur.clone();
    }
}
