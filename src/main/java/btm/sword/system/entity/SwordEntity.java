package btm.sword.system.entity;

import org.bukkit.entity.LivingEntity;

import java.util.UUID;

public abstract class SwordEntity {
	LivingEntity associatedEntity;
	UUID uuid;
	
	public SwordEntity(LivingEntity associatedEntity) {
		this.associatedEntity = associatedEntity;
		uuid = associatedEntity.getUniqueId();
	}
	
	public UUID getUUID() {
		return uuid;
	}
	
	public LivingEntity getAssociatedEntity() {
		return associatedEntity;
	}
}
