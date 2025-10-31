package btm.sword.util;

import btm.sword.Sword;
import btm.sword.util.sound.SoundType;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.entity.LivingEntity;

public class SoundUtil {
    public static void playSound(LivingEntity target, SoundType type, float volume, float pitch) {
        try {
            Sound sound = Sound.sound(Key.key(type.getKey()), Sound.Source.PLAYER, volume, pitch);
            target.playSound(sound, Sound.Emitter.self());
        } catch (Exception e) {
            Sword.getInstance().getLogger().info(e.getMessage());
        }
    }
}
