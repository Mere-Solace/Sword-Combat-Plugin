package btm.sword.system.hud;

/**
 * Immutable snapshot of the vanilla HUD values currently presented to a player.
 *
 * @param health     displayed health value
 * @param food       displayed food value
 * @param saturation displayed saturation value
 * @param air        displayed remaining air value
 */
public record HudRenderState(
    double health,
    int food,
    float saturation,
    int air
) {}
