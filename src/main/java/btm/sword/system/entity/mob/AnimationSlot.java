package btm.sword.system.entity.mob;

/**
 * A single animation slot: the resolved DEU tag and the animation's duration in ticks.
 * <p>
 * Duration is used as the fallback kill-delay when the {@code AnimationCompleteEvent}
 * does not fire (e.g. if the animation file is missing).
 * </p>
 *
 * @param tag           the full DEU animation tag (e.g. {@code "witha_die"}); empty means no animation
 * @param durationTicks the animation's length in ticks; 0 means unknown / no animation
 */
public record AnimationSlot(String tag, int durationTicks) {

    /** Sentinel for a slot with no animation registered. */
    public static final AnimationSlot NONE = new AnimationSlot("", 0);

    /**
     * Returns {@code true} when this slot has a non-empty animation tag.
     *
     * @return {@code true} if {@code tag} is non-null and non-empty
     */
    public boolean hasTag() {
        return tag != null && !tag.isEmpty();
    }
}
