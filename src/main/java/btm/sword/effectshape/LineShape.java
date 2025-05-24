package btm.sword.effectshape;

import org.bukkit.entity.LivingEntity;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Collection;
import java.util.HashSet;

public class LineShape extends EffectShape {
	public Collection<LivingEntity> getTargets(Player player, Location origin, Vector direction, double range) {
		Collection<LivingEntity> targets = new HashSet<>();
		
		Vector e = direction.normalize();
		Location cur = origin.clone();
		
		for (double i = 0; i < range; i += .25) {
			cur = cur.add(e.multiply(i));
			targets.addAll(cur.getNearbyLivingEntities(.25));
		}
		
		targets.remove(player);
		
		return targets;
	}
}
