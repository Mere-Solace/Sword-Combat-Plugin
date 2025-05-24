package btm.sword.effectshape;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Collection;

public abstract class EffectShape {
	public abstract Collection<LivingEntity> getTargets(Player player, Location origin, Vector direction, double range);
}
