package btm.sword.system.entity;

import btm.sword.Sword;
import btm.sword.system.combat.Affliction;
import btm.sword.system.playerdata.CombatProfile;
import btm.sword.util.Cache;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.UUID;

public abstract class SwordEntity {
	protected LivingEntity self;
	protected UUID uuid;
	protected CombatProfile combatProfile;
	
	protected EntityAspects aspects;
	
	private boolean grabbed;
	
	protected final HashMap<Class<? extends Affliction>, Affliction> afflictions;
	
	protected boolean toughnessBroken;
	
	protected final double eyeHeight;
	protected final Vector chestVector;
	
	public SwordEntity(@NotNull LivingEntity self, @NotNull CombatProfile combatProfile) {
		this.self = self;
		uuid = self.getUniqueId();
		
		this.combatProfile = combatProfile;
		aspects = new EntityAspects(combatProfile);
		
		afflictions = new HashMap<>();
		
		eyeHeight = self.getEyeHeight(true);
		chestVector = new Vector(0, eyeHeight * 0.45, 0);
	}
	
	public abstract void onSpawn();
	
	public abstract void onDeath();
	
	public LivingEntity entity() {
		return self;
	}
	
	public void setSelf(LivingEntity entity) {
		this.self = entity;
	}
	
	public UUID uuid() {
		return uuid;
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
	
	public Affliction getAffliction(Class<? extends Affliction> afflictionClass) {
		return afflictions.get(afflictionClass);
	}
	
	public void hit(Combatant source, int baseNumShards, float baseToughnessDamage, float baseSoulfireReduction, Vector knockbackVelocity, Affliction... afflictions) {
		if (self.getActiveItem().getType() != Material.SHIELD) {
			source.message("That lad is raisin 'is shield!");
		}
		
		self.damage(0.01);
		self.heal(7474040);
		
		if (aspects.toughness().remove(baseToughnessDamage) && !toughnessBroken) {
			Cache.toughnessBreakParticle1.display(getChestLocation());
			onToughnessBroken();
		}
		
		// remove returns true only if the value reaches or goes below 0
		if (toughnessBroken) {
			if (aspects.shards().remove(baseNumShards)) {
				self.damage(74077740, source.entity());
				if (!self.isDead())
					self.setHealth(0);
			}
		}
		
		aspects.soulfire().remove(baseSoulfireReduction);

		self.setVelocity(knockbackVelocity);

		for (Affliction affliction : afflictions) {
			affliction.start(this);
		}

		source.message("Hit that guy. He now has: " + aspects.shards().cur() + " pure shards,  "
				+ aspects.toughness().cur() + " toughness,  "
				+ " and " + aspects.soulfire().cur() + " soulfire.");
	}
	
	public void resetResources() {
		aspects.shards().reset();
		aspects.toughness().reset();
		aspects.soulfire().reset();
		aspects.soulfire().reset();
		message("Reset resources:\n" + aspects.curResources());
	}
	
	public void onToughnessBroken() {
		toughnessBroken = true;
		aspects.toughness().setEffAmountPercent(2f);
		aspects.toughness().setEffPeriodPercent(0.2f);
		new BukkitRunnable() {
			@Override
			public void run() {
				if (aspects.toughness().curPercent() > 0.6) {
					aspects.toughness().setEffAmountPercent(1f);
					aspects.toughness().setEffPeriodPercent(1f);
					toughnessBroken = false;
					Location c = getChestLocation();
					Cache.toughnessRechargeParticle.display(c);
					Cache.toughnessRechargeParticle2.display(c);
					cancel();
				}
			}
		}.runTaskTimer(Sword.getInstance(), 0L, 2L);
	}
	
	public double getEyeHeight() {
		return eyeHeight;
	}
	
	public Vector getChestVector() {
		return chestVector;
	}
	
	public Location getChestLocation() {
		return self.getLocation().add(chestVector);
	}
	
	public void message(String message) {
		self.sendMessage(message);
	}
}
