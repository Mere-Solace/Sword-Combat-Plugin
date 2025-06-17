package btm.sword.system.entity;

import org.bukkit.entity.LivingEntity;

import java.util.UUID;

public abstract class SwordEntity {
	protected LivingEntity associatedEntity;
	private final UUID uuid;
	
	private boolean grabbed;
	
	public SwordEntity(LivingEntity associatedEntity) {
		this.associatedEntity = associatedEntity;
		uuid = associatedEntity.getUniqueId();
	}
	
	public LivingEntity entity() {
		return associatedEntity;
	}
	
	public void setAssociatedEntity(LivingEntity entity) {
		this.associatedEntity = entity;
	}
	
	
	public boolean isGrabbed() {
		return grabbed;
	}
	
	public void setGrabbed(boolean grabbed) {
		this.grabbed = grabbed;
	}
	
	public boolean onGround() {
		return associatedEntity.getWorld().rayTraceBlocks(associatedEntity.getLocation(), associatedEntity.getLocation().getDirection(), 0.2) != null;
	}
}
