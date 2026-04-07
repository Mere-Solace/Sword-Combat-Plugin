package btm.sword.system.combat;

import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

import btm.sword.system.entity.base.SwordEntity;
import btm.sword.utility.Prefab;

/** An affliction that pins the entity downward, suppressing upward velocity for its duration. */
public class GroundedAffliction extends Affliction {
    /** Creates a grounded affliction with the given tick duration and downward strength. */
    public GroundedAffliction(long tickDuration, double strength) {
        super(true, tickDuration, strength);
    }

    @Override
    public void onApply(SwordEntity afflicted) {
        LivingEntity a = afflicted.self();
        Vector v = a.getVelocity();
        a.setVelocity(new Vector(v.getX(), -strength, v.getZ()));
    }

    @Override
    public void end(SwordEntity afflicted) {
        Prefab.Particles.THROW_TRAIL.display(afflicted.self().getLocation());
    }
}
