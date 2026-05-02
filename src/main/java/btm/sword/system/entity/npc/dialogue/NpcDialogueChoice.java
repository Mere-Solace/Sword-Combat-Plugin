package btm.sword.system.entity.npc.dialogue;

import java.util.Objects;

import net.kyori.adventure.text.Component;

/**
 * One selectable option presented on an {@link NpcDialogueNode}.
 * <p>
 * Immutable. The {@link NpcDialogueAction} encodes what happens when the
 * choice is selected; the choice itself carries no behaviour.
 * </p>
 *
 * @param label  text shown on the menu button
 * @param action action invoked when the player selects this choice
 */
public record NpcDialogueChoice(Component label, NpcDialogueAction action) {

    /** Compact constructor enforces non-null fields. */
    public NpcDialogueChoice {
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(action, "action");
    }
}
