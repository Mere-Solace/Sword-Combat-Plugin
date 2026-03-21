package btm.sword.utility.sound;

import org.bukkit.entity.LivingEntity;

import btm.sword.Sword;
import btm.sword.config.Config;
import net.kyori.adventure.sound.Sound;

public class SoundUtil {
    public static void playSound(LivingEntity target, SwordSoundType type, float volume, float pitch) {
        if (!Config.Audio.SOUNDS_ENABLED) return;

        try {
            Sound sound = Sound.sound(type, Sound.Source.PLAYER, volume, pitch);
            target.playSound(sound, Sound.Emitter.self());
        } catch (Exception e) {
            Sword.getInstance().getLogger().info(e.getMessage());
        }
    }
}
