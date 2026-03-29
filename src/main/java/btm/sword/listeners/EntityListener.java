package btm.sword.listeners;


import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.damage.DamageSource;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;

import btm.sword.system.entity.SwordEntityArbiter;
import btm.sword.system.entity.base.SwordEntity;
import btm.sword.system.entity.display.DisplayRig;
import btm.sword.system.entity.impl.Combatant;
import btm.sword.system.entity.impl.Hostile;
import btm.sword.utility.Prefab;
import net.donnypz.displayentityutils.events.AnimationCompleteEvent;

/**
 * Listener for entity lifecycle, damage, animation, and pickup events.
 *
 * <p>Bridges Bukkit/Paper entity events into the SwordEntity system: new
 * {@link org.bukkit.entity.LivingEntity} instances are registered with
 * {@link btm.sword.system.entity.SwordEntityArbiter}, departing entities are cleaned up,
 * vanilla damage is intercepted and re-routed through the Sword combat pipeline, DEU
 * animation completion drives death sequencing, and LIGHTNING_ROD item displays are tagged
 * as weapon-slot anchors for {@link btm.sword.system.entity.display.DisplayRig}.</p>
 */
public class EntityListener implements Listener {
    /**
     * Handles the event when any entity is added to the world (including players).
     * <p>
     * Registers new {@link LivingEntity} instances with the {@link SwordEntityArbiter}
     * to enable SwordEntity functionality. This ensures that all sword-related
     * systems (e.g., resource management, combat effects) recognize the entity.
     * </p>
     *
     * @implNote Must register new entities with the {@code SwordEntityArbiter}
     *           for SwordEntity functionality to work properly.
     *
     * @param event the {@link EntityAddToWorldEvent} triggered when an entity is added to the world
     */
    @EventHandler
    public void entityAddEvent(EntityAddToWorldEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof LivingEntity livingEntity) {
            SwordEntityArbiter.register(livingEntity);
            SwordEntity swordEntity = SwordEntityArbiter.get(livingEntity);
            if (swordEntity != null) {
                swordEntity.resetResources();
                swordEntity.onSpawn();
            }
        }
        // Tag any LIGHTNING_ROD item display as a weapon slot for DisplayRig.
        // Place a LIGHTNING_ROD item on the desired display entity part in your DEU group
        // to mark it as the weapon slot; the runtime item is then set via setWeaponSlotItem().
        if (entity instanceof ItemDisplay itemDisplay) {
            ItemStack displayed = itemDisplay.getItemStack();
            if (displayed.getType() == Material.LIGHTNING_ROD) {
                itemDisplay.addScoreboardTag(DisplayRig.WEAPON_SLOT_TAG);
            }
        }
    }

    /**
     * Handles the event when an entity is removed from the world.
     * <p>
     * Performs cleanup for entities managed by the {@link SwordEntityArbiter}.
     * This includes calling {@link SwordEntity#onDeath()} and deregistering the entity
     * to prevent memory leaks or stale references.
     * </p>
     *
     * @param event the {@link EntityRemoveFromWorldEvent} triggered when an entity is removed from the world
     */
    @EventHandler
    public void entityRemoveEvent(EntityRemoveFromWorldEvent event) {
        if (event.getEntity() instanceof LivingEntity livingEntity) {
            SwordEntity swordEntity = SwordEntityArbiter.get(livingEntity);
            if (swordEntity != null) {
                swordEntity.onDeath();
                SwordEntityArbiter.remove(livingEntity);
            }
        }
    }

    /**
     * Handles entity damage events for living entities.
     * <p>
     * This is currently used for debugging or testing, as it overrides normal
     * damage behavior by setting a minimal damage value and healing the entity
     * significantly. The arbitrary damage threshold (7474040) prevents unwanted
     * interference with large-damage test cases.
     * </p>
     *
     * @param event the {@link EntityDamageEvent} triggered when an entity takes damage
     */
    @EventHandler
    public void entityDamageEvent(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity)) return; // Might be used later for other types of damageable entities.
        SwordEntity hurt = SwordEntityArbiter.getOrAdd((LivingEntity) event.getEntity());

        DamageSource damageSource = event.getDamageSource();

        SwordEntity aggressor;
        if (damageSource.getCausingEntity() != null) {
            Location loc = damageSource.getDamageLocation();
            Vector kb = loc != null ? loc.getDirection() : new Vector();
            aggressor = SwordEntityArbiter.get((LivingEntity) damageSource.getCausingEntity());
            if (aggressor instanceof Combatant c) {
                if (hurt != null)
                    hurt.hit(c, Prefab.Attacks.DEFAULT_MOB_HIT, kb);
            }
        }

        if (event.getEntity() instanceof LivingEntity && event.getDamage() < 7474040) {
            event.setDamage(0.01);
            ((LivingEntity) event.getEntity()).heal(100);
        }
    }

    /**
     * Detects when a DEU animation finishes on a hostile mob's display rig.
     * If the mob is in its death animation, deal lethal damage to trigger the actual kill.
     *
     * @param event the {@link AnimationCompleteEvent} fired by DEU when an animation ends
     */
    @EventHandler
    public void onAnimationComplete(AnimationCompleteEvent event) {
        Entity vehicle = event.getGroup().getVehicle();
        if (!(vehicle instanceof LivingEntity livingEntity)) return;
        SwordEntity swordEntity = SwordEntityArbiter.get(livingEntity);
        if (!(swordEntity instanceof Hostile hostile)) return;
        if (!hostile.isInDeathAnimation()) return;
        String tag = event.getAnimation().getAnimationTag();
        DisplayRig rig = hostile.getDisplayRig();
        if (rig != null && tag != null && tag.equals(rig.dieAnimTag())) {
            livingEntity.damage(74077740);
        }
    }

    /**
     * Handles item pickup events by entities.
     *
     * @param event the {@link EntityPickupItemEvent} triggered when an entity picks up an item
     */
    @EventHandler
    public void entityPickupItemEvent(EntityPickupItemEvent event) {

    }
}
