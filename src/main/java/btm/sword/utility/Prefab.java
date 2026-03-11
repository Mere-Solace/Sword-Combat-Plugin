package btm.sword.utility;


import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

import btm.sword.config.Config;
import btm.sword.system.attack.Attack;
import btm.sword.system.attack.HitValuePacket;
import btm.sword.utility.display.ParticleWrapper;
import btm.sword.utility.sound.SoundType;
import btm.sword.utility.sound.SoundWrapper;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;

public class Prefab {
    public static class Particles {
        public static final ParticleWrapper TEST_FLAME = new ParticleWrapper(Particle.FLAME, 2, 0.025, 0.025, 0.025, 0);
        public static final ParticleWrapper TEST_SOUL_FLAME = new ParticleWrapper(Particle.SOUL_FIRE_FLAME, 1, 0.025, 0.025, 0.025, 0);
        public static final ParticleWrapper TEST_OBSIDIAN_TEAR = new ParticleWrapper(Particle.DRIPPING_OBSIDIAN_TEAR, 1, 0, 0, 0, 0);
        public static final ParticleWrapper DEBUG_BLOB = new ParticleWrapper(Particle.DRIPPING_OBSIDIAN_TEAR, 100, 0.2, 0.2, 0.2, 0);
        public static final ParticleWrapper TEST_LAVA_DRIP = new ParticleWrapper(Particle.DRIPPING_LAVA, 2, 0, 0, 0, 0);
        public static final ParticleWrapper TEST_SWING = new ParticleWrapper(Particle.DUST_COLOR_TRANSITION, 2, 0, 0, 0, 1,
                new Particle.DustTransition(Color.fromRGB(255, 0,0), Color.fromRGB(102,0,0), 0.7f));
        public static final ParticleWrapper TEST_HIT = new ParticleWrapper(Particle.CRIT, 30, 0.5, 0.5, 0.5, 0.15);
        public static final ParticleWrapper BLEED = new ParticleWrapper(Particle.BLOCK, 25, 0.1, 0.1, 0.1, Material.CRIMSON_HYPHAE.createBlockData());

        public static final ParticleWrapper THROWN_ITEM_IMPALE = new ParticleWrapper(Particle.TRIAL_SPAWNER_DETECTION, 4, 0.1, 0.1, 0.1, 0);

        public static final ParticleWrapper THROWN_ITEM_MARKER = new ParticleWrapper(Particle.TRIAL_SPAWNER_DETECTION, 3, 0.1, 0.1, 0.1, 0);

        /** Slow upward stream from the center of a landing prediction marker. */
        public static final ParticleWrapper LANDING_STREAM = new ParticleWrapper(Particle.TRIAL_SPAWNER_DETECTION, 1, 0, 0, 0, 0);
        public static final ParticleWrapper DOPPED_ITEM_MARKER = new ParticleWrapper(Particle.TRIAL_SPAWNER_DETECTION_OMINOUS, 4, 0.1, 0.1, 0.1, 0);

        public static final ParticleWrapper TEST_SWORD_BLUE = new ParticleWrapper(Particle.DUST_COLOR_TRANSITION, 5, 0.025, 0.025, 0.025, 1,
                new Particle.DustTransition(Color.fromRGB(14, 107, 207), Color.fromRGB(162, 226, 255), 0.75f));
        public static final ParticleWrapper TEST_SWORD_WHITE = new ParticleWrapper(Particle.DUST_COLOR_TRANSITION, 5, 0.025, 0.025, 0.025, 1,
                new Particle.DustTransition(Color.fromRGB(235, 243, 255), Color.fromRGB(120, 121, 255), 0.65f));

        public static final ParticleWrapper TEST_SPARKLE = new ParticleWrapper(Particle.ELECTRIC_SPARK, 2, 0, 0, 0, 0);

        public static final ParticleWrapper COLLIDE = new ParticleWrapper(Particle.CRIT, 1, 0.1, 0.1, 0.1, 0.5);

        public static final ParticleWrapper GRAB_CLOUD = new ParticleWrapper(Particle.POOF, 20, 0.5, 0.5, 0.5, 0.1);
        public static final ParticleWrapper GRAB_ATTEMPT = new ParticleWrapper(Particle.SONIC_BOOM, 2, 0.01, 0.01, 0.01);

        public static final ParticleWrapper PUNCH = new ParticleWrapper(Particle.SMALL_GUST, 1, 0, 0, 0, 0);
        public static final ParticleWrapper PUNCH_CONNECT = new ParticleWrapper(Particle.GUST, 1, 0, 0, 0, 0);

