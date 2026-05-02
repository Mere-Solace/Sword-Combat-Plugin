package btm.sword.system.entity.impl;

import java.util.concurrent.TimeUnit;

import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import btm.sword.control.SwordScheduler;
import btm.sword.system.entity.base.CombatProfile;

/**
 * A {@link Hostile} whose visuals are entirely driven by a DEU display rig.
 *
 * <p>Extends {@link Hostile} with overrides specific to display-rig mobs:</p>
 * <ul>
 *   <li><b>Weapon retrieval</b>: {@link #receiveRetrievedWeapon} updates the
 *       logical {@code itemInRightHand} field and the rig weapon-slot display but does
 *       <em>not</em> write to the mob's vanilla equipment slot (which is permanently AIR).</li>
 *   <li><b>Death sequence</b>: {@link #onZeroHealth()} schedules a 5-tick delayed freeze
 *       so knockback resolves first, then zeroes velocity, disables gravity, and calls
 *       {@link btm.sword.system.entity.display.DisplayRig#lockOnDeath()} to freeze the
 *       rig at the point of death and prevent animation state transitions.</li>
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
     * Extends {@link Hostile#onZeroHealth()} with display-rig death behaviour.
     *
     * <p>Immediately delegates to super (which disables AI and triggers the death animation),
     * then schedules a 5-tick delayed task that:</p>
     * <ol>
     *   <li>Zeroes velocity and disables gravity — mob body stays at the knockback-resolved
     *       position for the remainder of the death animation.</li>
     *   <li>Calls {@link btm.sword.system.entity.display.DisplayRig#lockOnDeath()} —
     *       dismounts the rig from the mob, cancels the yaw follow, and prevents any
     *       further animation state transitions.</li>
     * </ol>
     * <p>The 5-tick delay allows physics to apply one final knockback impulse before
     * the entity is frozen in place.</p>
     */
    @Override
    public void onZeroHealth() {
        // Trigger AI disable + death animation immediately.
        super.onZeroHealth();

        // After 5 ticks, freeze position so knockback has time to apply.
        SwordScheduler.runBukkitTaskLater(() -> {
            if (!mob().isValid()) return;
            mob().setVelocity(new Vector(0, 0, 0));
            mob().setGravity(false);
            if (getDisplayRig() != null) {
                getDisplayRig().lockOnDeath();
            }
        }, 5 * 50, TimeUnit.MILLISECONDS);
    }
}
