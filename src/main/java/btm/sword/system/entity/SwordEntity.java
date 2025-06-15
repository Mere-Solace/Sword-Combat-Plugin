package btm.sword.system.entity;

import org.bukkit.entity.LivingEntity;
import org.bukkit.util.RayTraceResult;

import java.util.UUID;

public abstract class SwordEntity {
	protected LivingEntity associatedEntity;
	private final UUID uuid;
	
	private boolean beingGrabbed;
	
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
	
	
	public boolean isBeingGrabbed() {
		return beingGrabbed;
	}
	
	public void setBeingGrabbed(boolean beingGrabbed) {
		this.beingGrabbed = beingGrabbed;
	}
	
	public boolean onGround() {
		return associatedEntity.getWorld().rayTraceBlocks(associatedEntity.getLocation(), associatedEntity.getLocation().getDirection(), 0.2) != null;
	}
}