        public static final ParticleWrapper UMBRAL_BLADE_POOF = new ParticleWrapper(Particle.LARGE_SMOKE, 50, 0.5, 0.5, 0.5, 0.001);
        public static final ParticleWrapper SOULFIRE_POOF = new ParticleWrapper(Particle.SMOKE, 3, 0.05, 0.05, 0.05, 0.0001);
        public static final ParticleWrapper SMOKE = new ParticleWrapper(Particle.SMOKE, 1, 0.005, 0.005, 0.005, 0);

        public static final ParticleWrapper UMBRAL_FLAME = new ParticleWrapper(Particle.DUST_COLOR_TRANSITION, 3, 0.05, 0.05, 0.05, 1,
            new Particle.DustTransition(Color.fromRGB(53, 166, 240), Color.fromRGB(52, 72, 81), 0.5f));

        public static final ParticleWrapper THROW_TRAIl = new ParticleWrapper(Particle.DUST, 1, 0.2, 0.2, 0.2,
                new Particle.DustOptions(Color.WHITE, 2.5f));

        public static final ParticleWrapper ITEM_THROW_BREAK = new ParticleWrapper(Particle.ENCHANTED_HIT, 150, 0.4, 0.4, 0.4);

        public static final ParticleWrapper TOUGH_BREAK_1 = new ParticleWrapper(Particle.ENCHANTED_HIT, 70, 1, 1, 1, 0);

        public static final ParticleWrapper TOUGH_RECHARGE_1 = new ParticleWrapper(Particle.ENCHANT, 100, 1, 1, 1, 0.1);
        public static final ParticleWrapper TOUGH_RECHARGE_2 = new ParticleWrapper(Particle.SOUL_FIRE_FLAME, 40, 0.5, 0.5, 0.5, 0.75);
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
                !target.getUniqueId().equals(checkAndSelf.getLast().getUniqueId()) &&
                target.isValid() &&
                target.getType() != EntityType.ARMOR_STAND;
    }

    // using Suppliers so that this basic record-like class (AttackHitValue) can use the values from the config.
    public static class Attacks {
        public static final HitValuePacket defaultMobHit = new HitValuePacket(
            () -> Config.Combat.HIT_DEFAULT_MOB_REAPED_SOULFIRE,
            () -> Config.Combat.HIT_DEFAULT_MOB_INVULN_TICKS,
            () -> Config.Combat.HIT_DEFAULT_MOB_SHARD_DAMAGE,
            () -> Config.Combat.HIT_DEFAULT_MOB_TOUGHNESS_DAMAGE,
            () -> Config.Combat.HIT_DEFAULT_MOB_SOULFIRE_LOSS
        );

        public static final HitValuePacket basicAttack = new HitValuePacket(
            () -> 5f,
            () -> Config.Combat.ATTACK_CLASS_HIT_INVULN_TICKS,
            () -> Config.Combat.ATTACK_CLASS_HIT_SHARDS,
            () -> Config.Combat.ATTACK_CLASS_HIT_TOUGHNESS,
            () -> Config.Combat.ATTACK_CLASS_HIT_SOULFIRE
        );

        public static final HitValuePacket grabHit = new HitValuePacket(
            () -> Config.Combat.HIT_GRAB_REAPED_SOULFIRE,
            () -> Config.Combat.HIT_GRAB_INVULN_TICKS,
            () -> Config.Combat.HIT_GRAB_SHARD_DAMAGE,
            () -> Config.Combat.HIT_GRAB_TOUGHNESS_DAMAGE,
            () -> Config.Combat.HIT_GRAB_SOULFIRE_LOSS
        );

        public static final HitValuePacket thrownWeapon = new HitValuePacket(
            () -> 0f,
            () -> Config.Combat.THROWN_DAMAGE_SWORD_AXE_INVULNERABILITY_TICKS,
            () -> Config.Combat.THROWN_DAMAGE_SWORD_AXE_BASE_SHARDS,
            () -> Config.Combat.THROWN_DAMAGE_SWORD_AXE_TOUGHNESS_DAMAGE,
            () -> Config.Combat.THROWN_DAMAGE_SWORD_AXE_SOULFIRE_REDUCTION
        );

        public static final HitValuePacket umbralItemDisplayAttack = new HitValuePacket(
            () -> Config.Combat.HIT_UMBRAL_DISPLAY_REAPED_SOULFIRE,
            () -> Config.Combat.HIT_UMBRAL_DISPLAY_INVULN_TICKS,
            () -> Config.Combat.HIT_UMBRAL_DISPLAY_SHARD_DAMAGE,
            () -> Config.Combat.HIT_UMBRAL_DISPLAY_TOUGHNESS_DAMAGE,
            () -> Config.Combat.HIT_UMBRAL_DISPLAY_SOULFIRE_LOSS
        );

