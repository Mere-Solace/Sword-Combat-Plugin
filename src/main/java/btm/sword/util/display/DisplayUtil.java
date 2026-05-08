package btm.sword.util.display;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.util.Vector;

import btm.sword.config.section.DirectionConfig;
import btm.sword.config.section.DisplayConfig;
import btm.sword.entity.base.SwordEntity;
import btm.sword.runtime.scheduler.PredicateRunnablePair;
import btm.sword.runtime.scheduler.TimeArbiter;
import btm.sword.util.prefab.Prefab;

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
        Vector offset = DirectionConfig.up().multiply(heightOffset);

        itemDisplay.setBillboard(DisplayConfig.ITEM_DISPLAY_FOLLOW_BILLBOARD_MODE);

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

                if (iteration.incrementAndGet() % DisplayConfig.ITEM_DISPLAY_FOLLOW_PARTICLE_INTERVAL == 0)
                    Prefab.Particles.BLEED.display(l);
            },
            null,
            0,  DisplayConfig.ITEM_DISPLAY_FOLLOW_UPDATE_INTERVAL,
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
}
