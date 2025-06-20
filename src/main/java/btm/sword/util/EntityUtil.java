package btm.sword.util;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

public class EntityUtil {
	public static boolean isOnGround(LivingEntity entity) {
		double maxCheckDist = 0.3;
		Location base = entity.getLocation().add(new Vector(0, -maxCheckDist, 0));
		
		double[] offsets = {-0.4, -0.2, 0, 0.2, -0.4};
		
		for (double x : offsets) {
			for (double z : offsets) {
				if (!base.clone().add(x, 0, z).getBlock().isPassable()) {
					return true;
				}
			}
		}
		return false;
	}
}
