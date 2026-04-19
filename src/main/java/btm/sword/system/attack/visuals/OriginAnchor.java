package btm.sword.system.attack.visuals;

/**
 * Resolvable origin for a {@link ParticleDisplay}. Each variant describes a different
 * source of world-space position — the owning keyframe, another keyframe by index, a
 * point on the attacker's body, or the origin captured when the attack was fired.
 *
 * <p>Resolved by {@link OriginResolver} against an {@link EffectsContext} at dispatch time.</p>
 *
 * <p>The {@link RaycastOrigin} variant is only meaningful for {@link btm.sword.system.attack.simulation.KeyframeType#RAYCAST}
 * keyframes — it resolves to the local-space start of the cast ray rather than the tip.</p>
 */
public sealed interface OriginAnchor {

    /** Anchor to the keyframe that owns this display. This is the default. */
    record OwningKeyframe() implements OriginAnchor {
        /** Shared singleton — all OwningKeyframe anchors are equivalent. */
        public static final OwningKeyframe INSTANCE = new OwningKeyframe();
    }

    /** Anchor to another keyframe in the same trajectory, by zero-based index. */
    record KeyframeIndex(int index) implements OriginAnchor {}

    /** Body point on the attacker — eye, chest, or feet. */
    record EntityBodyPoint(BodyPoint point) implements OriginAnchor {}

    /** Origin captured at attack-fire time. Falls back to the owning keyframe if unlocked. */
    record FireLockedOrigin() implements OriginAnchor {
        /** Shared singleton — all FireLockedOrigin anchors are equivalent. */
        public static final FireLockedOrigin INSTANCE = new FireLockedOrigin();
    }

    /**
     * Resolves to the ray-cast origin ({@code localRayOrigin}) of the owning RAYCAST keyframe.
     * Falls back to the owning keyframe tip position for non-RAYCAST keyframes.
     */
    record RaycastOrigin() implements OriginAnchor {
        /** Shared singleton — all RaycastOrigin anchors are equivalent. */
        public static final RaycastOrigin INSTANCE = new RaycastOrigin();
    }

    /**
     * Resolves to the keyframe {@code offset} positions after the owning keyframe.
     * Clamped to the last keyframe when the computed index exceeds the trajectory length.
     */
    record NextKeyframe(int offset) implements OriginAnchor {}

    /** Points on the attacker's body that an anchor can reference. */
    enum BodyPoint { EYE, CHEST, FEET }

    /** Convenience factory: owning keyframe anchor. */
    static OriginAnchor owning() { return OwningKeyframe.INSTANCE; }

    /** Convenience factory: another-keyframe anchor. */
    static OriginAnchor keyframe(int index) { return new KeyframeIndex(index); }

    /** Convenience factory: body-point anchor. */
    static OriginAnchor body(BodyPoint point) { return new EntityBodyPoint(point); }

    /** Convenience factory: fire-locked origin anchor. */
    static OriginAnchor fireLocked() { return FireLockedOrigin.INSTANCE; }

    /** Convenience factory: raycast-origin anchor (start of ray on RAYCAST keyframes). */
    static OriginAnchor raycastOrigin() { return RaycastOrigin.INSTANCE; }

    /** Convenience factory: nth-next-keyframe anchor. */
    static OriginAnchor nextKeyframe(int offset) { return new NextKeyframe(Math.max(1, offset)); }
}
