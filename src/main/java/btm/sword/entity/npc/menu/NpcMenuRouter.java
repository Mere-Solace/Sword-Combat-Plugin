package btm.sword.entity.npc.menu;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

import btm.sword.entity.npc.NpcEntity;
import btm.sword.entity.player.SwordPlayer;

/**
 * Registry and opener for {@link NpcMenu} factories keyed by {@link NpcMenuKey}.
 * <p>
 * The router is the single owner of the key-to-factory map. External callers
 * may only register factories ({@link #register(NpcMenuKey, BiFunction)}) and
 * request that a menu be opened ({@link #open(NpcMenuKey, SwordPlayer, NpcEntity)});
 * direct construction of {@link NpcMenu} subclasses is permitted but bypasses
 * the routing layer and should be reserved for trusted callers.
 * </p>
 *
 * <h2>Invariants</h2>
 * <ul>
 *   <li>At most one factory per key — re-registering replaces the prior factory.</li>
 *   <li>Opening an unregistered key throws {@link IllegalArgumentException}.</li>
 * </ul>
 */
public final class NpcMenuRouter {

    private NpcMenuRouter() {}

    private static final Map<NpcMenuKey, BiFunction<SwordPlayer, NpcEntity, NpcMenu>> FACTORIES =
        new ConcurrentHashMap<>();

    /**
     * Registers a factory for the given key. Replaces any prior factory.
     *
     * @param key     the key clients use to request this menu
     * @param factory builds the menu instance for a given player and NPC
     */
    public static void register(NpcMenuKey key, BiFunction<SwordPlayer, NpcEntity, NpcMenu> factory) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(factory, "factory");
        FACTORIES.put(key, factory);
    }

    /**
     * Opens the menu registered under the given key for the player, with NPC context.
     *
     * @param key    the registered key
     * @param player the viewing player
     * @param npc    the NPC providing context
     * @throws IllegalArgumentException if no factory is registered for the key
     */
    public static void open(NpcMenuKey key, SwordPlayer player, NpcEntity npc) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(npc, "npc");
        BiFunction<SwordPlayer, NpcEntity, NpcMenu> factory = FACTORIES.get(key);
        if (factory == null) {
            throw new IllegalArgumentException("no NpcMenu factory registered for key: " + key.name());
        }
        factory.apply(player, npc).open();
    }

    /**
     * Returns {@code true} if a factory is registered for the given key.
     *
     * @param key the key to check
     * @return whether the router can open this key
     */
    public static boolean isRegistered(NpcMenuKey key) {
        return key != null && FACTORIES.containsKey(key);
    }
}
