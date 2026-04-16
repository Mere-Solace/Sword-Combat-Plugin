package btm.sword.system.attack.dev;

import org.joml.Vector3f;

/**
 * A single world-space point deposited during a wand recording session, tagged with the
 * {@link PlacementMode} that was active when it was placed.
 *
 * <p>The mode tag is used by the visualization render loop to assign per-point colors
 * and by {@code SweepRecordingAction.saveDraft} to reconstruct the correct trajectory
 * type for each segment.</p>
 *
 * @param position world-space tip position recorded at placement time
 * @param mode     the active placement mode when this point was deposited
 */
public record PlacedPoint(Vector3f position, PlacementMode mode) {}
