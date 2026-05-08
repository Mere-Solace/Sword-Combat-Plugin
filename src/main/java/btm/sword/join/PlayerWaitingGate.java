package btm.sword.join;

import java.util.List;
import java.util.Objects;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataType;

import btm.sword.entity.player.SwordPlayer;
import btm.sword.input.trie.ActivationContext;
import btm.sword.item.core.ItemStackBuilder;
import btm.sword.item.core.KeyRegistry;
import btm.sword.join.stash.InventoryStashRepository;
import net.kyori.adventure.text.Component;

/**
 * Atomic enter / exit operations for the join-waiting phase.
 *
 * <h2>Responsibility</h2>
 * <p>This gate is the single owner of the player-side state changes that mark a player
 * as <em>waiting</em>. Each call to {@link #engage(SwordPlayer)} performs a complete
 * transition into the waiting phase; each call to {@link #disengage(SwordPlayer, Destination)}
 * performs a complete transition out of it. The gate does not own the lifecycle decision
 * — that lives in {@code JoinSession} — it merely encapsulates <em>how</em> a player
 * is moved into / out of the waiting state so the lifecycle code can call one method.</p>
 *
 * <h2>Idempotency</h2>
 * <p>Both methods are safe to call repeatedly with the same player. {@link #engage} re-stashes
 * over any prior stash for the UUID (the repository contract); the inventory clear and
 * placeholder fill produce the same end state regardless of starting state.
 * {@link #disengage} clears the stash and inventory unconditionally.</p>
 *
 * <h2>State touched</h2>
 * <ul>
 *   <li>{@link InventoryStashRepository} — stash on engage, clear on disengage</li>
 *   <li>{@link Player#getInventory()} — full 41-slot replacement on both transitions</li>
 *   <li>{@link Player#setGameMode(GameMode)} — {@link GameMode#CREATIVE} on engage,
 *       {@link GameMode#SURVIVAL} on disengage</li>
 *   <li>{@link Player#setInvisible(boolean)} — {@code true} on engage, {@code false} on disengage</li>
 *   <li>{@link SwordPlayer#setActivationContext} — {@link ActivationContext#WAITING} on engage,
 *       {@link ActivationContext#NORMAL} on disengage</li>
 *   <li>{@link SwordPlayer#deactivateUmbralBlade} on engage, {@link SwordPlayer#activateUmbralBlade}
 *       on disengage</li>
 * </ul>
 *
 * <h2>Input suppression</h2>
 * <p>Once {@link ActivationContext#WAITING} is set, the structural input gates in
 * {@link btm.sword.input.intent.InputRouter#route} and {@link SwordPlayer#act} suppress every
 * combat / blade / skill / ability / dash / swap / drop / sneak dispatch regardless of
 * gamemode. The creative-mode flip on this transition is for movement and reach behaviour
 * only — not a vector for ability inputs.</p>
 */
public final class PlayerWaitingGate {

    /**
     * Inventory layout constants (vanilla Bukkit ordering).
     *
     * <p>{@link PlayerInventory} holds 41 slots in {@code getContents()}:</p>
     * <ul>
     *   <li>0–8   — hotbar</li>
     *   <li>9–35  — main inventory (3 rows × 9 columns)</li>
     *   <li>36–39 — armour (boots / leggings / chestplate / helmet)</li>
     *   <li>40    — offhand</li>
     * </ul>
     */
    private static final int HOTBAR_BEGIN = 0;
    private static final int HOTBAR_END_EXCLUSIVE = 9;
    private static final int MAIN_BEGIN = 9;
    private static final int MAIN_END_EXCLUSIVE = 36;
    private static final int OFFHAND_SLOT = 40;
    private static final int FULL_INVENTORY_SIZE = 41;

    private final InventoryStashRepository stashRepository;

    /**
     * Constructs a gate bound to the given stash repository.
     *
     * @param stashRepository the repository used to hold inventory snapshots while the
     *                        player is in the waiting phase; never null
     */
    public PlayerWaitingGate(InventoryStashRepository stashRepository) {
        this.stashRepository = Objects.requireNonNull(stashRepository, "stashRepository");
    }

