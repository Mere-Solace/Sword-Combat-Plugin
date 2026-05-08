package btm.sword.config.section;

import static btm.sword.config.Config.register;

import btm.sword.config.Config;

/**
 * Angle constants used throughout the combat system.
 * <p>
 * All angle values are in <b>radians</b> (π = 180°). Used primarily for
 * entity rotation, attack arcs, and visual effects.
 * </p>
 *
 * @see btm.sword.umbral.UmbralBlade Umbral blade rotation behavior
 */
public final class AngleConfig {

    private AngleConfig() {}

    public static float UMBRAL_BLADE_IDLE_PERIOD = (float) Math.PI / 8;

    static {
        register(
            "angle.umbral_blade_idle_period",
            UMBRAL_BLADE_IDLE_PERIOD, Float.class,
            v -> UMBRAL_BLADE_IDLE_PERIOD = v,
            Config::loadFloat
        );
    }
}
