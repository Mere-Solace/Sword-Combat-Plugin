package btm.sword.system.entity;

import btm.sword.system.playerdata.CombatProfile;
import org.bukkit.entity.LivingEntity;

import java.util.UUID;

public abstract class SwordEntity {
	protected LivingEntity associatedEntity;
	protected UUID uuid;
	
	protected CombatProfile combatProfile;
	
	protected int curShards;
	protected int effToughness;
	protected int curSoulfire;
	
	private boolean grabbed;
	
	public SwordEntity(LivingEntity associatedEntity) {
		this.associatedEntity = associatedEntity;
		uuid = associatedEntity.getUniqueId();
	}
	
	public LivingEntity entity() {
		return associatedEntity;
	}
	
	public UUID uuid() {
		return uuid;
	}
	
	public void setAssociatedEntity(LivingEntity entity) {
		this.associatedEntity = entity;
	}
	
	public CombatProfile getCombatProfile() {
		return combatProfile;
	}
	
	public boolean isGrabbed() {
		return grabbed;
	}
	
	public void setGrabbed(boolean grabbed) {
		this.grabbed = grabbed;
	}
}
