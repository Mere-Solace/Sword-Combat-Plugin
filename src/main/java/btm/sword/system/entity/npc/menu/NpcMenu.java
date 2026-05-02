package btm.sword.system.entity.npc.menu;

import java.util.Objects;

import btm.sword.system.entity.impl.SwordPlayer;
import btm.sword.system.entity.npc.NpcEntity;
import btm.sword.system.inventory.menu.Menu;

/**
 * Base class for InvUI menus that need {@link NpcEntity} context.
 * <p>
 * Subclasses receive both the {@link SwordPlayer} viewer and the originating NPC
 * and may inspect either when constructing their GUI. The base only stores the
 * NPC reference — the player is owned by the parent {@link Menu}.
 * </p>
 */
public abstract class NpcMenu extends Menu {

    /** The NPC that opened this menu. Final and non-null. */
    protected final NpcEntity npc;

    /**
     * Constructs an NPC-bound menu for the given player and NPC.
     *
     * @param player the viewing player
     * @param npc    the NPC providing context for this menu
     */
    protected NpcMenu(SwordPlayer player, NpcEntity npc) {
        super(player);
        this.npc = Objects.requireNonNull(npc, "npc");
    }
}
