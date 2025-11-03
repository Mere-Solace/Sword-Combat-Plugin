package btm.sword.util.entity;

import btm.sword.Sword;
import btm.sword.system.entity.base.SwordEntity;
import btm.sword.util.display.DisplayUtil;
import btm.sword.util.display.Prefab;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;

/**
 * Utility class providing helpful static methods for operations on {@link Entity} objects
 * and {@link SwordEntity} wrappers, particularly checking ground status and managing
 * visual following behavior of item displays.
 */
public class EntityUtil {
    /**
     * Checks whether the specified {@link Entity} is currently on the ground.
     * This method checks blocks slightly below the entity's location to determine if it stands on solid ground.
     *
     * @param entity the entity to check
     * @return true if the entity is on ground, false otherwise
     */
    public static boolean isOnGround(Entity entity) {
        double maxCheckDist = 0.3;
        Location base = entity.getLocation().add(new Vector(0, -maxCheckDist, 0));

        double[] offsets = {0};

        for (double x : offsets) {
            for (double z : offsets) {
                if (!base.clone().add(x, 0, z).getBlock().isPassable()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Causes an {@link ItemDisplay} entity to follow a {@link SwordEntity} visually,
     * maintaining a specified direction offset and height, with optional following of the entity's head yaw.
     * The display is updated every 2 ticks asynchronously until either entity or display is dead or air.
     * <p>
     * Uses {@link DisplayUtil#line} and {@link DisplayUtil#smoothTeleport} to create particle trails and smooth motion.
     * </p>
     *
     * @param entity the SwordEntity to follow
     * @param itemDisplay the {@link ItemDisplay} to move to follow the entity
     * @param direction the direction {@link Vector} offset relative to the entity
     * @param heightOffset vertical height offset from the entity's location
     * @param followHead whether to align the display's direction to the entity's head yaw instead of body yaw
     */
    public static void itemDisplayFollow(SwordEntity entity, ItemDisplay itemDisplay, Vector direction, double heightOffset, boolean followHead) {
        double eyeHeight = entity.getEyeHeight();
        Transformation orientation = itemDisplay.getTransformation();
//        Transformation trOffset = new Transformation(
//                orientation.getTranslation().add(0, (float)(heightOffset-eyeHeight), 0),
//                orientation.getLeftRotation(),
//                orientation.getScale(),
//                orientation.getRightRotation());
//        itemDisplay.setTransformation(trOffset);
        double originalYaw = Math.toRadians(entity.entity().getBodyYaw());
        Vector offset = Prefab.Direction.UP.clone().multiply(heightOffset);

        itemDisplay.setBillboard(Display.Billboard.FIXED);
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


                DisplayUtil.smoothTeleport(itemDisplay, 2);
                itemDisplay.teleport(l);

                if (step % 4 == 0)
                    Prefab.Particles.BLEED.display(l);

                step++;
            }
        }.runTaskTimer(Sword.getInstance(), 0L, 2L);
    }
}
