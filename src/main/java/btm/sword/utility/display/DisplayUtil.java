package btm.sword.utility.display;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Mob;
import org.bukkit.util.Vector;

import btm.sword.config.Config;
import btm.sword.system.control.PredicateRunnablePair;
import btm.sword.system.control.SwordScheduler;
import btm.sword.system.control.TimeArbiter;
import btm.sword.system.entity.base.SwordEntity;
import btm.sword.utility.Prefab;

/**
 * Utility class for manipulating {@link Display} objects in Bukkit.
 * Provides methods to control teleportation animation, interpolation, and drawing geometric lines.
 * <p>
 * Useful for visual effects and particle display management in the Sword plugin.
 * </p>
 */
public final class DisplayUtil {

    private DisplayUtil() {}

    /**
     * Performs a smooth teleport of a {@link Display} over a specified duration in ticks.
     *
     * @param display the {@link Display} to teleport
     * @param duration the duration in ticks for the teleportation animation
     */
    public static void setSmoothTeleportDuration(Display display, int duration) {
        display.setTeleportDuration(Math.clamp(duration, 0, 59)); // <- Bukkit rule: 0 < duration < 59
    }

    /**
     * Sets interpolation delay and duration for a display, affecting the animation of visual effects.
     * Useful for fine-tuning the visual appearance of particle effects or display movements.
     *
     * @param display the {@link Display} to set interpolation parameters on
     * @param delay the delay in ticks before interpolation starts
     * @param duration the duration in ticks of the interpolation
     */
    public static void setInterpolationValues(Display display, int delay, int duration) {
        display.setInterpolationDelay(delay);
        display.setInterpolationDuration(duration);
    }

    /** Smoothly rotates the display toward an offset position using slerp, returning a task handle. */
    public static TimeArbiter.TaskHandle displaySlerpToOffset(
        SwordEntity entity, ItemDisplay display, Vector offset,
        double speed, int tpDuration, int period,
        double endDistance, boolean removeOnArrival,
        int timeoutTicks,
        Supplier<Boolean> contiueDespiteArrivalCondition,
        Supplier<Boolean> endCondition,
        Runnable callback) {

        AtomicInteger ticks = new AtomicInteger(0);
        AtomicReference<Vector> currentDirectionToTarget = new AtomicReference<>();
        return TimeArbiter.runTimeBoundBukkitTaskOnTimer(
            () -> {
                try {
                    Location curTarget = entity.self().getLocation().add(
                        entity.rightBasisVector(false).multiply(offset.getX()).add(
                            entity.upBasisVector(false).multiply(offset.getY()).add(
                                entity.forwardBasisVector(false).multiply(offset.getZ())
                            )
                        )
                    );
                    currentDirectionToTarget.set(curTarget.toVector().subtract(display.getLocation().toVector()));
                } catch (RuntimeException ignored) {}
            },
            () -> {
                Vector scaledDiff = currentDirectionToTarget.get().normalize().multiply(speed);
                Location update = display.getLocation().add(scaledDiff);
                TimeArbiter.teleportDisplay(
                    display,
                    update, update.toVector().subtract(display.getLocation().toVector()),
                    tpDuration,
                    DisplayUtil.class, 148
                );
            },
            null,
            0, period,
            DisplayUtil.class, "displaySlerpToOffset (2)",
            new PredicateRunnablePair(
                () -> entity.isInvalid() || !display.isValid() ||
                    ticks.getAndIncrement() > timeoutTicks || endCondition.get(),
                () -> {
                    if (display.isValid() && removeOnArrival) display.remove();
                    if (callback != null) {
                        SwordScheduler.runBukkitTask(callback);
                    }
                }
            ),
            new PredicateRunnablePair(
                () -> currentDirectionToTarget.get().isZero() ||
                    currentDirectionToTarget.get().lengthSquared() < endDistance * endDistance ||
                    ticks.get() > timeoutTicks || endCondition.get() &&
                    !(contiueDespiteArrivalCondition.get()), // make this condition very predictable
                () -> {
                    if (display.isValid() && removeOnArrival) display.remove();
                    if (callback != null) {
                        SwordScheduler.runBukkitTask(callback);
                    }
                }
            )
        );
    }

