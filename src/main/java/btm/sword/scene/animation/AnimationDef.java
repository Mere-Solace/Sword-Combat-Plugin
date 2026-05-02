package btm.sword.scene.animation;

/**
 * Immutable descriptor for a DEU-backed animation entry loaded from {@code animations.yml}.
 * <p>
 * Each {@code AnimationDef} maps a logical key (used throughout Sword code) to the
 * DEU group tag and animation tag that identify the physical data on disk, plus a
 * default loop flag for when no explicit loop preference is given at playback time.
 * </p>
 *
 * @param key         the unique logical key for this animation (e.g. {@code "slash_test_default"})
 * @param groupTag    the DEU group tag identifying the display-entity group
 * @param animTag     the DEU animation tag identifying the animation data
 * @param defaultLoop whether this animation loops by default
 */
public record AnimationDef(
    String key,
    String groupTag,
    String animTag,
    boolean defaultLoop
) {}
