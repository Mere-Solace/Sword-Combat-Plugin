package btm.sword.system.attack.simulation;

import java.util.List;

import org.jetbrains.annotations.Nullable;

/**
 * Effect bundle attached to a {@link VolumeKeyframe}.
 *
 * <p>All effects in this bundle fire exactly once when the simulation's normalized time
 * {@code t} crosses the keyframe's {@code t} value. Dispatch is handled by
 * {@link EffectsDispatcher} on the main thread.</p>
 *
 * @param particles zero or more particle bursts to spawn at the keyframe's world-centre
 * @param sound     optional sound cue played at the keyframe's world-centre; {@code null} for no sound
 */
public record KeyframeEffect(
    List<ParticleEffect> particles,
    @Nullable SoundCue sound
) {}
