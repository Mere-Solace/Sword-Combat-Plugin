package btm.sword.join.stash;

import java.util.Optional;
import java.util.UUID;

import org.bukkit.inventory.ItemStack;

/**
 * Storage abstraction for inventory snapshots taken when a player enters the join-waiting
 * phase and replaced with placeholder items.
 *
 * <h2>Purpose</h2>
 * <p>When a player enters {@link btm.sword.input.trie.ActivationContext#WAITING} their
 * inventory must be cleared and refilled with placeholder items. The original contents
 * are stashed here so they are not lost in flight if the player quits before the routing
 * decision lands them in their next loadout.</p>
 *
 * <h2>State ownership</h2>
 * <p>Implementations of this interface own the stashed snapshots. No other component may
 * read or mutate the underlying storage. All access is through the three methods on this
 * interface.</p>
 *
 * <h2>Lifecycle</h2>
 * <p>For a single player the lifecycle is:</p>
 * <ol>
 *   <li>{@link #stash(UUID, ItemStack[]) stash} — on enter WAITING</li>
 *   <li>{@link #consume(UUID) consume} or {@link #clear(UUID) clear} — on exit (terminate
 *       restores via consume; transition to ACTIVE drops via clear)</li>
 * </ol>
 *
 * <h2>Implementations</h2>
 * <ul>
 *   <li>{@link InMemoryInventoryStashRepository} — process-local, lost on plugin
 *       restart. Currently the only implementation.</li>
 *   <li>A future JDBC implementation can be substituted at the
 *       {@link btm.sword.Sword#onEnable()} construction site without touching call sites.</li>
 * </ul>
 *
 * <h2>Thread safety</h2>
 * <p>Implementations must be safe for concurrent calls from any combination of the Bukkit
 * main thread and the asynchronous quit-handler path.</p>
 */
public interface InventoryStashRepository {

    /**
     * Saves a defensive copy of {@code contents} keyed by {@code uuid}, replacing any
     * existing entry for that key.
     *
     * <p>Implementations must clone the array (and ideally each non-null item) so later
     * mutations of the caller-side array cannot affect the stored snapshot.</p>
     *
     * @param uuid     player UUID; never null
     * @param contents inventory contents to snapshot; never null. Individual slots may be null
     *                 to represent empty slots
     */
    void stash(UUID uuid, ItemStack[] contents);

    /**
     * Atomically retrieves and removes the stashed contents for {@code uuid}.
     *
     * <p>After a successful call no stash exists for that UUID — subsequent calls return
     * {@link Optional#empty()} until a new {@link #stash} is performed.</p>
     *
     * @param uuid player UUID; never null
     * @return the stashed contents, or {@link Optional#empty()} if nothing was stashed
     */
    Optional<ItemStack[]> consume(UUID uuid);

    /**
     * Discards the stash for {@code uuid} without retrieval. No-op if nothing is stashed.
     *
     * @param uuid player UUID; never null
     */
    void clear(UUID uuid);

    /**
     * Returns the number of entries currently held. Intended for diagnostics and tests
     * — production code should not branch on this value.
     *
     * @return the current entry count
     */
    int size();
}
