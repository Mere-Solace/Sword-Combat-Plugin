package btm.sword.system.entity.npc.dialogue;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import btm.sword.system.entity.impl.SwordPlayer;
import btm.sword.system.entity.npc.NpcEntity;
import btm.sword.system.entity.npc.menu.NpcMenuRouter;

/**
 * Owns per-player dialogue state for a single {@link NpcEntity}.
 * <p>
 * One controller per NPC. State is keyed by player UUID and is created lazily
 * when a player begins a dialogue and removed when they end it.
 * </p>
 *
 * <h2>State ownership</h2>
 * The controller is the SOLE writer of dialogue state. External code may only
 * express intent via {@link #begin(SwordPlayer)} and {@link #select(SwordPlayer, int)};
 * the controller translates intent into node transitions and presentation calls.
 *
 * <h2>Lifecycle</h2>
 * <ul>
 *   <li>{@link #begin(SwordPlayer)} initialises state at the root node and renders it.</li>
 *   <li>{@link #select(SwordPlayer, int)} applies the chosen action and re-renders.</li>
 *   <li>{@link #end(SwordPlayer)} clears state (idempotent).</li>
 *   <li>{@link #endAll()} clears state for every player (called on NPC despawn).</li>
 * </ul>
 *
 * <h2>Invariants</h2>
 * <ul>
 *   <li>If a player has dialogue state then their current node id is valid.</li>
 *   <li>{@link #end(SwordPlayer)} on an absent player is a no-op (idempotent).</li>
 * </ul>
 */
public final class NpcDialogueController {

    private final NpcDialogueDefinition definition;
    private final NpcDialogueRenderer renderer;
    private final Map<UUID, String> currentNodeByPlayer = new HashMap<>();

    /**
     * Constructs a controller for the given dialogue definition using the supplied renderer.
     *
     * @param definition the immutable dialogue tree
     * @param renderer   renders a node for the player; supplied by the dialogue layer
     */
    public NpcDialogueController(NpcDialogueDefinition definition, NpcDialogueRenderer renderer) {
        this.definition = Objects.requireNonNull(definition, "definition");
        this.renderer = Objects.requireNonNull(renderer, "renderer");
    }

    /**
     * Begins (or restarts) the dialogue for the given player, rendering the root node.
     * Idempotent — calling repeatedly resets the player to the root.
     *
     * @param player the player initiating the dialogue
     * @param npc    the NPC owning this controller
     */
    public void begin(SwordPlayer player, NpcEntity npc) {
        currentNodeByPlayer.put(player.getUniqueId(), definition.rootNodeId());
        renderer.render(player, npc, definition.node(definition.rootNodeId()));
    }

    /**
     * Applies the choice at the given index on the player's current node.
     * <p>
     * No-op if the player has no active dialogue state, or if the index is out of range.
     * </p>
     *
     * @param player      the player making the selection
     * @param npc         the NPC owning this controller
     * @param choiceIndex zero-based index into the current node's choice list
     */
    public void select(SwordPlayer player, NpcEntity npc, int choiceIndex) {
        String currentId = currentNodeByPlayer.get(player.getUniqueId());
        if (currentId == null) return;

        NpcDialogueNode current = definition.node(currentId);
        if (choiceIndex < 0 || choiceIndex >= current.choices().size()) return;

        NpcDialogueAction action = current.choices().get(choiceIndex).action();
        applyAction(player, npc, action);
    }

    /**
     * Ends the dialogue for the given player and clears any state.
     * Idempotent.
     *
     * @param player the player whose dialogue should end
     */
    public void end(SwordPlayer player) {
        currentNodeByPlayer.remove(player.getUniqueId());
    }

    /**
     * Clears dialogue state for every player. Called from {@link NpcEntity#onDeath()}
     * to guarantee no stale per-player state survives the NPC's destruction.
     */
    public void endAll() {
        currentNodeByPlayer.clear();
    }

    /**
     * Returns the player's current node id, or {@code null} if the player has no
     * active dialogue with this NPC.
     */
    public String currentNodeId(SwordPlayer player) {
        return currentNodeByPlayer.get(player.getUniqueId());
    }

    private void applyAction(SwordPlayer player, NpcEntity npc, NpcDialogueAction action) {
        switch (action) {
            case NpcDialogueAction.GoTo go -> {
                NpcDialogueNode next = definition.node(go.nodeId());
                currentNodeByPlayer.put(player.getUniqueId(), next.id());
                renderer.render(player, npc, next);
            }
            case NpcDialogueAction.End ignored -> end(player);
            case NpcDialogueAction.OpenMenu open -> {
                end(player);
                NpcMenuRouter.open(open.key(), player, npc);
            }
            case NpcDialogueAction.Custom custom -> custom.action().accept(player, npc);
        }
    }
}
