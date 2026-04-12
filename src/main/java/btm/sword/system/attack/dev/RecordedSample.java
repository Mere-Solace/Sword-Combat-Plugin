package btm.sword.system.attack.dev;

import org.joml.Vector3f;

/**
 * A single sweep-path sample captured during a recording session.
 *
 * @param worldPosition the tip position in world space, computed as
 *                      {@code eyePos + lookDir * TIP_DISTANCE}
 * @param capturedAtMs  absolute {@link System#currentTimeMillis()} at capture time,
 *                      used to derive the normalized {@code t} value when exporting
 *                      to {@link btm.sword.system.attack.simulation.VolumeKeyframe}
 */
public record RecordedSample(Vector3f worldPosition, long capturedAtMs) {}
