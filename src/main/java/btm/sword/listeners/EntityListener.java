package btm.sword.listeners;

import btm.sword.system.entity.SwordEntityArbiter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntitySpawnEvent;

public class EntityListener implements Listener {
	
	@EventHandler
	public void entitySpawnEvent(EntitySpawnEvent event) {
		Entity entity = event.getEntity();
		if (entity instanceof LivingEntity) {
			SwordEntityArbiter.register((LivingEntity) entity);
			SwordEntityArbiter.get(entity.getUniqueId()).resetResources();
		}
	}
	
	@EventHandler
	public void entityDeathEvent(EntityDeathEvent event) {
		SwordEntityArbiter.remove(event.getEntity().getUniqueId());
	}
	
	@EventHandler
	public void entityDamageEvent(EntityDamageEvent event) {
		if(event.getDamage() < 7474040) {
			event.setDamage(0.01);
			((LivingEntity) event.getEntity()).heal(100);
		}
	}
}