        public static final HitValuePacket punch = new HitValuePacket(
            () -> Config.Combat.HIT_PUNCH_REAPED_SOULFIRE,
            () -> Config.Combat.HIT_PUNCH_INVULN_TICKS,
            () -> Config.Combat.HIT_PUNCH_SHARD_DAMAGE,
            () -> Config.Combat.HIT_PUNCH_TOUGHNESS_DAMAGE,
            () -> Config.Combat.HIT_PUNCH_SOULFIRE_LOSS
        );
    }

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

        public static final SoundWrapper PRE_ATTACK = new SoundWrapper(
            () -> Config.Audio.PRE_ATTACK_SOUND,
            () -> Config.Audio.PRE_ATTACK_VOLUME,
            () -> Config.Audio.PRE_ATTACK_PITCH
        );

        public static final SoundWrapper SOULFIRE_GAIN_BACKGROUND = new SoundWrapper(
            () -> SoundType.PARTICLE_SOUL_ESCAPE,
            () -> 0.7f,
            () -> 0.2f
        );

        public static final SoundWrapper SHADOW_BLINK = new SoundWrapper(
            () -> SoundType.ENTITY_ENDERMAN_TELEPORT,
            () -> 1f,
            () -> 0.05f
        );
    }

    public static class Text {
        public static final List<Component> SOUL_LINK_LORE = List.of(
            Component.text("", Config.SwordColor.TEXT_ITEM_BASE),

            Component.text("Soul Link Controls", Config.SwordColor.TEXT_ITEM_HEADER, TextDecoration.ITALIC),

            Component.text("Shift + Swap", Config.SwordColor.TEXT_ITEM_CONTROLS)
                .append(Component.text(" – Toggle Standby / Sheathed", Config.SwordColor.TEXT_ITEM_BASE)),
            Component.text("  • Standby: ", Config.SwordColor.TEXT_ITEM_HEADER)
                .append(Component.text("Blade hovers, awaits commands", Config.SwordColor.TEXT_ITEM_BASE)),
            Component.text("  • Sheathed: ", Config.SwordColor.TEXT_ITEM_HEADER)
                .append(Component.text("Blade returns to your hip", Config.SwordColor.TEXT_ITEM_BASE)),

            Component.text("", Config.SwordColor.TEXT_ITEM_BASE),

            Component.text("Shift + Drop", Config.SwordColor.TEXT_ITEM_CONTROLS)
                .append(Component.text(" – Wield Blade", Config.SwordColor.TEXT_ITEM_BASE)),
            Component.text("  • Equips the Blade directly into your hand", Config.SwordColor.TEXT_ITEM_BASE),

            Component.text("", Config.SwordColor.TEXT_ITEM_BASE),

            Component.text("Left Click", Config.SwordColor.TEXT_ITEM_CONTROLS)
                .append(Component.text(" – Quick Attack (Standby, costs Soulfire)", Config.SwordColor.TEXT_ITEM_BASE)),

            Component.text("", Config.SwordColor.TEXT_ITEM_BASE),

            Component.text("Drop + Right Click", Config.SwordColor.TEXT_ITEM_CONTROLS)
                .append(Component.text(" – Lunge (Standby, costs Soulfire)", Config.SwordColor.TEXT_ITEM_BASE))
        );

        public static final List<Component> UMBRAL_BLADE_LORE = List.of(
            Component.text("", Config.SwordColor.TEXT_ITEM_BASE),

            Component.text("Umbral Blade Techniques", Config.SwordColor.TEXT_ITEM_HEADER, TextDecoration.ITALIC),

            Component.text("Left Click (×1/2/3)", Config.SwordColor.TEXT_ITEM_CONTROLS)
                .append(Component.text(" – Attack Chain", Config.SwordColor.TEXT_ITEM_BASE)),
            Component.text("Drop + Left Click (×1)", Config.SwordColor.TEXT_ITEM_CONTROLS)
                .append(Component.text(" – Heavy Sweep", Config.SwordColor.TEXT_ITEM_BASE)),
            Component.text("  • Repeated Left clicks increase sweep force", Config.SwordColor.TEXT_ITEM_BASE),
            Component.text("Drop + Right Click", Config.SwordColor.TEXT_ITEM_CONTROLS)
                .append(Component.text(" – Lunge Throw", Config.SwordColor.TEXT_ITEM_BASE)),
            Component.text("Shift + Swap", Config.SwordColor.TEXT_ITEM_CONTROLS)
                .append(Component.text(" – Return to Standby", Config.SwordColor.TEXT_ITEM_BASE)),

            Component.text("", Config.SwordColor.TEXT_ITEM_BASE),

            Component.text("Swap + Left [combo]", Config.SwordColor.TEXT_ITEM_CONTROLS)
                .append(Component.text(" – Umbral Skills", Config.SwordColor.TEXT_ITEM_BASE))
        );

    }
}
