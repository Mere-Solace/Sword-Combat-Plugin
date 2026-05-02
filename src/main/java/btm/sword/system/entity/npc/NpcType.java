package btm.sword.system.entity.npc;

/**
 * Categorical archetype of an {@link NpcEntity}.
 * <p>
 * The type carries no behaviour — behaviour is supplied by the
 * {@link btm.sword.system.entity.npc.interaction.NpcInteractionHandler} attached at spawn time.
 * The type exists purely as metadata for filtering, debug labelling, and authoring tools.
 * </p>
 */
public enum NpcType {
    /** Generic dialogue NPC with no special role. */
    NEUTRAL,
    /** Lore-bearing NPC; typically opens a dialogue tree on right-click. */
    LORE_KEEPER,
    /** Vendor NPC; opens a shop menu on right-click. */
    VENDOR,
    /** Quest-giving NPC. */
    QUEST_GIVER,
    /** Trainer NPC; opens a skill or stat menu. */
    TRAINER
}
