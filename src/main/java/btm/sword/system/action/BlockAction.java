package btm.sword.system.action;

import org.bukkit.util.Vector;

import btm.sword.config.Config;
import btm.sword.system.attack.Blockability;
import btm.sword.system.attack.HitValuePacket;
import btm.sword.system.entity.base.SwordEntity;
import btm.sword.system.entity.impl.Combatant;
import btm.sword.system.entity.impl.SwordPlayer;
import btm.sword.utility.Prefab;
import btm.sword.utility.sound.SoundUtil;
import btm.sword.utility.sound.SwordSoundType;

/**
 * Handles block and parry resolution for defending {@link SwordPlayer} combatants.
 * <p>
 * Called from {@link SwordEntity#hit} when the defender is blocking. Determines whether
 * the incoming hit is negated (block), perfectly countered (parry), or reduced
 * (shield-passing), then applies the appropriate stat changes and feedback.
 * </p>
 *
 * <h2>Block flow</h2>
 * <ol>
 *   <li>RIGHT pressed → {@link #startBlock} sets blocking and starts soulfire drain</li>
 *   <li>SWAP pressed within the trie timeout ({@link Config.Combat#PARRY_AVAILABLE_MS}) → {@link #parryAttempt}
 *       ends the block, opens hit-detection window, and puts the shield on cooldown</li>
 *   <li>Incoming hit during parry window → {@link #resolveBlock} applies parry or block result</li>
 * </ol>
 */
public final class BlockAction {

    private BlockAction() {}

    /** Result of a block resolution attempt. */
    public enum BlockResult {
        /** Hit was fully negated — defender blocked in time, attack was BLOCKABLE. */
        BLOCKED,
        /** Hit was parried — defender was in the parry window, attack was BLOCKABLE. */
        PARRIED,
        /** Hit bypassed the shield — attack is SHIELD_PASSING; caller applies scaled damage. */
        SHIELD_PASSED,
        /** Block did not apply (non-frontal hit, or no applicable blockability). */
        NOT_BLOCKED
    }

    /**
     * Resolves an incoming hit against a blocking defender.
     * <p>
     * Checks frontal alignment first. BLOCKABLE hits are either parried (if parry window is
     * active) or blocked. SHIELD_PASSING hits always return {@link BlockResult#SHIELD_PASSED}
     * so the caller can apply scaled damage.
     * </p>
     *
     * @param source   the attacking combatant
     * @param defender the blocking player
     * @param packet   the incoming hit packet
     * @return the block resolution result
     */
    public static BlockResult resolveBlock(Combatant source, SwordPlayer defender, HitValuePacket packet) {
        if (!isFrontalHit(source, defender)) {
            return BlockResult.NOT_BLOCKED;
        }

        if (packet.blockability() == Blockability.BLOCKABLE) {
            if (defender.isInParryWindow()) {
                applyParrySuccess(defender, source);
                return BlockResult.PARRIED;
            } else {
                applyBlockSuccess(defender);
                return BlockResult.BLOCKED;
            }
        } else if (packet.blockability() == Blockability.SHIELD_PASSING) {
            return BlockResult.SHIELD_PASSED;
        }

        return BlockResult.NOT_BLOCKED;
    }

    /**
     * Begins a block for the given player. Sets blocking state, marks the parry-available
     * end timestamp, and starts the soulfire drain ticker.
     * <p>
     * Called by the {@code RIGHT} trie node action in {@code InputRegistrar} when the
     * player has a shield in their offhand and is in a normal action state.
     * </p>
     *
     * @param player the player beginning to block
     */
    public static void startBlock(SwordPlayer player) {
        player.setBlocking(true);
        player.startBlockDrain();
    }

