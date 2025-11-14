package btm.sword.util.display;

import java.util.function.Predicate;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import btm.sword.Sword;
import btm.sword.config.ConfigManager;
import btm.sword.system.entity.base.SwordEntity;

/**
 * Utility class for manipulating {@link Display} objects in Bukkit.
 * Provides methods to control teleportation animation, interpolation, and drawing geometric lines.
 * <p>
 * Useful for visual effects and particle display management in the Sword plugin.
 * </p>
 */
public class DisplayUtil {
    /**
     * Performs a smooth teleport of a {@link Display} using the default duration.
     * Sets the teleport duration on the display, creating an animated transition.
     *
     * @param display the {@link Display} to teleport smoothly
     */
    public static void smoothTeleport(Display display) {
        display.setTeleportDuration(ConfigManager.getInstance().getDisplay().getDefaultTeleportDuration());
    }

    /**
     * Performs a smooth teleport of a {@link Display} over a specified duration in ticks.
     *
     * @param display the {@link Display} to teleport
     * @param duration the duration in ticks for the teleportation animation
     */
    public static void smoothTeleport(Display display, int duration) {
        display.setTeleportDuration(duration);
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

    // returns a task for use in detecting when finished
    public static BukkitTask displaySlerpToOffset(SwordEntity entity, ItemDisplay display, Vector offset,
                                                  double speed, int tpDuration, int period,
                                                  double endDistance, boolean removeOnArrival,
                                                  Runnable callback) {
        return new BukkitRunnable() {
            @Override
            public void run() {
                if (!entity.isValid() || !display.isValid()) {
                    if (display.isValid() && removeOnArrival) display.remove();
                    if (callback != null) {
                        Bukkit.getScheduler().runTask(Sword.getInstance(), callback);
                    }
                    cancel();
                    return;
                }

                Location curTarget = entity.entity().getLocation().add(
                        entity.rightBasisVector(false).multiply(offset.getX()).add(
                                entity.upBasisVector(false).multiply(offset.getY()).add(
                                        entity.forwardBasisVector(false).multiply(offset.getZ())
                                )
                        ));

                Vector diff = curTarget.toVector().subtract(display.getLocation().toVector());

                if (diff.isZero() || diff.lengthSquared() < endDistance*endDistance) {
                    if (display.isValid() && removeOnArrival) display.remove();
                    if (callback != null) {
                        Bukkit.getScheduler().runTask(Sword.getInstance(), callback);
                    }
                    cancel();
                    return;
                }

                Vector scaledDiff = diff.normalize().multiply(speed);

                Location update = display.getLocation().add(scaledDiff);

                smoothTeleport(display, tpDuration);
                display.teleport(update);
            }
        }.runTaskTimer(Sword.getInstance(), 0L, period);
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
    public static void itemDisplayFollow(SwordEntity entity, ItemDisplay itemDisplay, Vector direction, double heightOffset, boolean followHead) {
        double originalYaw = Math.toRadians(entity.entity().getBodyYaw());
        Vector offset = Prefab.Direction.UP.clone().multiply(heightOffset);

        var displayFollow = ConfigManager.getInstance().getDisplay().getItemDisplayFollow();
        itemDisplay.setBillboard(displayFollow.getBillboardMode());
        entity.entity().addPassenger(itemDisplay);

        new BukkitRunnable() {
            int step = 0;
            @Override
            public void run() {
                if (entity.isDead() || itemDisplay.isDead() || itemDisplay.getItemStack().getType().isAir()) {
                    cancel();
                }
                Location l = entity.entity().getLocation().add(offset);

                double yawRads = Math.toRadians(followHead ? entity.entity().getYaw() : entity.entity().getBodyYaw());
                Vector curDir = direction.clone().rotateAroundY(originalYaw-yawRads);
                l.setDirection(curDir);

                DisplayUtil.smoothTeleport(itemDisplay, displayFollow.getUpdateInterval());
                itemDisplay.teleport(l);

                if (step % displayFollow.getParticleInterval() == 0)
                    Prefab.Particles.BLEED.display(l);

                step++;
            }
        }.runTaskTimer(Sword.getInstance(), 0L, displayFollow.getUpdateInterval());
    }

    // x = right, y = up, z = forward
    public static <T> void itemDisplayFollowLerpTillCondition(SwordEntity entity, ItemDisplay display, Vector offset, int tpDuration, int period, boolean withPitch, Predicate<T> endCondition, T toTest) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (endCondition.test(toTest)) {
                    cancel();
                    return;
                }

                DisplayUtil.smoothTeleport(display, tpDuration);

                display.teleport(entity.entity().getLocation().add(
                        entity.rightBasisVector(withPitch).multiply(offset.getX()).add(
                                entity.upBasisVector(withPitch).multiply(offset.getY()).add(
                                        entity.forwardBasisVector(withPitch).multiply(offset.getZ())
                                )
                        )
                ).setDirection(entity.forwardBasisVector(withPitch)));
            }
        }.runTaskTimer(Sword.getInstance(), 0L, period);
    }

    public static BukkitTask itemDisplayFollowLerp(SwordEntity entity, ItemDisplay display, Vector offset, int tpDuration, int period, boolean withPitch) {
        return new BukkitRunnable() {
            @Override
            public void run() {
                DisplayUtil.smoothTeleport(display, tpDuration);

                display.teleport(entity.entity().getLocation().add(
                    entity.rightBasisVector(withPitch).multiply(offset.getX()).add(
                        entity.upBasisVector(withPitch).multiply(offset.getY()).add(
                            entity.forwardBasisVector(withPitch).multiply(offset.getZ())
                        )
                    )
                ).setDirection(entity.forwardBasisVector(withPitch)));
            }
        }.runTaskTimer(Sword.getInstance(), 0L, period);
    }
}
