package btm.sword.listeners;

import btm.sword.Sword;
import btm.sword.system.entity.SwordEntityArbiter;
import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.Objects;

public class EntityListener implements Listener {
	
	@EventHandler
	public void entityAddEvent(EntityAddToWorldEvent event) {
		Entity entity = event.getEntity();
		if (entity instanceof LivingEntity) {
			SwordEntityArbiter.register(entity);
			SwordEntityArbiter.get(entity.getUniqueId()).resetResources();
		}
	}
	
	@EventHandler
	public void entityRemoveEvent(EntityRemoveFromWorldEvent event) {
		SwordEntityArbiter.remove(event.getEntity().getUniqueId());
	}
	
	@EventHandler
	public void entityDamageEvent(EntityDamageEvent event) {
		if(event.getEntity() instanceof LivingEntity && event.getDamage() < 7474040) {
			event.setDamage(0.01);
			((LivingEntity) event.getEntity()).heal(100);
		}
	}
	
	@EventHandler
	public void entityPickupItemEvent(EntityPickupItemEvent event) {
		String itemType = event.getItem().getItemStack().getItemMeta().getPersistentDataContainer()
				.get(new NamespacedKey(Sword.getInstance(), "weapon"), PersistentDataType.STRING);
		if (Objects.equals(itemType, "long_sword")) {
			event.getEntity().sendMessage("Picked up a sword");
		}
	}
}