    /**
     * Executes a parry attempt: opens the hit-detection window, ends the sustained block,
     * and puts the shield on cooldown so the player cannot re-raise it immediately.
     * <p>
     * Called by the {@code RIGHT → SWAP} trie path while the parry window (trie timeout)
     * is still open. Soulfire drain self-cancels because blocking is set to false.
     * </p>
     *
     * @param player the blocking player who triggered the parry
     */
    public static void parryAttempt(SwordPlayer player) {
        player.setParryWindowEnd(System.currentTimeMillis() + Config.Combat.PARRY_WINDOW_MS);
        player.setBlocking(false);
        player.disableShield(Config.Combat.PARRY_SHIELD_COOLDOWN_TICKS);

        Prefab.Sounds.PARRY_ATTEMPT.playForAllInRadius(player.getLocation(), 15);
    }

    /**
     * Returns a {@link Vector} with shard, toughness, and soulfire values scaled by
     * the packet's {@link HitValuePacket#bypassPower()}, for use when applying shield-passing damage.
     * <p>
     * Convenience used by {@link SwordEntity#hit} to avoid duplicating math.
     * </p>
     *
     * @param packet the original hit packet
     * @return a new HitValuePacket with damage values multiplied by bypassPower
     */
    public static HitValuePacket applyBypassScale(HitValuePacket packet) {
        return new HitValuePacket(
            () -> 0f,
            packet::invulnerableTicks,
            () -> Math.max(1, (int) (packet.shardDamage() * packet.bypassPower())),
            () -> packet.toughnessDamage() * packet.bypassPower(),
            () -> packet.soulfireLoss() * packet.bypassPower(),
            Blockability.SHIELD_PASSING,
            packet::bypassPower
        );
    }

    /**
     * Applies feedback for a successful block (non-parry): drains soulfire and plays
     * the block sound.
     *
     * @param defender the player who blocked
     */
    private static void applyBlockSuccess(SwordPlayer defender) {
        defender.getAspects().soulfire().remove(Config.Combat.BLOCK_SOULFIRE_COST_ON_HIT);
        SoundUtil.playSound(defender.self(), SwordSoundType.ITEM_SHIELD_BLOCK,
            1.0f, 0.9f);

        if (defender.getAspects().soulfire().cur() <= 0) {
            onBlockBroken(defender);
        }
    }

    /**
     * Applies feedback for a successful parry: grants soulfire to the defender and
     * locks the attacker with a cast-duration stagger.
     *
     * @param defender the player who parried
     * @param attacker the combatant whose hit was parried
     */
    private static void applyParrySuccess(SwordPlayer defender, Combatant attacker) {
        defender.getAspects().soulfire().add(Config.Combat.PARRY_SOULFIRE_GAIN);
        SoundUtil.playSound(defender.self(), SwordSoundType.ITEM_SHIELD_BLOCK,
            1.5f, 2.0f);
        ActionCaster.cast(attacker, Config.Combat.PARRY_STAGGER_MS, () -> {});
    }

    /**
     * Returns true if the incoming attack originates from within the defender's frontal arc
     * (within 45° to either side of the direction they are facing).
     *
     * @param source   the attacker
     * @param defender the blocking player
     * @return true if the hit is frontal
     */
    private static boolean isFrontalHit(Combatant source, SwordPlayer defender) {
        Vector toAttacker = source.getLocation().toVector()
            .subtract(defender.getLocation().toVector())
            .setY(0);
        if (toAttacker.lengthSquared() < 1e-6) return true;
        toAttacker.normalize();
        Vector facing = defender.getFlatDir();
        // dot > cos(135°) means attacker is within the frontal 270° arc (45° behind cutoff)
        return facing.dot(toAttacker) > Math.cos(Math.toRadians(135));
    }

    /**
     * Forcibly breaks the block when the defender's soulfire hits zero while blocking.
     * Clears block state and applies a stagger (cast-duration lock) to the defender.
     *
     * @param player the player whose block was broken
     */
    public static void onBlockBroken(SwordPlayer player) {
        player.disableShield(Config.Combat.EXHAUSTED_BLOCKING_COOLDOWN_TICKS);
        player.setBlocking(false);
        player.cancelBlockDrainTask();

        Prefab.Sounds.BLOCK_BROKEN.playForAllInRadius(player.getLocation(), 15);

        ActionCaster.cast(player, Config.Combat.BLOCK_BREAK_STAGGER_MS, () -> {});
    }
}