    /**
     * Transitions the given player into the join-waiting phase.
     *
     * <p>Order of operations:</p>
     * <ol>
     *   <li>Suppress {@link SwordPlayer#setAllAnchoredItemUpkeep(boolean) anchored-item upkeep}
     *       so the periodic {@code inventoryUpkeep} tick does not re-populate the main menu
     *       button, ability slots, or storage buttons after we clear them.</li>
     *   <li>Snapshot the player's current 41-slot inventory and stash it under their UUID.</li>
     *   <li>Clear hotbar (0–8), offhand (40), and armour (36–39) to AIR.</li>
     *   <li>Fill main inventory (9–35) with black-stained-glass placeholder panes tagged
     *       with {@link KeyRegistry#WAITING_PLACEHOLDER_KEY}.</li>
     *   <li>Set {@link ActivationContext#WAITING} on the player — this engages the structural
     *       input-dispatch suppression at every chokepoint.</li>
     *   <li>Deactivate the umbral blade so any in-flight blade state terminates and no new
     *       blade can spawn. Nulling the blade reference also short-circuits the umbral-blade
     *       link-anchor branch in {@code inventoryUpkeep}.</li>
     *   <li>Switch gamemode to creative and set invisible.</li>
     * </ol>
     *
     * @param sp the player to gate; never null
     */
    public void engage(SwordPlayer sp) {
        Objects.requireNonNull(sp, "sp");
        Player player = sp.player();
        PlayerInventory inv = player.getInventory();

        // Disable anchored-item upkeep BEFORE clearing the inventory so there is no race
        // window in which the next 5-tick inventoryUpkeep fires between our clear and the
        // suppression toggle and re-restores the special items.
        sp.setAllAnchoredItemUpkeep(false);

        ItemStack[] snapshot = inv.getContents();
        stashRepository.stash(player.getUniqueId(), snapshot);

        ItemStack[] replacement = new ItemStack[FULL_INVENTORY_SIZE];
        ItemStack placeholder = placeholderPane();
        for (int i = MAIN_BEGIN; i < MAIN_END_EXCLUSIVE; i++) {
            replacement[i] = placeholder.clone();
        }
        // hotbar, armour, and offhand intentionally left null → AIR
        inv.setContents(replacement);

        sp.setActivationContext(ActivationContext.WAITING);
        sp.deactivateUmbralBlade();

        player.setGameMode(GameMode.CREATIVE);
        player.setInvisible(true);
    }

    /**
     * Transitions the given player out of the join-waiting phase, on the way to the
     * destination they selected.
     *
     * <p>Order of operations:</p>
     * <ol>
     *   <li>Drop the stashed inventory snapshot — the player does not carry waiting-phase
     *       items into their next destination; they receive a fresh loadout instead.</li>
     *   <li>Clear all 41 inventory slots so no placeholder panes leak into gameplay.</li>
     *   <li>Re-activate the umbral blade and set {@link ActivationContext#NORMAL}.</li>
     *   <li>Restore visibility and switch gamemode to survival.</li>
     *   <li>{@code TODO}: hand the player a destination-specific loadout. The
     *       {@code destination} parameter is accepted now so call sites are stable when the
     *       loadout mapping lands.</li>
     * </ol>
     *
     * @param sp          the player to ungate; never null
     * @param destination the destination the player selected; never null. Currently used only
     *                    for the future loadout dispatch
     */
    public void disengage(SwordPlayer sp, Destination destination) {
        Objects.requireNonNull(sp, "sp");
        Objects.requireNonNull(destination, "destination");
        Player player = sp.player();

        stashRepository.clear(player.getUniqueId());

        player.getInventory().setContents(new ItemStack[FULL_INVENTORY_SIZE]);

        // TODO: fill loadout per destination — pending loadout mapping.

        sp.activateUmbralBlade();
        sp.setActivationContext(ActivationContext.NORMAL);

        // Re-enable anchored-item upkeep — the next inventoryUpkeep tick (≤250ms) will
        // restore the main menu button, ability buttons, and storage buttons into their
        // anchor slots.
        sp.setAllAnchoredItemUpkeep(true);

        player.setInvisible(false);
        player.setGameMode(GameMode.SURVIVAL);
    }

    /**
     * Restores the stashed inventory back onto the player and ungates them <em>without</em>
     * routing — used by the lifecycle owner when a player quits or the session is otherwise
     * terminated before they reached the routing phase.
     *
     * <p>If no stash exists the inventory is simply cleared. Either way the player exits
     * the gate in a defined state: visible, survival, no placeholder panes.</p>
     *
     * @param sp the player to release; never null
     */
    public void abort(SwordPlayer sp) {
        Objects.requireNonNull(sp, "sp");
        Player player = sp.player();

        ItemStack[] restored = stashRepository.consume(player.getUniqueId())
            .orElseGet(() -> new ItemStack[FULL_INVENTORY_SIZE]);
        player.getInventory().setContents(restored);

        sp.activateUmbralBlade();
        sp.setActivationContext(ActivationContext.NORMAL);

        // Re-enable anchored-item upkeep so the player's UI items return to their anchor
        // slots on the next inventoryUpkeep tick.
        sp.setAllAnchoredItemUpkeep(true);

        player.setInvisible(false);
        player.setGameMode(GameMode.SURVIVAL);
    }

    /**
     * Builds a single placeholder pane. Each call produces a fresh {@link ItemStack} so
     * callers can place clones into individual slots without risking shared-state mutation.
     */
    private static ItemStack placeholderPane() {
        return ItemStackBuilder.of(Material.BLACK_STAINED_GLASS_PANE)
            .name(Component.empty())
            .lore(List.of())
            .hideAll()
            .stripAttributeModifiers()
            .tag(KeyRegistry.WAITING_PLACEHOLDER_KEY, PersistentDataType.BYTE, (byte) 1)
            .build();
    }
}
