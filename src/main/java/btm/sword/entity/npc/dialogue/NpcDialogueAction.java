package btm.sword.entity.npc.dialogue;

import java.util.function.BiConsumer;

import btm.sword.entity.npc.NpcEntity;
import btm.sword.entity.npc.menu.NpcMenuKey;
import btm.sword.entity.player.SwordPlayer;

/**
 * Action triggered when a player selects an {@link NpcDialogueChoice}.
 * <p>
 * Sealed hierarchy — exhaustive set of intents the dialogue layer can express.
 * The dialogue controller is the only component permitted to invoke an action;
 * external code constructs an action and hands it to the choice.
 * </p>
 */
public sealed interface NpcDialogueAction
    permits NpcDialogueAction.GoTo,
            NpcDialogueAction.End,
            NpcDialogueAction.OpenMenu,
            NpcDialogueAction.Custom {

    /** Advances the dialogue to the node with the given id. */
    record GoTo(String nodeId) implements NpcDialogueAction {}

    /** Ends the dialogue and clears the player's dialogue state. */
    record End() implements NpcDialogueAction {
        /** Shared singleton — End carries no data. */
        public static final End INSTANCE = new End();
    }

    /** Closes the dialogue and opens the registered NPC menu identified by the key. */
    record OpenMenu(NpcMenuKey key) implements NpcDialogueAction {}

    /**
     * Runs an arbitrary action with the interacting player and NPC as context.
     * The dialogue stays on the current node unless the consumer transitions it
     * via the controller — useful for side effects like granting an item.
     */
    record Custom(BiConsumer<SwordPlayer, NpcEntity> action) implements NpcDialogueAction {}
}
