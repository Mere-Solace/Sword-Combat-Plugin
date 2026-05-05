package btm.sword.umbral.motion.drivers;

import java.util.Objects;
import java.util.function.Supplier;

import org.bukkit.Location;
import org.bukkit.util.Vector;

import btm.sword.config.Config;
import btm.sword.entity.base.SwordEntity;
import btm.sword.umbral.motion.BladeMotionContext;
import btm.sword.umbral.motion.BladeMotionDriver;
import btm.sword.util.prefab.Prefab;

/**
 * Pins the blade to a host entity, rotating with the host's body or head yaw.
 * <p>
 * Replaces {@code DisplayUtil.itemDisplayFollow}. Used by {@code LodgedState} when the blade is
 * embedded in an entity. The original direction vector supplied at construction is rotated each
 * tick by the delta between the host's yaw at install time and its current yaw, so the blade
 * stays oriented relative to the host's body.
 * <p>
 * Reports {@link Status#DONE DONE} when the host becomes invalid or the externally-supplied
 * {@code exitCondition} returns {@code true}. Visual flair (bleed particles every
 * {@link Config.Display#ITEM_DISPLAY_FOLLOW_PARTICLE_INTERVAL}-th tick) is preserved from the
 * original utility.
 */
public final class ImpalementFollowDriver implements BladeMotionDriver {

    private final SwordEntity host;
    private final Vector initialDirection;
    private final double heightOffset;
    private final boolean followHead;
    private final Supplier<Boolean> exitCondition;
    private final int teleportDuration;

    private double originalYawRadians;

    /**
     * @param host              the entity the blade is embedded in
     * @param initialDirection  the world-space direction the blade is oriented along at install
     * @param heightOffset      vertical offset from the host's foot location
     * @param followHead        {@code true} to align rotation with the host's head yaw,
     *                          {@code false} for body yaw
     * @param exitCondition     external predicate; when it returns {@code true}, report DONE
     *                          (may be {@code null} to keep running until the host dies)
     * @param teleportDuration  smoothing duration in ticks for the teleport interpolation
     */
    public ImpalementFollowDriver(SwordEntity host, Vector initialDirection,
                                  double heightOffset, boolean followHead,
                                  Supplier<Boolean> exitCondition, int teleportDuration) {
        this.host = Objects.requireNonNull(host, "host");
        this.initialDirection = initialDirection.clone();
        this.heightOffset = heightOffset;
        this.followHead = followHead;
        this.exitCondition = exitCondition;
        this.teleportDuration = teleportDuration;
    }

    @Override
    public void onInstall(BladeMotionContext context) {
        if (host.isInvalid()) return;
        originalYawRadians = Math.toRadians(host.self().getBodyYaw());
    }

    @Override
    public Status tick(BladeMotionContext context) {
        if (host.isInvalid() || host.isDead()) return Status.DONE;
        if (exitCondition != null && Boolean.TRUE.equals(exitCondition.get())) return Status.DONE;

        Vector verticalOffset = Config.Direction.up().multiply(heightOffset);
        Location target = host.self().getLocation().add(verticalOffset);

        double currentYawRadians = Math.toRadians(followHead ? host.self().getYaw() : host.self().getBodyYaw());
        Vector currentDirection = initialDirection.clone().rotateAroundY(originalYawRadians - currentYawRadians);

        context.teleportTo(target, currentDirection, teleportDuration);

        if (context.tickCount() % Config.Display.ITEM_DISPLAY_FOLLOW_PARTICLE_INTERVAL == 0) {
            Prefab.Particles.BLEED.display(target);
        }
        return Status.RUNNING;
    }

    @Override
    public void onUninstall(BladeMotionContext context) {}
}
