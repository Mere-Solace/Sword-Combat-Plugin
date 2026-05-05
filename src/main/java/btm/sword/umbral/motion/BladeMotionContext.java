package btm.sword.umbral.motion;

import org.bukkit.Location;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;

import btm.sword.entity.base.Combatant;

/**
 * Per-driver view of the motion subsystem.
 * <p>
 * Drivers receive an instance of this class as the only argument to their lifecycle methods.
 * The context forwards display operations to a {@link BladeMotionHost} and exposes a tick
 * counter that resets each time a new driver is installed. Drivers MUST NOT cache the context
 * reference outside of their own internal fields and MUST NOT downcast it.
 * <p>
 * The instance is created by and owned exclusively by {@link BladeMotion}; only that class may
 * mutate the tick counter via the package-private accessors below.
 */
public final class BladeMotionContext {

    private final BladeMotionHost host;
    private long ticksSinceInstall;

    BladeMotionContext(BladeMotionHost host) {
        this.host = host;
    }

    /** Returns the combatant who owns the blade. Read-only access. */
    public Combatant thrower() {
        return host.thrower();
    }

    /**
     * Teleports the blade display to the given location with the given facing direction.
     * See {@link BladeMotionHost#teleportDisplay} for parameter semantics.
     */
    public void teleportTo(Location location, Vector direction, int teleportDuration) {
        host.teleportDisplay(location, direction, teleportDuration);
    }

    /**
     * Sets the blade display's transformation directly.
     * See {@link BladeMotionHost#setDisplayTransformation} for parameter semantics.
     */
    public void setTransformation(Transformation transformation, int transformDuration) {
        host.setDisplayTransformation(transformation, transformDuration);
    }

    /** Returns the current world location of the blade display, or {@code null} if unavailable. */
    public Location currentLocation() {
        return host.currentDisplayLocation();
    }

    /** Returns {@code true} if the blade display is alive and valid in the world. */
    public boolean isDisplayValid() {
        return host.isDisplayValid();
    }

    /** Returns the number of ticks the currently-installed driver has been ticked, starting at 0. */
    public long tickCount() {
        return ticksSinceInstall;
    }

    void resetTicksSinceInstall() {
        ticksSinceInstall = 0;
    }

    void incrementTicksSinceInstall() {
        ticksSinceInstall++;
    }
}
