package btm.sword.system.attack;

import java.util.Set;
import java.util.UUID;

import btm.sword.system.attack.def.AttackInstance;

/**
 * Represents an {@link AttackInstance}-driven attack that is currently active for a combatant.
 *
 * <p>Holds the game-layer view of an in-progress attack: which definition is running,
 * who owns it, when it started, and which entities have already been hit.
 * The simulation-side counterpart ({@link btm.sword.system.attack.simulation.SimulationAttack})
 * is registered separately with {@link btm.sword.system.attack.simulation.VolumeSimulation}.</p>
 *
 * <p>{@code hitThisAttack} is shared with the {@code SimulationAttack} so both layers
 * see the same already-hit set without copying.</p>
 *
 * @param def           the attack definition driving this attack
 * @param ownerUuid     UUID of the entity performing the attack
 * @param startTimeMs   wall-clock start time in milliseconds
 * @param hitThisAttack thread-safe set of entity UUIDs hit so far
 */
public record ActiveAttack(
        AttackInstance def,
        UUID ownerUuid,
        long startTimeMs,
        Set<UUID> hitThisAttack) {
}
