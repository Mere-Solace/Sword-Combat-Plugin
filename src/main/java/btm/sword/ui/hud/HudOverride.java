package btm.sword.ui.hud;

import org.bukkit.entity.Player;

/**
 * Functional hook for mutating the vanilla HUD values shown to a player.
 */
@FunctionalInterface
public interface HudOverride {

    /**
     * Applies an override to the current render state.
     *
     * @param player the player receiving the HUD
     * @param state  the current HUD state
     * @return the adjusted HUD state
     */
    HudRenderState apply(Player player, HudRenderState state);
}
