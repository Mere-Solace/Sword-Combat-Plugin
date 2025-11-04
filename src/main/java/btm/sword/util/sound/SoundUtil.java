package btm.sword.util.sound;

import btm.sword.Sword;
import net.kyori.adventure.sound.Sound;
import org.bukkit.entity.LivingEntity;

public class SoundUtil {
    public static void playSound(LivingEntity target, SoundType type, float volume, float pitch) {
        try {
            Sound sound = Sound.sound(type, Sound.Source.PLAYER, volume, pitch);
            target.playSound(sound, Sound.Emitter.self());
        } catch (Exception e) {
            Sword.getInstance().getLogger().info(e.getMessage());
        }
    }
}
