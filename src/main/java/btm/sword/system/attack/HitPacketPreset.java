package btm.sword.system.attack;

/**
 * Named, editable hit-packet template stored in the global {@link HitPacketRegistry}.
 *
 * <p>Presets are persisted to {@code plugins/sword/hit-packets.yaml} and selectable from
 * the attack editor. {@link #toHitValuePacket()} wraps the preset's static fields as
 * lambdas to produce a {@link HitValuePacket} usable by the attack system.</p>
 *
 * @param id                 registry key (unique, lowercase identifier)
 * @param displayName        human-readable label shown in menus
 * @param shardDamage        raw shard (HP) damage
 * @param toughnessDamage    raw toughness damage
 * @param soulfireLoss       soulfire drained from the defender
 * @param reapedSoulfire     soulfire transferred from defender to attacker
 * @param invulnerableTicks  invulnerability frames granted after the hit
 * @param blockability       how the hit interacts with a blocking defender
 * @param bypassPower        fraction of damage that bypasses a shield (0–1) for SHIELD_PASSING
 */
public record HitPacketPreset(
    String id,
    String displayName,
    int shardDamage,
    float toughnessDamage,
    float soulfireLoss,
    float reapedSoulfire,
    int invulnerableTicks,
    Blockability blockability,
    float bypassPower
) {

    /**
     * Creates a new preset with neutral defaults for the given id.
     *
     * @param id the registry key to use
     * @return a zeroed preset with display name equal to {@code id}
     */
    public static HitPacketPreset newDefault(String id) {
        return new HitPacketPreset(id, id, 2, 1.0f, 0.0f, 0.0f, 10, Blockability.BLOCKABLE, 0.0f);
    }

    /**
     * Converts this preset into a {@link HitValuePacket} by capturing each numeric
     * field as a {@link java.util.function.Supplier} lambda.
     *
     * @return a new hit-value packet backed by this preset's field values
     */
    public HitValuePacket toHitValuePacket() {
        return new HitValuePacket(
            () -> reapedSoulfire,
            () -> invulnerableTicks,
            () -> shardDamage,
            () -> toughnessDamage,
            () -> soulfireLoss,
            blockability,
            () -> bypassPower
        );
    }

    /** @return a copy of this preset with the given shard damage */
    public HitPacketPreset withShardDamage(int v) {
        return new HitPacketPreset(id, displayName, v, toughnessDamage, soulfireLoss,
            reapedSoulfire, invulnerableTicks, blockability, bypassPower);
    }

    /** @return a copy of this preset with the given toughness damage */
    public HitPacketPreset withToughnessDamage(float v) {
        return new HitPacketPreset(id, displayName, shardDamage, v, soulfireLoss,
            reapedSoulfire, invulnerableTicks, blockability, bypassPower);
    }

    /** @return a copy of this preset with the given soulfire loss */
    public HitPacketPreset withSoulfireLoss(float v) {
        return new HitPacketPreset(id, displayName, shardDamage, toughnessDamage, v,
            reapedSoulfire, invulnerableTicks, blockability, bypassPower);
    }

    /** @return a copy of this preset with the given reaped soulfire */
    public HitPacketPreset withReapedSoulfire(float v) {
        return new HitPacketPreset(id, displayName, shardDamage, toughnessDamage, soulfireLoss,
            v, invulnerableTicks, blockability, bypassPower);
    }

    /** @return a copy of this preset with the given invulnerable ticks */
    public HitPacketPreset withInvulnerableTicks(int v) {
        return new HitPacketPreset(id, displayName, shardDamage, toughnessDamage, soulfireLoss,
            reapedSoulfire, v, blockability, bypassPower);
    }

    /** @return a copy of this preset with the given blockability */
    public HitPacketPreset withBlockability(Blockability v) {
        return new HitPacketPreset(id, displayName, shardDamage, toughnessDamage, soulfireLoss,
            reapedSoulfire, invulnerableTicks, v, bypassPower);
    }

    /** @return a copy of this preset with the given bypass power */
    public HitPacketPreset withBypassPower(float v) {
        return new HitPacketPreset(id, displayName, shardDamage, toughnessDamage, soulfireLoss,
            reapedSoulfire, invulnerableTicks, blockability, v);
    }

    /** @return a copy of this preset with the given display name */
    public HitPacketPreset withDisplayName(String v) {
        return new HitPacketPreset(id, v, shardDamage, toughnessDamage, soulfireLoss,
            reapedSoulfire, invulnerableTicks, blockability, bypassPower);
    }

    /** @return a copy of this preset with the given id */
    public HitPacketPreset withId(String v) {
        return new HitPacketPreset(v, displayName, shardDamage, toughnessDamage, soulfireLoss,
            reapedSoulfire, invulnerableTicks, blockability, bypassPower);
    }

    /**
     * Converts an arbitrary string to a snake_case identifier suitable for use as a preset id.
     * Lowercases, replaces runs of non-alphanumeric characters with a single underscore, and
     * trims leading/trailing underscores.
     *
     * @param input the raw string to slugify
     * @return a snake_case identifier, or an empty string if {@code input} contains no usable chars
     */
    public static String slugify(String input) {
        if (input == null) return "";
        String slug = input.toLowerCase().replaceAll("[^a-z0-9]+", "_");
        slug = slug.replaceAll("^_+|_+$", "");
        return slug;
    }
}
