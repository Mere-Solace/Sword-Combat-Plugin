package btm.sword.system.entity.npc.dialogue;

import java.util.Map;
import java.util.Objects;

/**
 * Immutable description of a complete dialogue tree.
 * <p>
 * A definition is purely declarative — it carries no per-player state. Player
 * progress through the tree is owned by {@link NpcDialogueController}. The
 * same definition may be shared by any number of NPCs.
 * </p>
 *
 * <h2>Structural invariants</h2>
 * <ul>
 *   <li>{@link #rootNodeId()} must reference an entry in {@link #nodes()}.</li>
 *   <li>Every {@link NpcDialogueAction.GoTo} target referenced by a choice must
 *       reference an entry in {@link #nodes()}.</li>
 * </ul>
 * Violations throw {@link IllegalArgumentException} from {@link #validate()},
 * called automatically from the compact constructor.
 *
 * @param rootNodeId the id of the entry node shown when a dialogue begins
 * @param nodes      all nodes in the tree, keyed by id
 */
public record NpcDialogueDefinition(String rootNodeId, Map<String, NpcDialogueNode> nodes) {

    /** Compact constructor copies the map and validates structural invariants. */
    public NpcDialogueDefinition {
        Objects.requireNonNull(rootNodeId, "rootNodeId");
        Objects.requireNonNull(nodes, "nodes");
        nodes = Map.copyOf(nodes);
        validate(rootNodeId, nodes);
    }

    /**
     * Returns the {@link NpcDialogueNode} for the given id.
     *
     * @param nodeId the id to resolve
     * @return the node, never {@code null}
     * @throws IllegalArgumentException if no node with that id exists
     */
    public NpcDialogueNode node(String nodeId) {
        NpcDialogueNode node = nodes.get(nodeId);
        if (node == null) {
            throw new IllegalArgumentException("unknown dialogue node id: " + nodeId);
        }
        return node;
    }

    private static void validate(String rootNodeId, Map<String, NpcDialogueNode> nodes) {
        if (!nodes.containsKey(rootNodeId)) {
            throw new IllegalArgumentException("rootNodeId '" + rootNodeId + "' not present in nodes map");
        }
        for (NpcDialogueNode node : nodes.values()) {
            for (NpcDialogueChoice choice : node.choices()) {
                if (choice.action() instanceof NpcDialogueAction.GoTo goTo
                    && !nodes.containsKey(goTo.nodeId())) {
                    throw new IllegalArgumentException(
                        "node '" + node.id() + "' choice references unknown target '" + goTo.nodeId() + "'");
                }
            }
        }
    }
}
