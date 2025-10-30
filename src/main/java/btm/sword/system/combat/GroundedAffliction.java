package btm.sword.system.combat;

import btm.sword.system.entity.SwordEntity;
import btm.sword.util.Cache;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

public class GroundedAffliction extends Affliction {
    public GroundedAffliction(long tickDuration, double strength) {
        super(true, tickDuration, strength);
    }

    @Override
    public void onApply(SwordEntity afflicted) {
        LivingEntity a = afflicted.entity();
        Vector v = a.getVelocity();
        a.setVelocity(new Vector(v.getX(), -strength, v.getZ()));
    }

    @Override
    public void end(SwordEntity afflicted) {
        Cache.throwTrailParticle.display(afflicted.entity().getLocation());
    }
}
