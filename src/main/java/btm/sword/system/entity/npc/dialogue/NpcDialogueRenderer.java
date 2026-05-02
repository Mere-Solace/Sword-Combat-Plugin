package btm.sword.system.entity.npc.dialogue;

import btm.sword.system.entity.impl.SwordPlayer;
import btm.sword.system.entity.npc.NpcEntity;

/**
 * Translates a dialogue node into a presentation surface for a player.
 * <p>
 * Pure presentation contract — implementations open / refresh an InvUI menu,
 * a chat window, or any other UI without knowing about per-player dialogue
 * state. The {@link NpcDialogueController} drives the renderer and never
 * inspects what it produced.
 * </p>
 */
@FunctionalInterface
public interface NpcDialogueRenderer {

    /**
     * Renders the given dialogue node for the player interacting with the NPC.
     *
     * @param player the player to render the node to
     * @param npc    the NPC speaking
     * @param node   the dialogue node to display
     */
    void render(SwordPlayer player, NpcEntity npc, NpcDialogueNode node);
}
