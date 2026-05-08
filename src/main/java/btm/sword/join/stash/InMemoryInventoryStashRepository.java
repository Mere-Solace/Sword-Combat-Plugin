package btm.sword.join.stash;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.bukkit.inventory.ItemStack;

/**
 * Process-local {@link InventoryStashRepository} backed by a {@link ConcurrentHashMap}.
 *
 * <h2>Persistence</h2>
 * <p>Entries are <b>not</b> persisted across plugin reload or server restart. Any stashed
 * inventories present when the plugin disables are dropped. This is acceptable because
 * the join-waiting phase is short-lived and any player whose snapshot is lost in flight
 * will simply re-enter staging on the next join.</p>
 *
 * <h2>Substitution path</h2>
 * <p>To swap in a durable implementation (JDBC, Redis, etc.) construct that implementation
 * at the same site in {@link btm.sword.Sword#onEnable()} and assign it to the field. No
 * call sites need to change because all consumers use the
 * {@link InventoryStashRepository} interface.</p>
 */
public final class InMemoryInventoryStashRepository implements InventoryStashRepository {

    private final ConcurrentMap<UUID, ItemStack[]> entries = new ConcurrentHashMap<>();

    /** Constructs an empty repository. */
    public InMemoryInventoryStashRepository() {}

    @Override
    public void stash(UUID uuid, ItemStack[] contents) {
        Objects.requireNonNull(uuid, "uuid");
        Objects.requireNonNull(contents, "contents");
        entries.put(uuid, deepCopy(contents));
    }

    @Override
    public Optional<ItemStack[]> consume(UUID uuid) {
        Objects.requireNonNull(uuid, "uuid");
        return Optional.ofNullable(entries.remove(uuid));
    }

    @Override
    public void clear(UUID uuid) {
        Objects.requireNonNull(uuid, "uuid");
        entries.remove(uuid);
    }

    @Override
    public int size() {
        return entries.size();
    }

    private static ItemStack[] deepCopy(ItemStack[] source) {
        ItemStack[] copy = new ItemStack[source.length];
        for (int i = 0; i < source.length; i++) {
            copy[i] = source[i] == null ? null : source[i].clone();
        }
        return copy;
    }
}
