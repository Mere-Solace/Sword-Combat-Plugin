package btm.sword.util;


import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.util.Vector;

import btm.sword.config.section.AudioConfig;
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
        //            new Particle.DustTransition(Color.fromRGB(211, 222, 240), Color.fromRGB(36, 103, 220), 0.7f));
        public static final ParticleWrapper TEST_HIT = new ParticleWrapper(Particle.CRIT, 30, 0.5, 0.5, 0.5, 0.15);
        public static final ParticleWrapper BLEED = new ParticleWrapper(Particle.BLOCK, 5, 0, 0, 0, Material.CRIMSON_HYPHAE.createBlockData());

        public static final ParticleWrapper THROWN_ITEM_IMPALE = new ParticleWrapper(Particle.TRIAL_SPAWNER_DETECTION, 4, 0.1, 0.1, 0.1, 0);

        public static final ParticleWrapper THROWN_ITEM_MARKER = new ParticleWrapper(Particle.TRIAL_SPAWNER_DETECTION, 3, 0.1, 0.1, 0.1, 0);
        public static final ParticleWrapper DOPPED_ITEM_MARKER = new ParticleWrapper(Particle.TRIAL_SPAWNER_DETECTION_OMINOUS, 4, 0.1, 0.1, 0.1, 0);

        public static final ParticleWrapper TEST_SWORD_BLUE = new ParticleWrapper(Particle.DUST_COLOR_TRANSITION, 5, 0.025, 0.025, 0.025, 1,
                new Particle.DustTransition(Color.fromRGB(14, 107, 207), Color.fromRGB(162, 226, 255), 0.75f));
        public static final ParticleWrapper TEST_SWORD_WHITE = new ParticleWrapper(Particle.DUST_COLOR_TRANSITION, 5, 0.025, 0.025, 0.025, 1,
                new Particle.DustTransition(Color.fromRGB(235, 243, 255), Color.fromRGB(120, 121, 255), 0.65f));

        public static final ParticleWrapper TEST_SPARKLE = new ParticleWrapper(Particle.ELECTRIC_SPARK, 2, 0, 0, 0, 0);

        public static final ParticleWrapper COLLIDE = new ParticleWrapper(Particle.CRIT, 10, 0.1, 0.1, 0.1, 0.5);

        public static final ParticleWrapper GRAB_CLOUD = new ParticleWrapper(Particle.POOF, 20, 0.5, 0.5, 0.5, 0.1);
        public static final ParticleWrapper GRAB_ATTEMPT = new ParticleWrapper(Particle.GUST, 3, 0.01, 0.01, 0.01);
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

    public static class Direction {
        public static final Vector UP = new Vector(0, 1, 0);
        public static final Vector DOWN = new Vector(0, -1, 0);
        public static final Vector NORTH = new Vector(0, 0, -1);
        public static final Vector SOUTH = new Vector(0, 0, 1);
        public static final Vector OUT_UP = new Vector(0, 1, 1);
        public static final Vector OUT_DOWN = new Vector(0, -1, 1);
    }

    public static class Value {
        public static final int MILLISECONDS_PER_TICK = 50; // 1000/20 = 50
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
        public static final SoundWrapper ATTACK = new SoundWrapper(AudioConfig::getAttackSound);

        /**
         * Throw sound effect for thrown items.
         * <p>
         * Used when throwing swords, axes, and other throwable items.
         * Properties configured in config.yaml under audio.throw.
         * </p>
         */
        public static final SoundWrapper THROW = new SoundWrapper(AudioConfig::getThrowSound);
    }
}
