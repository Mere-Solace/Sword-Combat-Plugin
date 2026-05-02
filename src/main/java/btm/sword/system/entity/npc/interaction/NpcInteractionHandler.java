package btm.sword.system.entity.npc.interaction;

import btm.sword.input.InputType;
import btm.sword.system.entity.impl.SwordPlayer;
import btm.sword.system.entity.npc.NpcEntity;

/**
 * Behavioural strategy for an {@link NpcEntity}.
 * <p>
 * Receives intent from the input layer and translates it into NPC behaviour
 * (open a dialogue, open a menu, etc.). Handlers are stateless or maintain
 * their own state on the {@link NpcEntity} via dialogue controllers — they
 * MUST NOT manage Bukkit lifecycle, the wrapped {@link org.bukkit.entity.LivingEntity},
 * or any state owned by {@link NpcEntity} itself.
 * </p>
 */
@FunctionalInterface
public interface NpcInteractionHandler {

    /**
     * Handles a player interaction with the given NPC.
     *
     * @param interactor the player who initiated the interaction
     * @param npc        the NPC being interacted with
     * @param input      the input type (typically {@link InputType#RIGHT})
     */
    void handle(SwordPlayer interactor, NpcEntity npc, InputType input);
}
