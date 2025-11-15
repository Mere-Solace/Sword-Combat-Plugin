package btm.sword.system.entity.base;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;

import btm.sword.Sword;
import btm.sword.system.combat.Affliction;
import btm.sword.system.entity.SwordEntityArbiter;
import btm.sword.system.entity.aspect.AspectType;
import btm.sword.system.entity.types.Combatant;
import btm.sword.util.Prefab;
import btm.sword.util.display.DrawUtil;
import btm.sword.util.entity.EntityUtil;
import btm.sword.util.entity.HitboxUtil;
import btm.sword.util.math.VectorUtil;
import btm.sword.util.sound.SoundType;
import btm.sword.util.sound.SoundUtil;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

/**
 * Abstract base class representing an entity in the Sword plugin system.
 * This class wraps a {@link LivingEntity} and provides core combat-related functionality,
 * resource management via {@link EntityAspects}, affliction handling via {@link Affliction},
 * and interaction utilities.
 * <p>
 * Subclasses are expected to implement {@link #onDeath()} to define death behavior.
 * </p>
 */
@Getter
@Setter
public abstract class SwordEntity {
    protected final UUID uuid;
    protected final CombatProfile combatProfile;
    protected LivingEntity self;
    protected String displayName;

    protected EntityAspects aspects;

    /** Boolean value for whether onTick() should be run or not */
    protected boolean shouldTick;
    protected long ticks;

    private TextDisplay statusDisplay;
    private boolean statusActive;

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

    // TODO: override all getters of vectors and locations to return a .clone() of that instance
    protected final double eyeHeight;
    protected final Vector chestVector;

    protected boolean ableToPickup;

    protected List<Vector> currentEyeDirectionBasis;
    protected List<Vector> currentBodyDirectionBasis;
    protected long timeOfLastEyeBasisCalculation;
    protected long timeOfLastBodyBasisCalculation;

    /**
     * Constructs a new SwordEntity wrapping the specified {@link LivingEntity} and combat profile.
     * Initializes resources, afflictions, and starts ticking updates.
     *
     * @param self the Bukkit {@link LivingEntity} to wrap
     * @param combatProfile the {@link CombatProfile} associated with this entity
     */
    public SwordEntity(@NotNull LivingEntity self, @NotNull CombatProfile combatProfile) {
        this.self = self;
        uuid = self.getUniqueId();
        displayName = self.getName();

        this.combatProfile = combatProfile;
        aspects = new EntityAspects(combatProfile);

        shouldTick = true;
        ticks = 0L;

        statusActive = true;

        timeOfLastAttack = 0L;
        durationOfLastAttack = 0;

        grabbed = false;
        hit = false;

        shielding = false;

        afflictions = new HashMap<>();

        eyeHeight = self.getEyeHeight(true);
        chestVector = new Vector(0, eyeHeight * 0.45, 0);

        ableToPickup = true;

        timeOfLastEyeBasisCalculation = 0L;

        startTicking();
    }

