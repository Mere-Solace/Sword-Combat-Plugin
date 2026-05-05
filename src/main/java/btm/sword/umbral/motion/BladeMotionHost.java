package btm.sword.umbral.motion;

import org.bukkit.Location;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;

import btm.sword.entity.base.Combatant;

/**
 * Bridge between the motion subsystem and the underlying display + thrower state owned by
 * the blade. Exposes only the narrow surface that {@link BladeMotionDriver}s legitimately need:
 * read access to the thrower and write access to the display's position and transformation.
 * <p>
 * <b>Why an interface:</b> the motion package must remain agnostic to the blade's internal
 * representation of its display. Today the blade exposes a Bukkit {@code ItemDisplay} via
 * inheritance from {@code ThrownItem}; in the future it will expose a {@code BladeDisplay}
 * wrapper. The driver code does not change in either case.
 * <p>
 * Implementations are expected to be supplied at construction time of the {@link BladeMotion}
 * and to remain valid for that {@code BladeMotion}'s entire lifetime.
 */
public interface BladeMotionHost {

    /** Returns the combatant who owns the blade. Read-only access. */
    Combatant thrower();

    /**
     * Teleports the blade display to the given location with the given facing direction.
     *
     * @param location          the target world location
     * @param direction         the direction the display should face after teleporting; may be
     *                          {@code null} to leave the current direction unchanged
     * @param teleportDuration  the smoothing duration in ticks (clamped to Bukkit's 0..59 range)
     */
    void teleportDisplay(Location location, Vector direction, int teleportDuration);

    /**
     * Sets the blade display's transformation directly.
     *
     * @param transformation     the new transformation to apply
     * @param transformDuration  the interpolation duration in milliseconds
     */
    void setDisplayTransformation(Transformation transformation, int transformDuration);

    /** Returns the current world location of the blade display, or {@code null} if unavailable. */
    Location currentDisplayLocation();

    /** Returns {@code true} if the blade display is alive and valid in the world. */
    boolean isDisplayValid();
}
