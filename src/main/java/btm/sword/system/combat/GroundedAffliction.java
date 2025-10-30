package btm.sword.system.combat;

import btm.sword.system.entity.SwordEntity;
import btm.sword.util.Cache;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

/**
 * An affliction that forces an entity to the ground by applying downward velocity.
 * <p>
 * This affliction is typically applied when an entity is airborne and needs to be
 * grounded, such as after certain attacks. The strength parameter determines the
 * downward velocity applied.
 * </p>
 *
 * @see Affliction
 */
public class GroundedAffliction extends Affliction {
    /**
     * Constructs a new GroundedAffliction with the specified duration and strength.
     *
     * @param tickDuration the duration of the affliction in ticks
     * @param strength the downward velocity to apply (higher values = stronger downward force)
     */
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
