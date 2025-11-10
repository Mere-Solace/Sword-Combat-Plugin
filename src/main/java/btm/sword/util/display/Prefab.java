package btm.sword.util.display;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.util.Vector;

import btm.sword.config.section.AudioConfig;
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

        public static final ParticleWrapper COLLIDE = new ParticleWrapper(Particle.CRIT, 10, 0.1, 0.1, 0.1, 0.5);

        public static final ParticleWrapper GRAB_CLOUD = new ParticleWrapper(Particle.POOF, 20, 0.5, 0.5, 0.5, 0.1);
        public static final ParticleWrapper GRAB_ATTEMPT = new ParticleWrapper(Particle.GUST, 2, 0.1, 0.1, 0.1);
        public static final ParticleWrapper THROW_TRAIl = new ParticleWrapper(Particle.DUST, 1, 0.2, 0.2, 0.2,
                new Particle.DustOptions(Color.WHITE, 2.5f));

                public static final ParticleWrapper GRAB_HIT_1 = new ParticleWrapper(Particle.FLAME, 50, 0.6, 0.6, 0.6, 0.02);
        public static final ParticleWrapper GRAB_HIT_2 = new ParticleWrapper(Particle.DUST, 3, 0.01, 0.01, 0.01,
                new Particle.DustOptions(Color.ORANGE, 3f));

        public static final ParticleWrapper TOUGH_BREAK_1 = new ParticleWrapper(Particle.GUST, 2, 0.1, 0.1, 0.1, 1);

        public static final ParticleWrapper TOUGH_RECHARGE_1 = new ParticleWrapper(Particle.LAVA, 10, 0.1, 0.1, 0.1, 0.25);
        public static final ParticleWrapper TOUGH_RECHARGE_2 = new ParticleWrapper(Particle.FLAME, 100, 0.5, 0.5, 0.5, 0.5);
    }

    public static class ControlVectors {
        public static final List<Vector> SLASH1 = new ArrayList<>(List.of(
                new Vector(-2.06, -1.26, -0.5),
                new Vector(3.26, 0.79, -0.4),
                new Vector(-2.3, -0.16,3),
                new Vector(1.9, 0.21, 5)));
        public static final List<Vector> SLASH2 = new ArrayList<>(List.of(
                new Vector(2.6, -1.21, -1.2),
                new Vector(-1.47, 1.99, 0),
                new Vector(1.6, -0.11, 7),
                new Vector(-3.66, 0.26, 1.85)));
        public static final List<Vector> SLASH3 = new ArrayList<>(List.of(
                new Vector(-0.15,2.8,-1.5),
                new Vector(-1.1,-2.2,-0.9),
                new Vector(1.74,1.96,4.3),
                new Vector(-1.1,-1.77,5)));

        public static final List<Vector> UP_SMASH = new ArrayList<>(List.of(
                new Vector(0.66,-1.53,-0.5),
                new Vector(-0.4,0.67,-0.9),
                new Vector(0.56,-0.89,2.1),
                new Vector(-0.4,1.37,1.65)));

        public static final List<Vector> SIDE_SWORD_SLASH_R = new ArrayList<>(List.of(
                new Vector(-1.3,1.03,2),
                new Vector(8.2,1.03,-1.9),
                new Vector(-7,-1.73,3.3),
                new Vector(9,-0.93,5)));
        public static final List<Vector> SIDE_SWORD_SLASH_L = new ArrayList<>(List.of(
                new Vector(1.3,1.03,2),
                new Vector(-8.2,1.03,-1.9),
                new Vector(7,-1.73,3.3),
                new Vector(-9,-0.93,5)));

        public static final List<Vector> DRAGON_KILLER = new ArrayList<>(List.of(
                new Vector(0.26, 2.23, -2.5),
                new Vector(0, -1.53, 2.66),
                new Vector(-0.63, 4.04, 0.74),
                new Vector(0.14, 1.8, 3.4)));
        public static final List<Double> dragonKillerArcRatios = new ArrayList<>(List.of(
                0.86, 2.13, 2.3, 0.87));

        public static final List<Vector> N_AIR_SLASH = new ArrayList<>(List.of(
                new Vector(1.0961, 1.742, -1.13),
                new Vector(0, -1.987, -0.791),
                new Vector(-0.2825, 0.951, 9.153),
                new Vector(-0.7458, -5.151, -1.808)));

        public static final List<Vector> D_AIR_SLASH = new ArrayList<>(List.of(
                new Vector(-0.35, 2.53, 0.56),
                new Vector(0, -3.42, -0.581),
                new Vector(0.329, -0.165, 4.97),
                new Vector(-0.07, -6.15, 0.98)));
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