    /**
     * Starts a {@link BukkitRunnable} task that calls {@link #onTick()} every server tick (20 times per second).
     * Controls the continuous update logic for this entity.
     */
    private void startTicking() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (shouldTick) {
                    onTick();
                }
                ticks++;
            }
        }.runTaskTimer(Sword.getInstance(), 0L, 1L);
    }

    /**
     * Called on every server tick if ticking is enabled.
     * Manages invulnerability timers, AI enabling/disabling, grounding state, and dash resets.
     * <p>
     * For players, resets air dashes if grounded every 3 ticks.
     * For other entities, disables AI if pinned.
     * </p>
     */
    protected void onTick() {
        if (hit) {
            curTicksInvulnerable++;
            if (curTicksInvulnerable >= hitInvulnerableTickDuration) {
                hit = false;
                curTicksInvulnerable = 0;
            }
        }
        if (!(self instanceof Player)) {
            self.setAI(!pinned);
        }
        else {
            if (ticks % 3 == 0) {
                grounded = EntityUtil.isOnGround(self);
                if (grounded && this instanceof Combatant c) {
                    c.resetAirDashesPerformed();
                }
            }
        }

        if (statusDisplay != null && isStatusActive()) {
            updateStatusDisplayText();
        }

        if ((statusDisplay == null || statusDisplay.isDead()) && isStatusActive()) {
            restartStatusDisplay();
        }
    }

    private void updateStatusDisplayText() {
        int shards = (int) aspects.shardsCur();
        int maxEffShards = (int) aspects.shardsVal();
        float toughness = aspects.toughnessCur();
        float maxEffToughness = aspects.toughnessVal();

        String bar = "█".repeat(shards);
        TextComponent filledHealth = Component.text(bar, TextColor.color(5, 200, 7));

        String rest = "░".repeat(maxEffShards - shards);
        TextComponent unfilledHealth = Component.text(rest, TextColor.color(170, 170, 170));

        Component displayText = Component.text()
                .append(Component.text(getDisplayName() + "\n", NamedTextColor.AQUA, TextDecoration.BOLD))
                .append(Component.text(String.format("%d/%d HP\n", shards, maxEffShards)))
                .append(Component.text("|[", NamedTextColor.GRAY))
                .append(filledHealth)
                .append(unfilledHealth)
                .append(Component.text("]|\n", NamedTextColor.GRAY))
                .append(Component.text(String.format("%.0f/%.0f Toughness", toughness, maxEffToughness), NamedTextColor.GOLD))
                .build();

        statusDisplay.text(displayText);
    }

    private void restartStatusDisplay() {
        if (!entity().isValid() || (statusDisplay != null && !statusDisplay.isDead()))
            return;

        setStatusActive(false);

        if (!(entity() instanceof LivingEntity living) || living instanceof ArmorStand) return;
        if (entity().getType() == EntityType.ITEM_DISPLAY || entity().getType() == EntityType.ITEM) return;

        statusDisplay = (TextDisplay) entity().getWorld().spawnEntity(entity().getEyeLocation().setDirection(Prefab.Direction.NORTH), EntityType.TEXT_DISPLAY);
        statusDisplay.addScoreboardTag("remove_on_shutdown");
        statusDisplay.setNoPhysics(true);
        statusDisplay.setBillboard(Display.Billboard.CENTER);
        statusDisplay.setTransformation(
                new Transformation(
                        new Vector3f(0, 0.1f, 0),
                        new Quaternionf(),
                        new Vector3f(0.75f,0.75f,0.75f),
                        new Quaternionf()
                )
        );
        statusDisplay.setShadowed(true);
        var displayConfig = btm.sword.config.ConfigManager.getInstance().getDisplay();
        statusDisplay.setBrightness(new Display.Brightness(
            displayConfig.getStatusDisplayBlockBrightness(),
            displayConfig.getStatusDisplaySkyBrightness()
        ));
        statusDisplay.setPersistent(false);

        updateStatusDisplayText();

        entity().addPassenger(statusDisplay);
        statusDisplay.setBillboard(Display.Billboard.VERTICAL);

        if (entity() instanceof Player p) {
            p.hideEntity(Sword.getInstance(), statusDisplay);
        }

        setStatusActive(true);
    }

    public void endStatusDisplay() {
        setStatusActive(false);
        removeStatusDisplay();
    }

    private void removeStatusDisplay() {
        if (statusDisplay == null) return;
        Entity display = statusDisplay;
        statusDisplay = null;

        if (!Sword.getInstance().isEnabled()) return;

        new BukkitRunnable() {
            int attempts = 0;

            @Override
            public void run() {
                if (!display.isValid()) {
                    cancel();
                    return;
                }

                // Check if the chunk is loaded and not in transition
                if (display.getWorld().isChunkLoaded(display.getLocation().getBlockX() >> 4, display.getLocation().getBlockZ() >> 4)) {
                    try {
                        display.remove();
                    } catch (Throwable ignored) {
                        attempts++;
                        if (attempts > 20) cancel();
                        return;
                    }
                    cancel();
                } else {
                    display.getChunk().load();
                }

                attempts++;
                if (attempts > 40) cancel();
            }
        }.runTaskTimer(Sword.getInstance(), 1L, 2L);
    }

    public void onRegister() {
        new BukkitRunnable() {
            @Override
            public void run() {
                restartStatusDisplay();
            }
        }.runTaskLater(Sword.getInstance(), 2L);
    }

    /**
     * Called when this entity is spawned or re-spawned.
     * Resets resources and tick counter.
     */
    public void onSpawn() {
        ticks = 0;
        setShouldTick(true);
        resetResources();
    }

    /**
     * Clean up for use in {@link btm.sword.listeners.EntityListener#entityRemoveEvent(EntityRemoveFromWorldEvent)}.
     */
    public void onDeath() {
        endStatusDisplay();
        setShouldTick(false);
        aspects.stopAllResourceTasks();
    }

    /**
     * Gets the underlying {@link LivingEntity} wrapped by this SwordEntity.
     *
     * @return the Bukkit living entity
     */
    public LivingEntity entity() {
        return self;
    }

    public boolean isValid() {
        return entity().isValid();
    }

    /**
     * Gets the unique identifier of this entity.
     *
     * @return the UUID of the entity
     */
    public UUID getUniqueId() {
        return uuid;
    }

    /**
     * Increments the count of impalements on this entity.
     */
    public void addImpalement() {
        numberOfImpalements++;
    }

    /**
     * Decrements the count of impalements on this entity.
     */
    public void removeImpalement() {
        numberOfImpalements--;
    }

    /**
     * Checks if this entity is currently impaled (has one or more impalements).
     *
     * @return true if impaled, false otherwise
     */
    public boolean isImpaled() {
        return numberOfImpalements > 0;
    }

    /**
     * Retrieves an active affliction of the specified class from this entity.
     *
     * @param afflictionClass the class of affliction to retrieve
     * @return the affliction instance or null if none present
     */
    public Affliction getAffliction(Class<? extends Affliction> afflictionClass) {
        return afflictions.get(afflictionClass);
    }

    /**
     * Applies a hit to this entity from a given source {@link Combatant}, triggering resource damage,
     * invulnerability, knockback, afflictions, and toughness breaking effects.
     * <p>
     * If the entity is currently invulnerable due to a recent hit, this method does nothing.
     * Also manages shard loss and potential death of the entity if toughness is broken.
     * </p>
     *
     * @param source the {@link Combatant} causing the hit
     * @param hitInvulnerableTickDuration duration of invulnerability in ticks after this hit
     * @param baseNumShards base number of shards to remove from the entity
     * @param baseToughnessDamage base toughness damage to apply
     * @param baseSoulfireReduction reduction of the soulfire resource
     * @param knockbackVelocity velocity vector to apply knockback
     * @param afflictions optional afflictions to apply from the hit
     */
    public void hit(Combatant source, long hitInvulnerableTickDuration, int baseNumShards, float baseToughnessDamage, float baseSoulfireReduction, Vector knockbackVelocity, Affliction... afflictions) {
        if (hit)
            return;
        else
            hit = true;
        this.hitInvulnerableTickDuration = hitInvulnerableTickDuration;

        self.damage(0.01);

        Prefab.Particles.TEST_HIT.display(getChestLocation());
        SoundUtil.playSound(source.entity(), SoundType.ENTITY_PLAYER_ATTACK_STRONG, 0.9f, 1f);

        if (aspects.toughness().remove(baseToughnessDamage)) {
            if (!toughnessBroken) {
                Prefab.Particles.TOUGH_BREAK_1.display(getChestLocation());
                onToughnessBroken();
            }
            self.playHurtAnimation(0);
            displayShardLoss();
            aspects.restartResourceProcessAfterDelay(AspectType.SHARDS);
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
    }

    /**
     * Displays visual effects related to shard loss.
     * Intended to be overridden in subclasses.
     */
    public void displayShardLoss() {

    }

    /**
     * Resets this entity's combat resources (shards, toughness, soulfire) to their defaults.
     * Also sends a message to the entity displaying current resource values.
     */
    public void resetResources() {
        aspects.shards().reset();
        aspects.toughness().reset();
        aspects.soulfire().reset();
        aspects.soulfire().reset();
        message("Reset resources:\n" + aspects.curResources());
    }

    /**
     * Called when the entity's toughness breaks. Adjusts effectiveness percentages and
     * starts a repeating task to monitor toughness recharge and reset state.
     */
    public void onToughnessBroken() {
        toughnessBroken = true;
        aspects.toughness().setEffAmountPercent(2f);
        aspects.toughness().setEffPeriodPercent(0.2f);
        new BukkitRunnable() {
            @Override
            public void run() {
                if (self == null || self.isDead()) {
                    aspects.toughness().setEffAmountPercent(1f);
                    aspects.toughness().setEffPeriodPercent(1f);
                    toughnessBroken = false;
                    cancel();
                }

                if (aspects.toughness().curPercent() > 0.6) {
                    aspects.toughness().setEffAmountPercent(1f);
                    aspects.toughness().setEffPeriodPercent(1f);
                    toughnessBroken = false;
                    Location c = getChestLocation();
                    Prefab.Particles.TOUGH_RECHARGE_1.display(c);
                    Prefab.Particles.TOUGH_RECHARGE_2.display(c);
                    cancel();
                }
            }
        }.runTaskTimer(Sword.getInstance(), 0L, 2L);
    }

    /**
     * Gets the approximate chest location of the entity by adding a chest offset vector
     * to the entity's current location.
     *
     * @return the {@link Location} representing the entity's chest position
     */
    public Location getChestLocation() {
        return self.getLocation().add(chestVector);
    }

    /**
     * Sends a chat message to this entity if it is a player.
     *
     * @param message the message string to send
     */
    public void message(String message) {
        self.sendMessage(message);
    }

    /**
     * Gives an {@link ItemStack} to this entity.
     * <p>
     * If the entity is a player, attempts to place the item in main hand, off hand,
     * or inventory; if none available, drops the item near them with particle effects.
     * For non-player entities, the item is equipped in main hand.
     * </p>
     *
     * @param itemStack the item stack to give
     */
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

            ItemStack[] contents = inv.getStorageContents();
            for (int slot = 0; slot < contents.length; slot++) {
//				if (slot >= 36 && slot <= 39) continue;

                ItemStack slotItem = contents[slot];
                if (slotItem == null || slotItem.getType().isAir()) {
                    inv.setItem(slot, itemStack);
                    return;
                }
            }

            Item dropped = p.getWorld().dropItemNaturally(p.getLocation(), itemStack);
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (dropped.isDead()) {
                        cancel();
                    }
                    Prefab.Particles.DOPPED_ITEM_MARKER.display(dropped.getLocation());
                }
            }.runTaskTimer(Sword.getInstance(), 0L, 5L);
        }
        else {
            Objects.requireNonNull(self.getEquipment()).setItemInMainHand(itemStack);
        }
    }

    /**
     * Gets the {@link ItemStack} held in the main or offhand of this entity.
     *
     * @param main true for main hand, false for offhand
     * @return the held {@link ItemStack}
     */
    public ItemStack getItemStackInHand(boolean main) {
        if (self instanceof Player p) {
            return main ? p.getInventory().getItemInMainHand() : p.getInventory().getItemInOffHand();
        }
        return main ? Objects.requireNonNull(self.getEquipment()).getItemInMainHand() : Objects.requireNonNull(self.getEquipment()).getItemInOffHand();
    }

    /**
     * Gets the {@link Material} type of the item held in the main or off hand of this entity.
     *
     * @param main true for main hand, false for off hand
     * @return the {@link Material} type held
     */
    public Material getItemTypeInHand(boolean main) {
        return getItemStackInHand(main).getType();
    }

    /**
     * Sets the {@link ItemStack} held in the main or off hand of this entity.
     *
     * @param itemStack the item stack to set
     * @param main true for main hand, false for off hand
     */
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

    /**
     * Sets the item type held in the main or off hand using a {@link Material}.
     * Creates a new {@link ItemStack} of the specified type.
     *
     * @param itemType the {@link Material} type to set
     * @param main true for main hand, false for off hand
     */
    public void setItemTypeInHand(Material itemType, boolean main) {
        setItemStackInHand(new ItemStack(itemType), main);
    }

    public void setItemInInventory(int index, ItemStack item) {
        if (entity() instanceof Player p) {
            p.getInventory().setItem(index, item);
        } else setItemStackInHand(item, index == 0);
    }

    /**
     * Checks if the entity does not have an item in its main hand.
     *
     * @return true if main hand is empty, false otherwise
     */
    public boolean isMainHandEmpty() {
        return getItemStackInHand(true).isEmpty();
    }

    /**
     * Checks if the entity is dead or effectively dead (no shards remaining).
     *
     * @return true if dead or shards depleted, false otherwise
     */
    public boolean isDead() {
        return self.isDead() || aspects.shards().cur() == 0;
    }

    /**
     * Returns the flat directional vector based on the entity's eye yaw angle.
     *
     * @return a horizontal facing {@link Vector} based on the eye direction
     */
    public Vector getFlatDir() {
        double yawRads = Math.toRadians(self.getEyeLocation().getYaw());
        return new Vector(-Math.sin(yawRads), 0, Math.cos(yawRads));
    }

    /**
     * Returns the flat directional vector based on the entity's body yaw angle.
     *
     * @return a horizontal facing {@link Vector} based on the body direction
     */
    public Vector getFlatBodyDir() {
        double yawRads = Math.toRadians(self.getBodyYaw());
        return new Vector(-Math.sin(yawRads), 0, Math.cos(yawRads));
    }

    /**
     * Sets the velocity of this entity.
     *
     * @param v the velocity {@link Vector} to set
     */
    public void setVelocity(Vector v) {
        self.setVelocity(v);
    }

    public SwordEntity getTargetedEntity(double range) {
        LivingEntity target = (LivingEntity) HitboxUtil.ray(
                entity().getEyeLocation(), entity().getEyeLocation().getDirection(), range, 1,
                entity -> entity instanceof LivingEntity e &&
                        !(e.getUniqueId() == getUniqueId()) &&
                        e.isValid());

        return target == null ? null :SwordEntityArbiter.getOrAdd(target.getUniqueId());
    }

    public Vector rightBasisVector(boolean withPitch) {
        if (withPitch) {
            calcEyeDirBasis();
            return currentEyeDirectionBasis.getFirst().clone();
        }
        calcBodyDirBasis();
        return currentBodyDirectionBasis.getFirst().clone();
    }

    public Vector upBasisVector(boolean withPitch) {
        if (withPitch) {
            calcEyeDirBasis();
            return currentEyeDirectionBasis.get(1).clone();
        }
        calcBodyDirBasis();
        return currentBodyDirectionBasis.get(1).clone();
    }

    public Vector forwardBasisVector(boolean withPitch) {
        if (withPitch) {
            calcEyeDirBasis();
            return currentEyeDirectionBasis.getLast().clone();
        }
        calcBodyDirBasis();
        return currentBodyDirectionBasis.getLast().clone();
    }

    private void calcEyeDirBasis() {
        if (currentEyeDirectionBasis == null || System.currentTimeMillis() - timeOfLastEyeBasisCalculation > 5) {
            updateEyeDirectionBasis();
        }
    }

    private void calcBodyDirBasis() {
        if (currentBodyDirectionBasis == null || System.currentTimeMillis() - timeOfLastBodyBasisCalculation > 5) {
            updateBodyDirectionBasis();
        }
    }

    private void updateEyeDirectionBasis() {
        currentEyeDirectionBasis = VectorUtil.getBasis(entity().getEyeLocation(), entity().getEyeLocation().getDirection());
        timeOfLastEyeBasisCalculation = System.currentTimeMillis();
    }

    private void updateBodyDirectionBasis() {
        currentBodyDirectionBasis = VectorUtil.getBasisWithoutPitch(entity());
        timeOfLastBodyBasisCalculation = System.currentTimeMillis();
    }

    public void drawBasis() {
        List<Vector> testBasis = VectorUtil.getBasisWithoutPitch(entity());
        DrawUtil.line(List.of(Prefab.Particles.TEST_SWORD_BLUE),
                entity().getEyeLocation(), testBasis.getFirst(), 4, 0.25);
        DrawUtil.line(List.of(Prefab.Particles.TEST_SWORD_BLUE),
                entity().getEyeLocation(), testBasis.get(1), 4, 0.25);
        DrawUtil.line(List.of(Prefab.Particles.TEST_SWORD_BLUE),
                entity().getEyeLocation(), testBasis.getLast(), 4, 0.25);
    }
}
