package btm.sword.system.attack.dev;

import org.joml.Vector3f;

/**
 * A single sweep-path sample captured during a recording session.
 *
 * @param localPosition the tip position in the player's local space (yaw-stripped,
 *                      +Z forward, +X right, +Y up), at {@code TIP_DISTANCE} blocks
 *                      from the body origin
 * @param capturedAtMs  absolute {@link System#currentTimeMillis()} at capture time,
 *                      used to derive the normalized {@code t} value when exporting
 *                      to {@link btm.sword.system.attack.simulation.VolumeKeyframe}
 */
public record RecordedSample(Vector3f localPosition, long capturedAtMs) {}
