package btm.sword.combat.attack;

/**
 * Classifies how an attack interacts with a blocking defender.
 * <p>
 * Assigned on every {@link HitValuePacket} to control block and parry resolution
 * in {@link btm.sword.entity.base.SwordEntity#hit}.
 * </p>
 */
public enum Blockability {
    /** Attack is fully negated by a block or parry. */
    BLOCKABLE,

    /**
     * Attack partially bypasses the shield. Damage is scaled by
     * {@link HitValuePacket#bypassPower()} regardless of parry window.
     */
    SHIELD_PASSING
}
