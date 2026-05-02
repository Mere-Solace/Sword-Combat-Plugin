package btm.sword.entity.npc;

import java.util.Objects;

import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import btm.sword.entity.base.CombatProfile;
import btm.sword.entity.base.SwordEntity;
import btm.sword.entity.npc.dialogue.NpcDialogueController;
import btm.sword.entity.npc.interaction.NpcInteractionHandler;
import btm.sword.entity.team.SwordTeam;
import btm.sword.input.InputType;
import lombok.Getter;

/**
 * Non-combat scripted entity wrapping a Bukkit {@link LivingEntity}.
 * <p>
 * NpcEntity is the existence and lifecycle layer for NPCs. It carries no combat
 * mechanics ({@link btm.sword.entity.base.Combatant} is intentionally NOT
 * an ancestor) — interactions are dispatched to a fixed
 * {@link NpcInteractionHandler} supplied at spawn time, which is the only
 * component that decides what an interaction means.
 * </p>
 *
 * <h2>State ownership</h2>
 * <ul>
 *   <li>{@link #handler} — set at construction, never reassigned.</li>
 *   <li>{@link #dialogueController} — optional, set once at construction; controller
 *       owns its own internal per-player dialogue state.</li>
 *   <li>{@link #displayName} — owned by {@link SwordEntity}; mutable through {@link SwordEntity}'s setter.</li>
 * </ul>
 *
 * <h2>Lifecycle</h2>
 * <pre>
 *   created (private ctor) ──spawned by NpcSpawner──▶ ACTIVE
 *   ACTIVE ──onDeath()──▶ DESPAWNED   (terminal — destroyed flag set, registry entry removed)
 * </pre>
 *
 * <h2>Invariants</h2>
 * <ul>
 *   <li>Construction is package-private — {@link NpcSpawner} is the only legal creator.</li>
 *   <li>{@code handler} is non-null for the lifetime of the NPC.</li>
 *   <li>{@code NpcRegistry} contains this entity iff it has been registered and not yet despawned.</li>
 * </ul>
 */
public final class NpcEntity extends SwordEntity {

    @Getter
    private final NpcType npcType;

    @Getter
    private final NpcInteractionHandler handler;

    @Getter
    @Nullable
    private final NpcDialogueController dialogueController;

    /**
     * Package-private constructor — invoked exclusively by {@link NpcSpawner}.
     *
     * @param self               the wrapped Bukkit living entity
     * @param profile            combat profile (unused for behaviour, required by {@link SwordEntity})
     * @param npcType            metadata archetype
     * @param handler            interaction strategy
     * @param dialogueController optional dialogue controller (may be {@code null})
     */
    NpcEntity(LivingEntity self, CombatProfile profile, NpcType npcType,
              NpcInteractionHandler handler,
              @Nullable NpcDialogueController dialogueController) {
        super(self, profile);
        this.npcType = Objects.requireNonNull(npcType, "npcType");
        this.handler = Objects.requireNonNull(handler, "handler");
        this.dialogueController = dialogueController;
    }

    @Override
    public void onSpawn() {
        super.onSpawn();
        joinTeam(SwordTeam.GREEN);
    }

    @Override
    public void onDeath() {
        super.onDeath();
        if (dialogueController != null) {
            dialogueController.endAll();
        }
//        NpcRegistry.unregister(this);
    }

    /**
     * Forwards an external player input to this NPC's {@link NpcInteractionHandler}.
     * <p>
     * The single API external systems use to express interaction intent. The handler
     * decides what to do (open dialogue, open menu, ignore, etc.). External callers
     * MUST NOT inspect or mutate {@link #dialogueController} directly — they go
     * through this method.
     * </p>
     *
     * @param interactor the player who interacted
     * @param input      the input received
     */
    public void onInteract(btm.sword.entity.player.SwordPlayer interactor, InputType input) {
        handler.handle(interactor, this, input);
    }

    /** Returns the human-readable display name shown in dialogue UI and HUD. */
    public String displayName() {
        return getDisplayName();
    }
}
