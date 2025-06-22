package btm.sword.system.entity;

import btm.sword.Sword;
import btm.sword.system.combat.Affliction;
import btm.sword.system.playerdata.CombatProfile;
import btm.sword.system.playerdata.StatType;
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
	
	protected int maxShards;
	protected double maxToughness;
	protected double maxSoulfire;
	
	protected int curShards;
	protected double effToughness;
	protected double curSoulfire;
	
	protected boolean shouldRegen;
	
	private boolean grabbed;
	
	protected final HashMap<Class<? extends Affliction>, Affliction> afflictions;
	
	protected final double eyeHeight;
	protected final Vector chestVector;
	
	public SwordEntity(@NotNull LivingEntity self, @NotNull CombatProfile combatProfile) {
		this.self = self;
		uuid = self.getUniqueId();
		this.combatProfile = combatProfile;
		maxShards = combatProfile.getStat(StatType.SHARDS);
		maxToughness = combatProfile.getStat(StatType.TOUGHNESS);
		maxSoulfire = combatProfile.getStat(StatType.SOULFIRE);
		resetCombatStats();
		
		shouldRegen = true;
		regenTask();
		
		afflictions = new HashMap<>();
		
		eyeHeight = self.getEyeHeight(true);
		chestVector = new Vector(0, eyeHeight * 0.45, 0);
	}
	
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
	
	public void resetCombatStats() {
		curShards = maxShards;
		effToughness = maxToughness;
		curSoulfire = maxSoulfire;
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
	
	public void hit(Combatant source, int baseNumShards, double baseToughnessDamage, double baseSoulfireReduction, Vector knockbackVelocity, Affliction... afflictions) {
		if (self.getActiveItem().getType() != Material.SHIELD) {
			source.message("That lad is raisin 'is shield!");
		}

		curShards -= baseNumShards;
		if (curShards <= 0) {
			self.damage(77040, source.entity());
		}
		
		effToughness -= baseToughnessDamage;
		if (effToughness <= 0) {
			Cache.throwTrailParticle2.display(self.getLocation().add(chestVector));
		}
		
		curSoulfire -= baseSoulfireReduction;
		
		self.setVelocity(knockbackVelocity);
		
		for (Affliction affliction : afflictions) {
			affliction.start(this);
		}
		
		source.message("Hit that guy. He now has: " + curShards + " pure shards,  " + effToughness + " toughness,  " + " and " + curSoulfire + " soulfire.");
	}
	
	public void regenShards(int amount) {
		curShards = Math.min(curShards + amount, maxShards);
	}
	
	public void regenToughness(double amount) {
		effToughness = Math.min(effToughness + amount, maxToughness);
	}
	
	public void regenSoulfire(double amount) {
		curSoulfire = Math.min(curSoulfire + amount, curSoulfire);
	}
	
	// obviously un-tuned values rn, but it's working
	public void regenTask() {
		int[] step = {0};
		new BukkitRunnable() {
			@Override
			public void run() {
				if (shouldRegen) {
					if (step[0] % 3 == 0) {
						if (effToughness < maxToughness) regenToughness(0.5);
						Cache.testFlameParticle.display(entity().getEyeLocation());
						if (step[0] >= 10) {
							step[0] = 0;
							if (curShards < maxShards) regenShards(1);
						}
					}
					if (curSoulfire < maxSoulfire) regenSoulfire(0.25);
				}
				step[0]++;
			}
		}.runTaskTimer(Sword.getInstance(), 0L, 3L);
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
}
