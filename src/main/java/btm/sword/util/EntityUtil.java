package btm.sword.util;

import btm.sword.Sword;
import btm.sword.system.entity.SwordEntity;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;

import java.util.List;

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
		Transformation orientation = itemDisplay.getTransformation();
		Vector offset = VectorUtil.UP.clone().multiply(heightOffset);
		
		double originalYaw = Math.toRadians(entity.entity().getBodyYaw());
		
		int[] step = {0};
		new BukkitRunnable() {
			@Override
			public void run() {
				if (entity.isDead() || itemDisplay.isDead() || itemDisplay.getItemStack().getType().isAir()) {
					cancel();
				}
				
				Location l = entity.entity().getLocation().add(offset);
				
				double yawRads = Math.toRadians(followHead ? entity.entity().getYaw() : entity.entity().getBodyYaw());
				Vector curDir = direction.clone().rotateAroundY(originalYaw-yawRads);
				l.setDirection(curDir);
				if (step[0] % 6 == 0) {
                    DisplayUtil.line(List.of(Cache.thrownItemStickParticle), l.clone().subtract(curDir), curDir, 0.75, 0.25);
                    if (step[0] % 12 == 0)
                        DisplayUtil.line(List.of(Cache.testBleedParticle), l.clone().subtract(curDir), curDir, 0.3, 0.25);
                }

                DisplayUtil.smoothTeleport(itemDisplay);
				itemDisplay.teleport(l);
				itemDisplay.setTransformation(orientation);
				
				step[0]++;
			}
		}.runTaskTimer(Sword.getInstance(), 0L, 2L);
	}
}
