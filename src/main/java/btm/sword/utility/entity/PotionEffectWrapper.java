package btm.sword.utility.entity;

import java.util.function.Supplier;

import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import btm.sword.system.entity.base.SwordEntity;

public record PotionEffectWrapper(Supplier<PotionEffectType> type, Supplier<Integer> duration,
                                  Supplier<Integer> amplifier, Supplier<Boolean> ambient, Supplier<Boolean> particles,
                                  Supplier<Boolean> icon) {

    public PotionEffectWrapper(PotionEffectType type, int duration, int amplifier) {
        this(() -> type, () -> duration, () -> amplifier, () -> false, () -> false, () -> false);
    }

    public PotionEffect create() {
        return new PotionEffect(
            type.get(),
            duration.get(),
            amplifier.get(),
            ambient.get(),
            particles.get(),
            icon.get()
        );
    }

    public void apply(LivingEntity entity) {
        entity.addPotionEffect(create());
    }

    public void apply(SwordEntity entity) {
        entity.self().addPotionEffect(create());
    }
}
