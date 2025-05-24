package btm.sword.visualeffect;

import org.bukkit.Location;
import org.bukkit.util.Vector;

public abstract class VisualEffect {
	public abstract void drawEffect(Location origin, Vector direction, double range);
}
