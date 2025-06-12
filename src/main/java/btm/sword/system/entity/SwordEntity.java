package btm.sword.system.entity;

import org.bukkit.entity.LivingEntity;

import java.util.UUID;

public abstract class SwordEntity {
	protected LivingEntity associatedEntity;
	private UUID uuid;
	
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
	
	public void setAssociatedEntity(LivingEntity entity) {
		this.associatedEntity = entity;
	}
}
