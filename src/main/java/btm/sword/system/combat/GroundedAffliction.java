package btm.sword.system.combat;

import btm.sword.system.entity.SwordEntity;
import btm.sword.util.Cache;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

public class GroundedAffliction extends Affliction {
	public GroundedAffliction(long tickDuration) {
		super(true, tickDuration);
	}
	
	@Override
	public void apply(SwordEntity afflicted) {
		LivingEntity a = afflicted.entity();
		Vector v = a.getVelocity();
		a.setVelocity(new Vector(v.getX(), 0, v.getZ()));
	}
	
	@Override
	public void end(SwordEntity afflicted) {
		Cache.throwTrailParticle.display(afflicted.entity().getLocation());
	}
}
