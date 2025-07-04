package btm.sword.system.entity;

import btm.sword.Sword;
import btm.sword.system.combat.Affliction;
import btm.sword.system.playerdata.CombatProfile;
import btm.sword.util.Cache;
import btm.sword.util.EntityUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;

public abstract class SwordEntity {
	protected LivingEntity self;
	protected UUID uuid;
	protected CombatProfile combatProfile;
	
	protected EntityAspects aspects;
	
	private boolean tick;
	private long ticks;
	
	private long timeOfLastAttack;
	private int durationOfLastAttack;
	
	private boolean grounded;
	
	private boolean hit;
	private long curTicksInvulnerable;
	private long hitInvulnerableTickDuration;
	
	private boolean grabbed;
	private int numberOfImpalements;
	private boolean pinned;
	private boolean aiEnabled;
	
	protected boolean shielding;
	
	protected final HashMap<Class<? extends Affliction>, Affliction> afflictions;
	
	protected boolean toughnessBroken;
	protected int shardsLost;
	
	protected final double eyeHeight;
	protected final Vector chestVector;
	
	protected boolean ableToPickup;
	
	public SwordEntity(@NotNull LivingEntity self, @NotNull CombatProfile combatProfile) {
		this.self = self;
		uuid = self.getUniqueId();
		
		this.combatProfile = combatProfile;
		aspects = new EntityAspects(combatProfile);
		
		tick = true;
		ticks = 0L;
		
		timeOfLastAttack = 0L;
		durationOfLastAttack = 0;
		
		grabbed = false;
		hit = false;
		
		shielding = false;
		
		afflictions = new HashMap<>();
		
		eyeHeight = self.getEyeHeight(true);
		chestVector = new Vector(0, eyeHeight * 0.45, 0);
		
		ableToPickup = true;
		
		startTicking();
	}
	
	private void startTicking() {
		new BukkitRunnable() {
			@Override
			public void run() {
				if (tick) {
					onTick();
				}
				ticks++;
			}
		}.runTaskTimer(Sword.getInstance(), 0L, 1L);
	}
	
	protected void onTick() {
		if (hit) {
			curTicksInvulnerable++;
			if (curTicksInvulnerable >= hitInvulnerableTickDuration) {
				hit = false;
				curTicksInvulnerable = 0;
			}
		}
		if (!(self instanceof Player)) {
			self.setAI(!isPinned());
		}
		else {
			if (ticks % 3 == 0) {
				grounded = EntityUtil.isOnGround(self);
				if (grounded && this instanceof Combatant c) {
					c.resetAirDashesPerformed();
				}
			}
		}
	}
	
	public void onSpawn() {
		resetResources();
		ticks = 0;
	}
	
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
	
	public boolean isTick() {
		return tick;
	}
	
	public void setTick(boolean tick) {
		this.tick = tick;
	}
	
	public boolean isGrounded() {
		return grounded;
	}
	
	public boolean isGrabbed() {
		return grabbed;
	}
	
	public void setGrabbed(boolean grabbed) {
		this.grabbed = grabbed;
	}
	
	public boolean isHit() {
		return hit;
	}
	
	public void setHit(boolean hit) {
		this.hit = hit;
	}
	
	public void addImpalement() {
		numberOfImpalements++;
	}
	
	public void removeImpalement() {
		numberOfImpalements--;
	}
	
	public boolean isImpaled() {
		return numberOfImpalements > 0;
	}
	
	public int getNumberOfImpalements() {
		return numberOfImpalements;
	}
	
	public boolean isPinned() {
		return pinned;
	}
	
	public void setPinned(boolean pinned) {
		this.pinned = pinned;
	}
	
	public boolean isAiEnabled() {
		return aiEnabled;
	}
	
	public void setAiEnabled(boolean aiEnabled) {
		this.aiEnabled = aiEnabled;
	}
	
	public Affliction getAffliction(Class<? extends Affliction> afflictionClass) {
		return afflictions.get(afflictionClass);
	}
	
	public void hit(Combatant source, long hitInvulnerableTickDuration, int baseNumShards, float baseToughnessDamage, float baseSoulfireReduction, Vector knockbackVelocity, Affliction... afflictions) {
//		if (self.getActiveItem().getType() != Material.SHIELD) {
//			source.message("That lad is raisin 'is shield!");
//		}
		if (hit)
			return;
		else
			hit = true;
		this.hitInvulnerableTickDuration = hitInvulnerableTickDuration;
		
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
				return;
			}
			shardsLost += baseNumShards;
			
