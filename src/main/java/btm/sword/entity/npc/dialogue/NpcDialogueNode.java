package btm.sword.entity.npc.dialogue;

import java.util.List;
import java.util.Objects;

import net.kyori.adventure.text.Component;

/**
 * One node in a dialogue tree.
 * <p>
 * Immutable. A node has a stable id (used by {@link NpcDialogueAction.GoTo}),
 * one or more lines of speaker text, and zero or more choices. A node with no
 * choices is a terminal node — the dialogue layer renders it with an implicit
 * {@code End} option so the player can always close the dialogue.
 * </p>
 *
 * @param id      unique identifier within the parent {@link NpcDialogueDefinition}
 * @param lines   speaker text shown above the choices
 * @param choices selectable options (may be empty)
 */
public record NpcDialogueNode(String id, List<Component> lines, List<NpcDialogueChoice> choices) {

    /** Compact constructor validates fields and freezes the input lists. */
    public NpcDialogueNode {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(lines, "lines");
        Objects.requireNonNull(choices, "choices");
        if (id.isBlank()) {
            throw new IllegalArgumentException("dialogue node id must not be blank");
        }
        lines = List.copyOf(lines);
        choices = List.copyOf(choices);
    }
}
