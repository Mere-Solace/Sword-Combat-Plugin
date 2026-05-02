package btm.sword.entity.mob;

import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import btm.sword.combat.affliction.Affliction;
import btm.sword.config.Config;
import btm.sword.entity.arbiter.SwordEntityArbiter;
import btm.sword.entity.aspect.AspectType;
import btm.sword.entity.aspect.value.ResourceValue;
import btm.sword.entity.base.CombatProfile;
import btm.sword.entity.base.Combatant;
import btm.sword.entity.base.SoulfireManager;
import btm.sword.entity.player.SwordPlayer;
import btm.sword.util.prefab.Prefab;
import btm.sword.util.sound.SoundUtil;
import btm.sword.util.sound.SwordSoundType;
import lombok.Setter;


/**
 * A placeable training dummy entity.
 * <p>
 * Wraps an {@link ArmorStand} with a minimal {@link btm.sword.entity.base.CombatProfile}
 * (10 shards, 2000 max, regen 1/tick). Owned by a {@link SwordPlayer}; removed from the owner's
 * dummy list on death.
 * </p>
 */
public class Dummy extends Passive {
    @Setter
    private SwordPlayer owner;

    /**
     * Constructs a new SwordEntity wrapping the specified {@link LivingEntity} and combat profile.
     * Initializes resources, afflictions, and starts ticking updates.
     *
     * @param dummy          the Bukkit {@link LivingEntity} to wrap
     * @param combatProfile the {@link CombatProfile} associated with this entity
     */
    public Dummy(ArmorStand dummy, @NotNull CombatProfile combatProfile) {
        super(dummy, combatProfile);
        combatProfile.setStat(AspectType.SHARDS, new ResourceValue(10, 2000, 1));
    }

    /** Returns the underlying {@link ArmorStand} entity for this dummy. */
    public ArmorStand armorStand() {
        return (ArmorStand) self;
    }

    @Override
    protected void onTick() {
        super.onTick();
    }

    @Override
    public void onDeath() {
        if (owner == null || owner.isInvalid()) return;
        owner.getYourDummies().remove(this);
        owner.decrementNumDummies();
        super.onDeath();
    }

    @Override
    public void hit(Combatant source,
                    float reapedSoulfire,
                    long hitInvulnerableTickDuration,
                    int baseNumShards,
                    float baseToughnessDamage,
                    float baseSoulfireReduction,
                    Vector knockbackVelocity,
                    Affliction... afflictions) {
        if (isHit())
            return;
        else
            setHit(true);

        SoulfireManager.transferSoulfire(source, this, reapedSoulfire);

        if (baseNumShards > 70000) {
            self.remove();
            SwordEntityArbiter.remove(self);
            return;
        }

        this.setHitInvulnerableTickDuration(hitInvulnerableTickDuration);

        self.damage(0.01);

        Prefab.Particles.TEST_HIT.display(getChestLocation());
        SoundUtil.playSound(source.self(), SwordSoundType.ENTITY_PLAYER_ATTACK_STRONG,
            Config.Audio.ENTITY_HIT_CONNECT_VOLUME, Config.Audio.ENTITY_HIT_CONNECT_PITCH);

//        self.setVelocity(knockbackVelocity);

        // If Toughness == 0
        if (aspects.toughness().remove(baseToughnessDamage)) {
            if (!toughnessBroken) {
                Prefab.Particles.TOUGH_BREAK_1.display(getChestLocation());
                onToughnessBroken();
            }
            self.playHurtAnimation(0);
            displayShardLoss();
            aspects.restartResourceProcessAfterDelay(AspectType.SHARDS, aspects.shards().getBaseRegenPeriod());
        }

        // remove returns true only if the value reaches or goes below 0
        if (toughnessBroken) {
            // If Shards == 0 (dead)
            if (aspects.shards().remove(baseNumShards)) {
                aspects.shards().setCur(aspects.shards().effectiveMaxValue());
                return;
            }
            shardsLostDuringToughnessBreak += baseNumShards;


            if (shardsLostDuringToughnessBreak >= Config.Combat.SHARDS_LOST_PERCENT_TOUGHNESS_RESET * aspects.shards().effectiveMaxValue()) {
                aspects.toughness().setCurPercent(Config.Combat.TOUGHNESS_RECHARGE_PERCENT);
            }
        }

        aspects.soulfire().remove(baseSoulfireReduction);

        for (Affliction affliction : afflictions) {
            affliction.start(this);
        }
    }
}
