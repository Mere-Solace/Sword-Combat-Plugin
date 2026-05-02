package btm.sword.combat.dev;

import org.bukkit.Location;
import org.jetbrains.annotations.Nullable;

/**
 * A single world-space point deposited during a wand recording session, tagged with the
 * {@link PlacementMode} that was active when it was placed.
 *
 * <p>The mode tag is used by the visualization render loop to assign per-point colors
 * and by {@code SweepRecordingAction.saveDraft} to reconstruct the correct trajectory
 * type for each segment.</p>
 *
 * <p>For {@link PlacementMode#RAYCAST} points, {@code rayOrigin} holds the world-space
 * position where the ray started (eye + offset along look direction at placement time).
 * For all other modes it is {@code null}.</p>
 *
 * @param location  world-space tip position recorded at placement time
 * @param mode      the active placement mode when this point was deposited
 * @param rayOrigin world-space ray start position; non-null only for {@link PlacementMode#RAYCAST}
 */
public record PlacedPoint(Location location, PlacementMode mode, @Nullable Location rayOrigin) {

    /** Convenience constructor for non-RAYCAST modes — {@code rayOrigin} defaults to {@code null}. */
    public PlacedPoint(Location location, PlacementMode mode) {
        this(location, mode, null);
    }
}
