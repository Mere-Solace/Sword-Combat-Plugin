package btm.sword.umbral.motion.drivers;

import org.bukkit.Location;
import org.bukkit.util.Vector;

import btm.sword.entity.base.Combatant;
import btm.sword.umbral.motion.BladeMotionContext;
import btm.sword.umbral.motion.BladeMotionDriver;

/**
 * Holds the blade at a fixed world-space offset from the thrower's location.
 * <p>
 * The offset is in <b>world space</b> — the blade does not rotate with the thrower. Use
 * {@link AnchoredFollowDriver} when the offset should be expressed in the thrower's local
 * basis (right / up / forward) and rotate as the thrower turns.
 * <p>
 * The driver runs indefinitely; the owning state must call {@link btm.sword.umbral.motion.BladeMotion#stop()}
 * or install a different driver to end it.
 */
public final class StaticDriver implements BladeMotionDriver {

    private final Vector worldOffset;
    private final int teleportDuration;

    /**
     * @param worldOffset       the world-space offset added to the thrower's location each tick
     * @param teleportDuration  smoothing duration in ticks for the teleport interpolation
     */
    public StaticDriver(Vector worldOffset, int teleportDuration) {
        this.worldOffset = worldOffset.clone();
        this.teleportDuration = teleportDuration;
    }

    @Override
    public void onInstall(BladeMotionContext context) {}

    @Override
    public Status tick(BladeMotionContext context) {
        Combatant thrower = context.thrower();
        if (thrower == null || thrower.isInvalid()) return Status.DONE;
        Location target = thrower.self().getLocation().add(worldOffset);
        context.teleportTo(target, null, teleportDuration);
        return Status.RUNNING;
    }

    @Override
    public void onUninstall(BladeMotionContext context) {}
}
