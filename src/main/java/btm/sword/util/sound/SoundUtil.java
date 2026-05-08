package btm.sword.util.sound;

import org.bukkit.entity.LivingEntity;

import btm.sword.Sword;
import btm.sword.config.section.AudioConfig;
import net.kyori.adventure.sound.Sound;

/**
 * Utility class for playing {@link btm.sword.util.sound.SwordSoundType}-keyed sounds to entities.
 * <p>
 * All playback is gated on {@link btm.sword.config.section.AudioConfig#SOUNDS_ENABLED}. Exceptions during
 * sound playback are caught and logged to prevent game-breaking errors from audio failures.
 * </p>
 */
public final class SoundUtil {

    private SoundUtil() {}

    /**
     * Plays the given sound to the target entity at the specified volume and pitch.
     * No-ops if {@link btm.sword.config.section.AudioConfig#SOUNDS_ENABLED} is {@code false}.
     *
     * @param target the entity to receive the sound
     * @param type   the sound key to play
     * @param volume playback volume (1.0 = normal)
     * @param pitch  playback pitch (1.0 = normal)
     */
    public static void playSound(LivingEntity target, SwordSoundType type, float volume, float pitch) {
        if (!AudioConfig.SOUNDS_ENABLED) return;

        try {
            Sound sound = Sound.sound(type, Sound.Source.PLAYER, volume, pitch);
            target.playSound(sound, Sound.Emitter.self());
        } catch (Exception e) {
            Sword.getInstance().getLogger().info(e.getMessage());
        }
    }
}
