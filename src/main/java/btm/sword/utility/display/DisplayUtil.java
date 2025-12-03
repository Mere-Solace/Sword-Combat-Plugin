package btm.sword.utility.display;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
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
public class DisplayUtil {

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

    public static TimeArbiter.TaskHandle displaySlerpToOffset(
        SwordEntity entity, ItemDisplay display, Vector offset,
        double speed, int tpDuration, int period,
        double endDistance, boolean removeOnArrival,
        int timeoutTicks,
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
                        ));
                    currentDirectionToTarget.set(curTarget.toVector().subtract(display.getLocation().toVector()));
                } catch (RuntimeException ignored) { }
            },
            () -> {
                Vector scaledDiff = currentDirectionToTarget.get().clone().normalize().multiply(speed);
                Location update = display.getLocation().add(scaledDiff);
                TimeArbiter.teleportDisplay(
                    display,
                    update, update.toVector().subtract(display.getLocation().toVector()),
                    tpDuration
                );
            },
            null,
            0, period,
            new PredicateRunnablePair(
                // ticks is incremented here, not explicitly during a runnable.
                () -> entity.isInvalid() || !display.isValid() || ticks.getAndIncrement() > timeoutTicks,
                () -> {
                    if (display.isValid() && removeOnArrival) display.remove();
                    if (callback != null) {
                        SwordScheduler.runBukkitTask(callback);
                    }
                }
            ),
            new PredicateRunnablePair(
                () -> currentDirectionToTarget.get().isZero() ||
                    currentDirectionToTarget.get().lengthSquared() < endDistance * endDistance,
                () -> {
                    if (display.isValid() && removeOnArrival) display.remove();
                    if (callback != null) {
                        SwordScheduler.runBukkitTask(callback);
                    }
                }
            )
        );
    }

    // returns a task for use in detecting when finished
    public static <T> TimeArbiter.TaskHandle displaySlerpToOffset(
        SwordEntity entity, ItemDisplay display, Vector offset,
        double speed, int tpDuration, int period,
        double endDistance, boolean removeOnArrival,
        int timeoutTicks,
        Predicate<T> condition, T toTest,
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
                } catch (RuntimeException ignored) { }
            },
            () -> {
                Vector scaledDiff = currentDirectionToTarget.get().normalize().multiply(speed);
                Location update = display.getLocation().add(scaledDiff);
                TimeArbiter.teleportDisplay(
                    display,
                    update, update.toVector().subtract(display.getLocation().toVector()),
                    tpDuration
                );
            },
            null,
            0, period,
            new PredicateRunnablePair(
                () -> entity.isInvalid() || !display.isValid() ||
                    ticks.getAndIncrement() > timeoutTicks || condition.test(toTest),
                () -> {
                    if (display.isValid() && removeOnArrival) display.remove();
                    if (callback != null) {
                        SwordScheduler.runBukkitTask(callback);
                    }
                }
            ),
            new PredicateRunnablePair(
                () -> currentDirectionToTarget.get().isZero() ||
                    currentDirectionToTarget.get().lengthSquared() < endDistance*endDistance ||
                    ticks.get() > timeoutTicks || condition.test(toTest),
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
    public static <T> void itemDisplayFollow(
        SwordEntity entity, ItemDisplay itemDisplay, Vector direction,
        double heightOffset, boolean followHead,
        Predicate<T> condition, T toTest,
        Consumer<T> callback, T toConsume) {

        double originalYaw = Math.toRadians(entity.self().getBodyYaw());
        Vector offset = Config.Direction.UP().multiply(heightOffset);

        itemDisplay.setBillboard(Config.Display.ITEM_DISPLAY_FOLLOW_BILLBOARD_MODE);
        entity.self().addPassenger(itemDisplay);

        AtomicInteger iteration = new AtomicInteger(0);
        TimeArbiter.runTimeBoundBukkitTaskOnTimer(
            null,
            () -> {
                Location l = entity.self().getLocation().add(offset);

                double yawRads = Math.toRadians(followHead ? entity.self().getYaw() : entity.self().getBodyYaw());
                Vector curDir = direction.clone().rotateAroundY(originalYaw-yawRads);

                TimeArbiter.teleportDisplay(
                    itemDisplay,
                    l, curDir,
                    1
                );

                if (iteration.incrementAndGet() % Config.Display.ITEM_DISPLAY_FOLLOW_PARTICLE_INTERVAL == 0)
                    Prefab.Particles.BLEED.display(l);
            },
            null,
            0,  Config.Display.ITEM_DISPLAY_FOLLOW_UPDATE_INTERVAL,
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
                    tpDuration
                );
            },
            null,
            null,
            0, period
        );
    }
}
