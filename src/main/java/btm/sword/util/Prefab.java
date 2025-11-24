package btm.sword.util;


import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import btm.sword.util.sound.SoundType;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

import btm.sword.config.Config;
import btm.sword.system.attack.Attack;
import btm.sword.system.attack.HitPacket;
import btm.sword.util.display.ParticleWrapper;
import btm.sword.util.sound.SoundWrapper;

public class Prefab {
    public static class Particles {
        public static final ParticleWrapper TEST_FLAME = new ParticleWrapper(Particle.FLAME, 2, 0.025, 0.025, 0.025, 0);
        public static final ParticleWrapper TEST_SOUL_FLAME = new ParticleWrapper(Particle.SOUL_FIRE_FLAME, 1, 0.025, 0.025, 0.025, 0);
        public static final ParticleWrapper TEST_OBSIDIAN_TEAR = new ParticleWrapper(Particle.DRIPPING_OBSIDIAN_TEAR, 1, 0, 0, 0, 0);
        public static final ParticleWrapper TEST_LAVA_DRIP = new ParticleWrapper(Particle.DRIPPING_LAVA, 2, 0, 0, 0, 0);
        public static final ParticleWrapper TEST_SWING = new ParticleWrapper(Particle.DUST_COLOR_TRANSITION, 2, 0, 0, 0, 1,
                new Particle.DustTransition(Color.fromRGB(255, 0,0), Color.fromRGB(102,0,0), 0.7f));
        public static final ParticleWrapper TEST_HIT = new ParticleWrapper(Particle.CRIT, 30, 0.5, 0.5, 0.5, 0.15);
        public static final ParticleWrapper BLEED = new ParticleWrapper(Particle.BLOCK, 25, 0.1, 0.1, 0.1, Material.CRIMSON_HYPHAE.createBlockData());

        public static final ParticleWrapper THROWN_ITEM_IMPALE = new ParticleWrapper(Particle.TRIAL_SPAWNER_DETECTION, 4, 0.1, 0.1, 0.1, 0);

        public static final ParticleWrapper THROWN_ITEM_MARKER = new ParticleWrapper(Particle.TRIAL_SPAWNER_DETECTION, 3, 0.1, 0.1, 0.1, 0);
        public static final ParticleWrapper DOPPED_ITEM_MARKER = new ParticleWrapper(Particle.TRIAL_SPAWNER_DETECTION_OMINOUS, 4, 0.1, 0.1, 0.1, 0);

        public static final ParticleWrapper TEST_SWORD_BLUE = new ParticleWrapper(Particle.DUST_COLOR_TRANSITION, 5, 0.025, 0.025, 0.025, 1,
                new Particle.DustTransition(Color.fromRGB(14, 107, 207), Color.fromRGB(162, 226, 255), 0.75f));
        public static final ParticleWrapper TEST_SWORD_WHITE = new ParticleWrapper(Particle.DUST_COLOR_TRANSITION, 5, 0.025, 0.025, 0.025, 1,
                new Particle.DustTransition(Color.fromRGB(235, 243, 255), Color.fromRGB(120, 121, 255), 0.65f));

        public static final ParticleWrapper TEST_SPARKLE = new ParticleWrapper(Particle.ELECTRIC_SPARK, 2, 0, 0, 0, 0);

        public static final ParticleWrapper COLLIDE = new ParticleWrapper(Particle.CRIT, 1, 0.1, 0.1, 0.1, 0.5);

        public static final ParticleWrapper GRAB_CLOUD = new ParticleWrapper(Particle.POOF, 20, 0.5, 0.5, 0.5, 0.1);
        public static final ParticleWrapper GRAB_ATTEMPT = new ParticleWrapper(Particle.SONIC_BOOM, 2, 0.01, 0.01, 0.01);
        public static final ParticleWrapper PUNCH = new ParticleWrapper(Particle.GUST, 1, 0, 0, 0, 0);

        public static final ParticleWrapper UMBRAL_POOF = new ParticleWrapper(Particle.LARGE_SMOKE, 50, 0.5, 0.5, 0.5, 0.001);

        public static final ParticleWrapper THROW_TRAIl = new ParticleWrapper(Particle.DUST, 1, 0.2, 0.2, 0.2,
                new Particle.DustOptions(Color.WHITE, 2.5f));

                public static final ParticleWrapper GRAB_HIT_1 = new ParticleWrapper(Particle.FLAME, 50, 0.6, 0.6, 0.6, 0.02);
        public static final ParticleWrapper GRAB_HIT_2 = new ParticleWrapper(Particle.DUST, 3, 0.01, 0.01, 0.01,
                new Particle.DustOptions(Color.ORANGE, 3f));

        public static final ParticleWrapper TOUGH_BREAK_1 = new ParticleWrapper(Particle.GUST, 2, 0.1, 0.1, 0.1, 1);

