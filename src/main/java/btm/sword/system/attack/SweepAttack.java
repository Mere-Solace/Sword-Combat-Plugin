package btm.sword.system.attack;

import org.bukkit.inventory.ItemStack;

import btm.sword.system.attack.style.AttackProfile;

/**
 * A {@link Attack} subtype reserved for future sweep-trail visualisation.
 *
 * <p>Currently behaves identically to {@link Attack}. The visual trail feature
 * (spawning and animating ItemDisplay entities along the sweep path) is tracked in
 * GitHub issue TODO — implement before adding display fields back.</p>
 */
public class SweepAttack extends Attack {

    /** Constructs a sweep attack with default timing and iteration values from the profile. */
    public SweepAttack(ItemStack itemUsedInAttack, AttackProfile profile, boolean orientWithPitch) {
        super(itemUsedInAttack, profile, orientWithPitch);
    }

    /** Constructs a sweep attack with explicit timing, iteration count, and Bezier range. */
    public SweepAttack(ItemStack itemUsedInAttack, AttackProfile profile, boolean orientWithPitch,
            int attackMilliseconds, int attackIterations,
            double attackStartValue, double attackEndValue) {
        super(itemUsedInAttack, profile, orientWithPitch,
            attackMilliseconds, attackIterations, attackStartValue, attackEndValue);
    }
}