    /**
     * Causes an {@link ItemDisplay} entity to follow a {@link SwordEntity} visually,
     * maintaining a specified direction offset and height, with optional following of the entity's head yaw.
     * The display is updated every 2 ticks asynchronously until either entity or display is dead or air.
     *
     * @param entity the SwordEntity to follow
     * @param itemDisplay the {@link ItemDisplay} to move to follow the entity
     * @param direction the direction {@link Vector} offset relative to the entity
     * @param heightOffset vertical height offset from the entity's location
     * @param followHead whether to align the display's direction to the entity's head yaw instead of body yaw
     */
    public static <T> TimeArbiter.TaskHandle itemDisplayFollow(
        SwordEntity entity, ItemDisplay itemDisplay, Vector direction,
        double heightOffset, boolean followHead,
        Predicate<T> condition, T toTest,
        Consumer<T> callback, T toConsume) {

        double originalYaw = Math.toRadians(entity.self().getBodyYaw());
        Vector offset = Config.Direction.up().multiply(heightOffset);

        itemDisplay.setBillboard(Config.Display.ITEM_DISPLAY_FOLLOW_BILLBOARD_MODE);

        AtomicInteger iteration = new AtomicInteger(0);
        return TimeArbiter.runTimeBoundBukkitTaskOnTimer(
            null,
            () -> {
                Location l = entity.self().getLocation().add(offset);

                double yawRads = Math.toRadians(followHead ? entity.self().getYaw() : entity.self().getBodyYaw());
                Vector curDir = direction.clone().rotateAroundY(originalYaw - yawRads);

                TimeArbiter.teleportDisplay(
                    itemDisplay,
                    l, curDir,
                    2,
                    DisplayUtil.class, 214
                );

                if (iteration.incrementAndGet() % Config.Display.ITEM_DISPLAY_FOLLOW_PARTICLE_INTERVAL == 0)
                    Prefab.Particles.BLEED.display(l);
            },
            null,
            0,  Config.Display.ITEM_DISPLAY_FOLLOW_UPDATE_INTERVAL,
            DisplayUtil.class, "itemDisplayFollow",
            new PredicateRunnablePair(
                () -> entity.isDead() || itemDisplay.isDead() || itemDisplay.getItemStack().getType().isAir(),
                null
            ),
            new PredicateRunnablePair(
                () -> condition != null && toTest != null && condition.test(toTest),
                () -> {
                    if (callback != null && toConsume != null) callback.accept(toConsume);
                }
            )
        );
    }

    /** Starts a task that moves the display to a lerp-computed offset relative to the entity each period. */
    public static TimeArbiter.TaskHandle itemDisplayFollowLerp(
        SwordEntity entity, ItemDisplay display, Vector offset,
        int tpDuration, int period, boolean withPitch) {

        return TimeArbiter.runTimeBoundBukkitTaskOnTimer(
            () -> {
                Vector current =
                    entity.rightBasisVector(withPitch).multiply(offset.getX()).add(
                        entity.upBasisVector(withPitch).multiply(offset.getY()).add(
                            entity.forwardBasisVector(withPitch).multiply(offset.getZ())
                        )
                    );

                Location updated = entity.self()
                    .getLocation()
                    .add(current);

                TimeArbiter.teleportDisplay(
                    display,
                    updated, entity.forwardBasisVector(withPitch),
                    tpDuration,
                    DisplayUtil.class, 257
                );
            },
            null,
            null,
            0, period,
            DisplayUtil.class, "itemDisplayFollowLerp",
            new PredicateRunnablePair(
                () -> !entity.self().isValid() || display.isDead() || !display.isValid(),
                null
            )
        );
    }

    /**
     * Makes an {@link ItemDisplay} track a {@link Mob}'s position each tick with a
     * yaw-relative offset. The offset axes are relative to the mob's facing:
     * {@code offsetRight} is the mob's lateral right, {@code offsetUp} is world up,
     * and {@code offsetForward} is the mob's forward direction.
     *
     * @param mob           the mob to follow
     * @param display       the display entity to teleport
     * @param offsetRight   right-axis offset (mob's local right)
     * @param offsetUp      vertical offset
     * @param offsetForward forward-axis offset (mob's local forward)
     * @param tpDuration    teleport interpolation duration in ticks
     * @param period        update interval in ticks
     * @return a task handle that can be cancelled to stop following
     */
    public static TimeArbiter.TaskHandle itemDisplayFollowMob(
            Mob mob, ItemDisplay display,
            float offsetRight, float offsetUp, float offsetForward,
            int tpDuration, int period) {
        return TimeArbiter.runTimeBoundBukkitTaskOnTimer(
            () -> {
                if (!mob.isValid() || !display.isValid()) return;
                Location loc = mob.getLocation();
                double yawRad = Math.toRadians(loc.getYaw());
                // Forward direction in Minecraft: (-sin yaw, 0, cos yaw)
                double fwdX = -Math.sin(yawRad);
                double fwdZ = Math.cos(yawRad);
                // Right direction: 90° clockwise from forward
                double rightX = Math.cos(yawRad);
                double rightZ = Math.sin(yawRad);
                double worldX = offsetRight * rightX + offsetForward * fwdX;
                double worldZ = offsetRight * rightZ + offsetForward * fwdZ;
                Location target = loc.clone().add(worldX, offsetUp, worldZ);
                TimeArbiter.teleportDisplay(display, target, new Vector(fwdX, 0, fwdZ), tpDuration, DisplayUtil.class, 0);
            },
            null, null, 0, period,
            DisplayUtil.class, "itemDisplayFollowMob"
        );
    }

    /** Starts a task that teleports the display directly to the entity's location each period. */
    public static TimeArbiter.TaskHandle itemDisplayFollowDirect(
        Entity entity, ItemDisplay display, int tpDuration, int period) {

        return TimeArbiter.runTimeBoundBukkitTaskOnTimer(
            () -> {
                Location updated = entity.getLocation();

                TimeArbiter.teleportDisplay(
                    display,
                    updated, updated.getDirection(),
                    tpDuration,
                    DisplayUtil.class, 257
                );
            },
            null,
            null,
            0, period,
            DisplayUtil.class, "itemDisplayFollowDirect"
        );
    }
}
