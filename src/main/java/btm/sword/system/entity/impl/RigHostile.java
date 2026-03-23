package btm.sword.system.entity.impl;

import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import btm.sword.system.entity.base.CombatProfile;



/**
 * A {@link Hostile} whose visuals are entirely driven by a DEU display rig.
 *
 * <p>Extends {@link Hostile} with overrides specific to display-rig mobs:</p>
 * <ul>
 *   <li><b>Weapon retrieval</b>: {@link #receiveRetrievedWeapon} updates the
 *       logical {@code itemInRightHand} field and the rig weapon-slot display but does
 *       <em>not</em> write to the mob's vanilla equipment slot (which is permanently AIR).</li>
 *   <li><b>Death sequence</b>: {@link #onZeroHealth()} additionally freezes the mob's
 *       velocity, locks the rig's server-side position via {@link btm.sword.system.entity.display.DisplayRig#lockOnDeath()},
 *       and prevents any further animation state transitions so the DEATH animation
 *       plays to completion without interruption.</li>
 * </ul>
 */
public class RigHostile extends Hostile {

    /**
     * Constructs a new RigHostile wrapping the given entity.
     *
     * @param associatedEntity the Bukkit living entity to wrap
     * @param combatProfile    the combat profile defining stats and settings
     */
    public RigHostile(LivingEntity associatedEntity, CombatProfile combatProfile) {
        super(associatedEntity, combatProfile);
    }

    /**
     * Reclaims the thrown weapon into the logical {@code itemInRightHand} field and
     * restores the rig weapon-slot display. Does <em>not</em> write to the vanilla
     * equipment slot — the vanilla main hand remains AIR for all display-rig mobs.
     *
     * @param item the recovered item stack
     */
    @Override
    public void receiveRetrievedWeapon(ItemStack item) {
        itemInRightHand = item;
        onWeaponRetrieved();
    }

    /**
     * Extends {@link Hostile#onZeroHealth()} with display-rig death behaviour:
     * <ol>
     *   <li>Zeroes the mob's server-side velocity and disables gravity so the
     *       body does not drift during the death animation.</li>
     *   <li>Calls {@link btm.sword.system.entity.display.DisplayRig#lockOnDeath()} to
     *       dismount the rig from the mob and cancel the yaw follow, freezing the
     *       rig's server-side position at the point of death.</li>
     *   <li>Locks the state machine so no transition can interrupt the DEATH
     *       animation after it starts.</li>
     * </ol>
     */
    @Override
    public void onZeroHealth() {
        // Freeze the mob body in place so it does not fall or drift.
        mob().setVelocity(new Vector(0, 0, 0));
        mob().setGravity(false);

        // Lock the display rig position and prevent further state transitions.
        if (getDisplayRig() != null) {
            getDisplayRig().lockOnDeath();
        }

        // Run common Hostile death logic (AI disable, animation trigger, fallback timer).
        super.onZeroHealth();
    }
}
