package btm.sword.system.combat;

import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

import btm.sword.system.entity.base.SwordEntity;
import btm.sword.util.Prefab;

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
        Prefab.Particles.THROW_TRAIl.display(afflicted.entity().getLocation());
    }
}
