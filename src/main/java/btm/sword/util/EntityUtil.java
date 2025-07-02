package btm.sword.util;

import btm.sword.Sword;
import btm.sword.system.entity.SwordEntity;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;

public class EntityUtil {
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
	
	public static void itemDisplayFollow(SwordEntity entity, ItemDisplay itemDisplay, double relativeOffsetAngle, boolean clockwise, Transformation orientation) {
		new BukkitRunnable() {
			@Override
			public void run() {
				if (entity.isDead() || itemDisplay.isDead() || itemDisplay.getItemStack().getType().isAir()) {
					cancel();
				}
				
				Location l = entity.entity().getLocation();
				
				double yawRads = clockwise ?
						Math.toRadians(entity.entity().getBodyYaw()) + relativeOffsetAngle :
						Math.toRadians(entity.entity().getBodyYaw()) - relativeOffsetAngle;
				
				l.setDirection(new Vector(-Math.sin(yawRads), 0, Math.cos(yawRads)));
				
				itemDisplay.teleport(l);
				itemDisplay.setTransformation(orientation);
			}
		}.runTaskTimer(Sword.getInstance(), 0L, 2L);
	}
	
	public static void itemDisplayFollowTest(SwordEntity entity, ItemDisplay itemDisplay, double heightOffset, double relativeOffsetAngle) {
		Transformation orientation = itemDisplay.getTransformation();
		Vector offset = VectorUtil.UP.clone().multiply(heightOffset);
		
		new BukkitRunnable() {
			@Override
			public void run() {
				if (entity.isDead() || itemDisplay.isDead() || itemDisplay.getItemStack().getType().isAir()) {
					cancel();
				}
				
				Location l = entity.entity().getLocation().add(offset);
				
				double yawRads = Math.toRadians(entity.entity().getBodyYaw()) + relativeOffsetAngle;
				
				l.setDirection(new Vector(-Math.sin(yawRads), 0, Math.cos(yawRads)));
				
				itemDisplay.teleport(l);
				itemDisplay.setTransformation(orientation);
			}
		}.runTaskTimer(Sword.getInstance(), 0L, 2L);
	}
}
