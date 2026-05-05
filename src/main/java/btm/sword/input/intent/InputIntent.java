package btm.sword.input.intent;

import java.util.UUID;

import javax.annotation.Nullable;

import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

/**
 * Source-agnostic description of a single physical input event.
 *
 * <p>Implementations are immutable records, one per input source, each carrying only the
 * minimum data the {@link InputRouter} needs to make a routing decision. The sealed
 * hierarchy is the contract between the input listener (which translates Bukkit events
 * into intents) and the router (which dispatches them).</p>
 *
 * <h2>Why a sealed hierarchy</h2>
 * <ul>
 *   <li>Each Bukkit event maps to a distinct intent type, so the per-source data
 *       (clicked block, dropped item stack, target hotbar slot) lives only on the
 *       record where it applies — no nullable "context" bag.</li>
 *   <li>The router can use an exhaustive {@code switch} pattern; adding a new intent
 *       record forces the router to be updated at compile time.</li>
 * </ul>
 */
public sealed interface InputIntent {
    /** UUID of the player that produced the input. */
    UUID playerId();

    /** Wall-clock timestamp (milliseconds) at which the listener observed the source event. */
    long observedTimeMs();

    /** Discriminates left- vs right-click {@link Interact} intents. */
    enum Side { LEFT, RIGHT }

    /** Vanilla left-click attack on an entity (pre-attack hook). */
    record Attack(UUID playerId, long observedTimeMs) implements InputIntent {}

    /**
     * Generic player interaction (left- or right-click on air or block).
     *
     * @param block the clicked block or {@code null} for air interactions
     */
    record Interact(UUID playerId, long observedTimeMs, Side side, @Nullable Block block) implements InputIntent {}

    /** Right-click directly targeting an entity. */
    record InteractEntity(UUID playerId, long observedTimeMs) implements InputIntent {}

    /**
     * Player dropped an item stack from the hotbar or inventory.
     *
     * @param droppedStack the item stack being dropped
     */
    record Drop(UUID playerId, long observedTimeMs, ItemStack droppedStack) implements InputIntent {}

    /** Player toggled sneak ON. */
    record SneakBegin(UUID playerId, long observedTimeMs) implements InputIntent {}

    /** Player toggled sneak OFF. */
    record SneakEnd(UUID playerId, long observedTimeMs) implements InputIntent {}

    /** Player pressed the offhand-swap key. */
    record Swap(UUID playerId, long observedTimeMs) implements InputIntent {}

    /**
     * Player changed the held hotbar slot.
     *
     * @param newSlot the new selected slot index
     */
    record HotbarChange(UUID playerId, long observedTimeMs, int newSlot) implements InputIntent {}
}