			if (shardsLost >= 0.75 * aspects.shards().effectiveValue()) {
				aspects.toughness().setCurPercent(0.9f);
			}
		}
		
		aspects.soulfire().remove(baseSoulfireReduction);

		self.setVelocity(knockbackVelocity);

		for (Affliction affliction : afflictions) {
			affliction.start(this);
		}
//
//		source.message("Hit that guy. He now has: " + aspects.shards().cur() + " pure shards,  "
//				+ aspects.toughness().cur() + " toughness,  "
//				+ " and " + aspects.soulfire().cur() + " soulfire.");
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
				if (self == null || self.isDead())
					cancel();
				
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
	
	public long getTimeOfLastAttack() {
		return timeOfLastAttack;
	}
	
	public void setTimeOfLastAttack(long timeOfLastAttack) {
		this.timeOfLastAttack = timeOfLastAttack;
	}
	
	public int getDurationOfLastAttack() {
		return durationOfLastAttack;
	}
	
	public void setDurationOfLastAttack(int durationOfLastAttack) {
		this.durationOfLastAttack = durationOfLastAttack;
	}

	public void giveItem(ItemStack itemStack) {
		if (self instanceof Player p) {
			PlayerInventory inv = p.getInventory();
			
			ItemStack mainHand = inv.getItemInMainHand();
			if (mainHand.getType().isAir()) {
				inv.setItemInMainHand(itemStack);
				return;
			}
			
			ItemStack offHand = inv.getItemInOffHand();
			if (offHand.getType().isAir()) {
				inv.setItemInOffHand(itemStack);
				return;
			}
			
			ItemStack[] contents = inv.getContents();
			for (int slot = 0; slot < contents.length; slot++) {
				if (slot >= 36 && slot <= 39) continue;
				
				ItemStack slotItem = contents[slot];
				if (slotItem == null || slotItem.getType().isAir()) {
					inv.setItem(slot, itemStack);
					return;
				}
			}
		}
		else {
			Objects.requireNonNull(self.getEquipment()).setItemInMainHand(itemStack);
		}
	}
	
	public ItemStack getItemStackInHand(boolean main) {
		if (self instanceof Player p) {
			return main ? p.getInventory().getItemInMainHand() : p.getInventory().getItemInOffHand();
		}
		return main ? Objects.requireNonNull(self.getEquipment()).getItemInMainHand() : Objects.requireNonNull(self.getEquipment()).getItemInOffHand();
	}
	
	public Material getItemTypeInHand(boolean main) {
		return getItemStackInHand(main).getType();
	}
	
	public void setItemStackInHand(ItemStack itemStack, boolean main) {
		if (self instanceof Player) {
			if (main)
				((Player) self).getInventory().setItemInMainHand(itemStack);
			else
				((Player) self).getInventory().setItemInOffHand(itemStack);
		}
		else {
			if (main)
				Objects.requireNonNull(self.getEquipment()).setItemInMainHand(itemStack);
			else
				Objects.requireNonNull(self.getEquipment()).setItemInOffHand(itemStack);
		}
	}
	
	public void setItemTypeInHand(Material itemType, boolean main) {
		setItemStackInHand(new ItemStack(itemType), main);
	}
	
	public boolean isAbleToPickup() {
		return ableToPickup;
	}
	
	public void setAbleToPickup(boolean ableToPickup) {
		this.ableToPickup = ableToPickup;
	}
	
	public boolean isDead() {
		return self.isDead() || aspects.shards().cur() == 0;
	}
	
	public Vector getFlatDir() {
		double yawRads = Math.toRadians(self.getEyeLocation().getYaw());
		return new Vector(-Math.sin(yawRads), 0, Math.cos(yawRads));
	}
	
	public Vector getFlatBodyDir() {
		double yawRads = Math.toRadians(self.getBodyYaw());
		return new Vector(-Math.sin(yawRads), 0, Math.cos(yawRads));
	}
	
	public void setVelocity(Vector v) {
		self.setVelocity(v);
	}
}
