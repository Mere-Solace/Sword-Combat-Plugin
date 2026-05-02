package btm.sword.util.entity;

import java.util.function.Supplier;

import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import btm.sword.entity.base.SwordEntity;

/** Wraps {@link PotionEffect} construction behind suppliers for Config hot-reload compatibility. */
public record PotionEffectWrapper(Supplier<PotionEffectType> type, Supplier<Integer> duration,
                                  Supplier<Integer> amplifier, Supplier<Boolean> ambient, Supplier<Boolean> particles,
                                  Supplier<Boolean> icon) {

    /** Convenience constructor for a simple non-ambient, non-particle potion effect. */
    public PotionEffectWrapper(PotionEffectType type, int duration, int amplifier) {
        this(() -> type, () -> duration, () -> amplifier, () -> false, () -> false, () -> false);
    }

    /** Builds and returns a new {@link PotionEffect} from the current supplier values. */
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

    /** Applies the potion effect to the given living entity. */
    public void apply(LivingEntity entity) {
        entity.addPotionEffect(create());
    }

    /** Applies the potion effect to the underlying living entity of the given sword entity. */
    public void apply(SwordEntity entity) {
        entity.self().addPotionEffect(create());
    }
}
