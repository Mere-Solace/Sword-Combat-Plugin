package btm.sword.combat.attack;

import org.bukkit.inventory.ItemStack;

import btm.sword.combat.style.AttackProfile;
import btm.sword.util.prefab.Prefab;

/**
 * A sweep attack used by hostile mobs.
 *
 * <p>Extends {@link SweepAttack} and overrides {@link #hit()} to apply
 * {@link btm.sword.util.prefab.Prefab.Attacks#DEFAULT_MOB_HIT} instead of the default player hit packet.
 */
public class MobSweepAttack extends SweepAttack {

    /**
     * Constructs a {@code MobSweepAttack}.
     *
     * @param itemUsedInAttack the item used in the attack
     * @param profile          the {@link AttackProfile} defining the sweep path and knockback
     * @param orientWithPitch  whether to orient the attack with the attacker's pitch
     */
    public MobSweepAttack(ItemStack itemUsedInAttack, AttackProfile profile, boolean orientWithPitch) {
        super(itemUsedInAttack, profile, orientWithPitch);
    }

    @Override
    protected void hit() {
        currentTarget.hit(attacker, Prefab.Attacks.DEFAULT_MOB_HIT,
            attackProfile.knockbackFunction().apply(currentTarget).apply(this));
        Prefab.Particles.TEST_HIT.display(currentTarget.getChestLocation());
    }
}