        public static final ParticleWrapper TOUGH_RECHARGE_1 = new ParticleWrapper(Particle.LAVA, 10, 0.1, 0.1, 0.1, 0.25);
        public static final ParticleWrapper TOUGH_RECHARGE_2 = new ParticleWrapper(Particle.FLAME, 100, 0.5, 0.5, 0.5, 0.5);
    }

    public static class Value {
        public static final int MILLISECONDS_PER_TICK = 50; // 1000/20 = 50
    }

    public static class Instructions {
        public static final Function<Attack, Vector> DEFAULT_KNOCKBACK =
            a -> a.getTo().add(a.getForwardVector());

        /**
         * 1st in list: target to be checked
         * </p>
         * 2nd in list: self/entity to be excluded
         */
        public static final Predicate<List<Entity>> DEFAULT_HITBOX_FILTER = // TODO: #125 - Make more of these and use them
            checkAndSelf ->
                checkAndSelf.size() == 2 &&
                (checkAndSelf.getFirst() instanceof LivingEntity target) &&
                target.getUniqueId() != checkAndSelf.getLast().getUniqueId() &&
                target.isValid() &&
                target.getType() != EntityType.ARMOR_STAND;
    }

    // using Suppliers so that this basic record-like class (AttackHitValue) can use the values from the config.
    public static class Attacks {
        public static final HitPacket defaultMobHit = new HitPacket(
            () -> 5f,
            () -> 15,
            () -> 1,
            () -> 10f,
            () -> 10f
        );

        public static final HitPacket basicAttack = new HitPacket(
            () -> 10f,
            () -> Config.Combat.ATTACK_CLASS_HIT_INVULN_TICKS,
            () -> Config.Combat.ATTACK_CLASS_HIT_SHARDS,
            () -> Config.Combat.ATTACK_CLASS_HIT_TOUGHNESS,
            () -> Config.Combat.ATTACK_CLASS_HIT_SOULFIRE
        );

        public static final HitPacket grabHit = new HitPacket(
            () -> 1f,
            () -> 0,
            () -> 0,
            () -> 5f,
            () -> 5f
        );

        public static final HitPacket thrownWeapon = new HitPacket(
            () -> 0f,
            () -> Config.Combat.THROWN_DAMAGE_SWORD_AXE_INVULNERABILITY_TICKS,
            () -> Config.Combat.THROWN_DAMAGE_SWORD_AXE_BASE_SHARDS,
            () -> Config.Combat.THROWN_DAMAGE_SWORD_AXE_TOUGHNESS_DAMAGE,
            () -> Config.Combat.THROWN_DAMAGE_SWORD_AXE_SOULFIRE_REDUCTION
        );

        public static final HitPacket umbralItemDisplayAttack = new HitPacket(
            () -> 0f,
            () -> 5,
            () -> 1,
            () -> 15f,
            () -> 10f
        );

        public static final HitPacket punch = new HitPacket(
            () -> 10f,
            () -> 2,
            () -> 1,
            () -> 5f,
            () -> 5f
        );
    }

    /**
     * Prefab sound effects that internally use the configuration system.
     * <p>
     * Provides pre-configured {@link SoundWrapper} instances that fetch properties
     * from config.yaml at play time, enabling hot-reload functionality.
     * Sound properties (type, volume, pitch) are dynamically loaded from the
     * configuration system when {@link SoundWrapper#play(org.bukkit.entity.LivingEntity)}
     * is called.
     * </p>
     * <p>
     * Usage: {@code Prefab.Sounds.ATTACK.play(entity);}
     * </p>
     */
    public static class Sounds {
        /**
         * Attack sound effect for melee combat.
         * <p>
         * Used for basic slashes, aerial attacks, and other melee actions.
         * Properties configured in config.yaml under audio.attack.
         * </p>
         */
        public static final SoundWrapper ATTACK = new SoundWrapper(
            () -> Config.Audio.ATTACK_SOUND,
            () -> Config.Audio.ATTACK_VOLUME,
            () -> Config.Audio.ATTACK_PITCH
        );

        public static final SoundWrapper PUNCH_ATTEMPT = new SoundWrapper(
            () -> Config.Audio.PUNCH_ATTEMPT,
            () -> Config.Audio.PUNCH_ATTEMPT_VOL,
            () -> Config.Audio.PUNCH_ATTEMPT_PITCH
        );

        public static final SoundWrapper PUNCH_CONNECT = new SoundWrapper(
            () -> Config.Audio.PUNCH_CONNECT,
            () -> Config.Audio.PUNCH_CONNECT_VOL,
            () -> Config.Audio.PUNCH_CONNECT_PITCH
        );

        /**
         * Throw sound effect for thrown items.
         * <p>
         * Used when throwing swords, axes, and other throwable items.
         * Properties configured in config.yaml under audio.throw.
         * </p>
         */
        public static final SoundWrapper THROW = new SoundWrapper(
            () -> Config.Audio.THROW_SOUND,
            () -> Config.Audio.THROW_VOLUME,
            () -> Config.Audio.THROW_PITCH
        );
    }
}
