package btm.sword.config.section;

import static btm.sword.config.Config.register;

import org.bukkit.configuration.ConfigurationSection;

/**
 * Animation configuration. Each named string entry maps a logical slot (e.g. {@code "main_menu"})
 * to an animation key registered in {@code animations.yml}. The icon controls the section button
 * in the config browser.
 */
public final class AnimationConfig {

    private AnimationConfig() {}

    /** Timeout in seconds for each step of DEU animation conversion. */
    public static int ANIMATION_CONVERSION_STEP_TIMEOUT_SECONDS = 120;

    /**
     * Animation key for the static menu scene.
     * <p>
     * Must be the <b>anim key</b> — the sub-entry key under {@code anims:} in
     * {@code animations.yml} (e.g. {@code "gentle_v2_default"}), NOT the group/section key
     * ({@code "gentle_v2"}). {@link btm.sword.scene.animation.AnimationRegistry#get}
     * looks up by this key; the resulting AnimationDef then supplies both
     * {@code groupTag} and {@code animTag} to the DEU controller.
     * </p>
     */
    public static String STATIC_MENU_ANIMATION_KEY = "gentle_v2_default";

    static {
        register("animation.conversion_step_timeout_seconds",
            ANIMATION_CONVERSION_STEP_TIMEOUT_SECONDS, Integer.class,
            v -> ANIMATION_CONVERSION_STEP_TIMEOUT_SECONDS = v,
            ConfigurationSection::getInt);
        register("animation.static_menu_animation_key",
            STATIC_MENU_ANIMATION_KEY, String.class,
            v -> STATIC_MENU_ANIMATION_KEY = v,
            ConfigurationSection::getString);
    }
}
