package btm.sword.umbral.motion.drivers;

import org.bukkit.Location;
import org.bukkit.util.Vector;

import btm.sword.entity.base.Combatant;
import btm.sword.umbral.motion.BladeMotionContext;
import btm.sword.umbral.motion.BladeMotionDriver;

/**
 * Follows the thrower with a basis-rotated offset, smoothly tracking the thrower's facing.
 * <p>
 * Replaces {@code DisplayUtil.itemDisplayFollowLerp}. The offset is expressed in the thrower's
 * local basis: {@code x} along {@link Combatant#rightBasisVector(boolean) right},
 * {@code y} along {@link Combatant#upBasisVector(boolean) up}, {@code z} along
 * {@link Combatant#forwardBasisVector(boolean) forward}. Each tick the blade is teleported to
 * {@code thrower.location + (right * x + up * y + forward * z)} and oriented to face along
 * the thrower's forward axis.
 * <p>
 * The driver runs indefinitely; the owning state must call {@link btm.sword.umbral.motion.BladeMotion#stop()}
 * or install a different driver to end it.
 */
public final class AnchoredFollowDriver implements BladeMotionDriver {

    private final Vector localOffset;
    private final boolean withPitch;
    private final int teleportDuration;

    /**
     * @param localOffset       the thrower-local offset (right / up / forward components)
     * @param withPitch         {@code true} to use pitch-aware basis vectors, {@code false} for
     *                          horizontal-only follow
     * @param teleportDuration  smoothing duration in ticks for the teleport interpolation
     */
    public AnchoredFollowDriver(Vector localOffset, boolean withPitch, int teleportDuration) {
        this.localOffset = localOffset.clone();
        this.withPitch = withPitch;
        this.teleportDuration = teleportDuration;
    }

    @Override
    public void onInstall(BladeMotionContext context) {}

    @Override
    public Status tick(BladeMotionContext context) {
        Combatant thrower = context.thrower();
        if (thrower == null || thrower.isInvalid()) return Status.DONE;

        Vector worldOffset = thrower.rightBasisVector(withPitch).multiply(localOffset.getX())
            .add(thrower.upBasisVector(withPitch).multiply(localOffset.getY()))
            .add(thrower.forwardBasisVector(withPitch).multiply(localOffset.getZ()));

        Location target = thrower.self().getLocation().add(worldOffset);
        context.teleportTo(target, thrower.forwardBasisVector(withPitch), teleportDuration);
        return Status.RUNNING;
    }

    @Override
    public void onUninstall(BladeMotionContext context) {}
}
