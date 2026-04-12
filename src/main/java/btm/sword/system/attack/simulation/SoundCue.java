package btm.sword.system.attack.simulation;

import org.bukkit.Sound;
import org.bukkit.SoundCategory;

/**
 * A sound played once when the attack's normalized time crosses a keyframe's {@code t} value.
 *
 * <p>The sound is fired at the keyframe's world-space centre position. Only one sound per
 * keyframe is supported; use multiple keyframes with the same {@code t} if multiple sounds
 * are needed.</p>
 *
 * @param sound    the Bukkit sound to play
 * @param category the sound category controlling which client volume slider applies
 * @param volume   sound volume (1.0 = normal, &gt;1.0 increases audible radius)
 * @param pitch    sound pitch (0.5–2.0; 1.0 = default pitch)
 */
public record SoundCue(Sound sound, SoundCategory category, float volume, float pitch) {}
