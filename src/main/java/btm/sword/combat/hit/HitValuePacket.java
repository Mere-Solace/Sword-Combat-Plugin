package btm.sword.combat.hit;

import java.util.function.Supplier;

import btm.sword.combat.attack.Blockability;

/**
 * Encapsulates all damage-related values for a single hit, using suppliers so that
 * hot-reloaded config values are always read fresh at hit time.
 * <p>
 * {@link #blockability} controls how this hit interacts with a blocking defender.
 * For {@link Blockability#SHIELD_PASSING} hits, {@link #bypassPower()} (0–1) is
 * applied as a damage multiplier — 1.0 means full damage passes through, 0.5 means
 * half.
 * </p>
 */
public class HitValuePacket {
    private final Supplier<Float> reapedSoulfire;
    private final Supplier<Integer> invulnerableTicks;
    private final Supplier<Integer> shardDamage;
    private final Supplier<Float> toughnessDamage;
    private final Supplier<Float> soulfireLoss;
    private final Blockability blockability;
    private final Supplier<Float> bypassPower;

    /**
     * Full constructor with explicit blockability classification.
     *
     * @param reapedSoulfire    soulfire transferred from defender to attacker on hit
     * @param invulnerableTicks invulnerability frames granted after the hit
     * @param shardDamage       raw shard (HP) damage
     * @param toughnessDamage   raw toughness damage
     * @param soulfireLoss      soulfire drained from the defender
     * @param blockability      how the hit interacts with a blocking defender
     * @param bypassPower       fraction of damage that passes through for SHIELD_PASSING (0–1);
     *                          evaluated fresh on each hit so config hot-reload takes effect
     */
    public HitValuePacket(
        Supplier<Float> reapedSoulfire,
        Supplier<Integer> invulnerableTicks,
        Supplier<Integer> shardDamage,
        Supplier<Float> toughnessDamage,
        Supplier<Float> soulfireLoss,
        Blockability blockability,
        Supplier<Float> bypassPower) {
        this.reapedSoulfire = reapedSoulfire;
        this.invulnerableTicks = invulnerableTicks;
        this.shardDamage = shardDamage;
        this.toughnessDamage = toughnessDamage;
        this.soulfireLoss = soulfireLoss;
        this.blockability = blockability;
        this.bypassPower = bypassPower;
    }

    /**
     * Convenience constructor that defaults to {@link Blockability#BLOCKABLE} with no bypass power.
     *
     * @param reapedSoulfire   soulfire transferred from defender to attacker on hit
     * @param invulnerableTicks invulnerability frames granted after the hit
     * @param shardDamage      raw shard (HP) damage
     * @param toughnessDamage  raw toughness damage
     * @param soulfireLoss     soulfire drained from the defender
     */
    public HitValuePacket(
        Supplier<Float> reapedSoulfire,
        Supplier<Integer> invulnerableTicks,
        Supplier<Integer> shardDamage,
        Supplier<Float> toughnessDamage,
        Supplier<Float> soulfireLoss) {
        this(reapedSoulfire, invulnerableTicks, shardDamage, toughnessDamage, soulfireLoss,
            Blockability.BLOCKABLE, () -> 0f);
    }

    /** @return soulfire transferred from defender to attacker on hit */
    public float reapedSoulfire() {
        return reapedSoulfire.get();
    }

    /** @return invulnerability frames granted after the hit */
    public int invulnerableTicks() {
        return invulnerableTicks.get();
    }

    /** @return raw shard (HP) damage */
    public int shardDamage() {
        return shardDamage.get();
    }

    /** @return raw toughness damage */
    public float toughnessDamage() {
        return toughnessDamage.get();
    }

    /** @return soulfire drained from the defender */
    public float soulfireLoss() {
        return soulfireLoss.get();
    }

    /** @return how this hit interacts with a blocking defender */
    public Blockability blockability() {
        return blockability;
    }

    /**
     * Fraction of damage that passes through a shield for {@link Blockability#SHIELD_PASSING} hits.
     * Range 0–1: 1.0 = full damage, 0.0 = no damage.
     * Evaluated fresh each call so config hot-reload takes effect.
     *
     * @return bypass power fraction
     */
    public float bypassPower() {
        return bypassPower.get();
    }
}
