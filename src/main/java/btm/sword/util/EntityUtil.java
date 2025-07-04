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
	
	public static void itemDisplayFollowTest(SwordEntity entity, ItemDisplay itemDisplay, Vector direction, double heightOffset, boolean followHead) {
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
				if (step[0] % 6 == 0)
					DisplayUtil.line(List.of(Cache.thrownItemStickParticle), l.clone().subtract(curDir), curDir, 0.75, 0.25);
				
				itemDisplay.teleport(l);
				itemDisplay.setTransformation(orientation);
				
				step[0]++;
			}
		}.runTaskTimer(Sword.getInstance(), 0L, 2L);
	}
}
