package btm.sword.listeners;

import java.util.Objects;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.damage.DamageSource;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;

import btm.sword.Sword;
import btm.sword.system.entity.SwordEntityArbiter;
import btm.sword.system.entity.base.SwordEntity;
import btm.sword.system.entity.types.Combatant;

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
        if (entity instanceof LivingEntity) {
            SwordEntityArbiter.register(entity);
            SwordEntity swordEntity = SwordEntityArbiter.get(entity.getUniqueId());
            if (swordEntity != null) {
                swordEntity.resetResources();
                swordEntity.onSpawn();
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
        SwordEntity swordEntity = SwordEntityArbiter.get(event.getEntity().getUniqueId());
        if (swordEntity != null) {
            swordEntity.onDeath();
            SwordEntityArbiter.remove(event.getEntity().getUniqueId());
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
        SwordEntity hurt = SwordEntityArbiter.getOrAdd(event.getEntity().getUniqueId());

        DamageSource damageSource = event.getDamageSource();

        SwordEntity aggressor;
        if (damageSource.getCausingEntity() != null) {
            Location loc = damageSource.getDamageLocation();
            Vector kb = loc != null ? loc.getDirection() : new Vector();
            aggressor = SwordEntityArbiter.get(damageSource.getCausingEntity().getUniqueId());
            if (aggressor instanceof Combatant c) {
//                if (c instanceof SwordPlayer s && s.isBlocking()) {
//                    s.message("Blocked that tomfoolery with ease!");
//                    return;
//                }
                hurt.hit(c, 15, 1, 10, 10, kb);
            }
        }

        if(event.getEntity() instanceof LivingEntity && event.getDamage() < 7474040) {
            event.setDamage(0.01);
            ((LivingEntity) event.getEntity()).heal(100);
        }
    }

    /**
     * Handles item pickup events by entities.
     * <p>
     *
     * </p>
     *
     * @param event the {@link EntityPickupItemEvent} triggered when an entity picks up an item
     */
    @EventHandler
    public void entityPickupItemEvent(EntityPickupItemEvent event) {
        // Test call to see how NamespacedKey works
        String itemType = event.getItem().getItemStack().getItemMeta().getPersistentDataContainer()
                .get(new NamespacedKey(Sword.getInstance(), "weapon"), PersistentDataType.STRING);
        if (Objects.equals(itemType, "long_sword")) {
            event.getEntity().sendMessage("Picked up a sword");
        }
    }
}
