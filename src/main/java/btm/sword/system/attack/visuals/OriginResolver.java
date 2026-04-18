package btm.sword.system.attack.visuals;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.joml.Vector3f;

import btm.sword.system.attack.simulation.VolumeKeyframe;
import btm.sword.system.entity.SwordEntityArbiter;
import btm.sword.system.entity.base.SwordEntity;

/**
 * Resolves an {@link OriginAnchor} to a world-space {@link Location} against an {@link EffectsContext}.
 *
 * <p>Main-thread only: body-point resolution calls {@link Bukkit#getEntity} and
 * {@link SwordEntityArbiter#getOrAdd} which both require the main thread.</p>
 */
public final class OriginResolver {

    private OriginResolver() {}

    /**
     * Resolves the anchor to a world-space location. Never returns {@code null}:
     * on any lookup failure this falls back to the owning keyframe's world position.
     *
     * @param anchor the anchor to resolve
     * @param ctx    the effects dispatch context
     * @return world-space location of the resolved anchor
     */
    public static Location resolve(OriginAnchor anchor, EffectsContext ctx) {
        return switch (anchor) {
            case OriginAnchor.OwningKeyframe ignored -> resolveKeyframe(ctx.owningKeyframeIndex(), ctx);
            case OriginAnchor.KeyframeIndex ki -> resolveKeyframe(ki.index(), ctx);
            case OriginAnchor.EntityBodyPoint bp -> resolveBodyPoint(bp.point(), ctx);
            case OriginAnchor.FireLockedOrigin ignored -> ctx.lockedOrigin() != null
                ? toLoc(ctx, ctx.lockedOrigin())
                : resolveKeyframe(ctx.owningKeyframeIndex(), ctx);
            case OriginAnchor.RaycastOrigin ignored -> resolveRaycastOrigin(ctx);
        };
    }

    private static Location resolveKeyframe(int index, EffectsContext ctx) {
        List<VolumeKeyframe> kfs = ctx.trajectory().getKeyframes();
        if (kfs.isEmpty()) return new Location(ctx.world(), 0, 0, 0);
        int clamped = Math.max(0, Math.min(index, kfs.size() - 1));
        Vector3f local = new Vector3f(kfs.get(clamped).localPosition());
        Vector3f world = ctx.worldTransform().transformPosition(local);
        return toLoc(ctx, world);
    }

    private static Location resolveBodyPoint(OriginAnchor.BodyPoint point, EffectsContext ctx) {
        Entity entity = Bukkit.getEntity(ctx.attackerId());
        if (!(entity instanceof LivingEntity living)) {
            return resolveKeyframe(ctx.owningKeyframeIndex(), ctx);
        }
        SwordEntity swordEntity = SwordEntityArbiter.getOrAdd(living);
        return switch (point) {
            case EYE -> swordEntity.eyeLoc();
            case CHEST -> swordEntity.getChestLocation();
            case FEET -> living.getLocation();
        };
    }

    private static Location resolveRaycastOrigin(EffectsContext ctx) {
        List<VolumeKeyframe> kfs = ctx.trajectory().getKeyframes();
        if (kfs.isEmpty()) return new Location(ctx.world(), 0, 0, 0);
        int clamped = Math.max(0, Math.min(ctx.owningKeyframeIndex(), kfs.size() - 1));
        VolumeKeyframe kf = kfs.get(clamped);
        // Null fallback: use body-center (0,0,0 in local space) rather than the tip so the
        // line has a non-zero length if localRayOrigin was never recorded.
        Vector3f local = kf.localRayOrigin() != null ? new Vector3f(kf.localRayOrigin()) : new Vector3f();
        Vector3f world = ctx.worldTransform().transformPosition(local);
        return toLoc(ctx, world);
    }

    private static Location toLoc(EffectsContext ctx, Vector3f v) {
        return new Location(ctx.world(), v.x, v.y, v.z);
    }
}
