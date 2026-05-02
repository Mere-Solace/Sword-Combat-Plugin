package btm.sword.system.entity.base;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;

import btm.sword.Sword;
import btm.sword.config.Config;
import btm.sword.control.EntityController;
import btm.sword.control.PredicateRunnablePair;
import btm.sword.control.SwordScheduler;
import btm.sword.control.TimeArbiter;
import btm.sword.input.ActivationContext;
import btm.sword.system.action.BlockAction;
import btm.sword.system.action.throwing.impale.Impalement;
import btm.sword.system.action.throwing.types.DroppedItem;
import btm.sword.system.attack.HitValuePacket;
import btm.sword.system.attack.simulation.EntitySnapshotMap;
import btm.sword.system.combat.Affliction;
import btm.sword.system.entity.SwordEntityArbiter;
import btm.sword.system.entity.SwordTeam;
import btm.sword.system.entity.aspect.AspectType;
import btm.sword.system.entity.impl.Combatant;
import btm.sword.system.entity.impl.SwordPlayer;
import btm.sword.utility.Debug;
import btm.sword.utility.Prefab;
import btm.sword.utility.SwordTimeUnit;
import btm.sword.utility.entity.EntityUtil;
import btm.sword.utility.entity.HitboxUtil;
import btm.sword.utility.math.Basis;
import btm.sword.utility.math.VectorUtil;
import btm.sword.utility.sound.SoundUtil;
import btm.sword.utility.sound.SwordSoundType;
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
    private static final PotionEffect IMPALE_SLOW = new PotionEffect(PotionEffectType.SLOWNESS, 1, 1);

    protected final UUID uuid;
    protected final CombatProfile combatProfile;
    protected final LivingEntity self;
    protected String displayName;
    protected boolean destroyed;

    protected boolean dead;

    protected EntityAspects aspects;

    /** Boolean value for whether onTick() should be run or not */
    protected boolean shouldTick;
    protected long ticks;

    protected TextDisplay statusDisplay;
    protected boolean statusActive;

    private int prevDisplayShards = -1;
    private float prevDisplayToughness = -1f;

    private long timeOfLastAttack;
    /**
     * in milliseconds
     */
    private int durationOfLastAttack;

    private boolean grounded;

    private boolean hit;
    private long curTicksInvulnerable;
    private long hitInvulnerableTickDuration;

    private boolean grabbed;
    private boolean aiEnabled;

    private SwordTeam cachedTeam;

    protected boolean shielding;

    protected final HashMap<Class<? extends Affliction>, Affliction> afflictions = new HashMap<>();
    protected final Set<Impalement> impalements = new HashSet<>();

    protected boolean toughnessBroken;
    protected int shardsLostDuringToughnessBreak;

    protected final double eyeHeight;
    protected final Vector chestVector;

    protected boolean ableToPickup;

    protected Basis currentEyeDirectionBasis;
    protected Basis currentBodyDirectionBasis;
    protected long timeOfLastEyeBasisCalculation;
    protected long timeOfLastBodyBasisCalculation;

    protected double bodyLength;
    protected double bodyWidth;
    protected double averageSize;

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

        eyeHeight = self.getEyeHeight(true);
        chestVector = new Vector(0, eyeHeight * 0.45, 0);

        ableToPickup = true;

        timeOfLastEyeBasisCalculation = 0L;

        BoundingBox bb = self.getBoundingBox();
        bodyWidth = bb.getWidthX();
        bodyLength = bb.getWidthZ();
        averageSize = (bodyWidth + bodyLength) / 2;

        startTicking();
    }

    /**
     * Starts a {@link BukkitRunnable} task that calls {@link #onTick()} every server tick (20 times per second).
     * Controls the continuous update logic for this entity.
     */
    private void startTicking() {
        TimeArbiter.runTimeIndependentBukkitTaskOnTimer(
          null,
            () -> {
              if (shouldTick) onTick();
              ticks++;
            },
            0, 50,
            SwordEntity.class, "startTicking",
            new PredicateRunnablePair(
                this::isDestroyed,
                () -> Debug.system("tick task ending")
            )
        );
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
//        if (!(self instanceof Player)) {
////            self.setAI(!isPinned()); // TODO: #160 remake later
//        }

        if (ticks % 3 == 0) {
            grounded = EntityUtil.isOnGround(self);
            if (grounded && this instanceof Combatant c) {
                c.resetAirDashesPerformed();
            }
        }


        if (isImpaled()) {
            addPotionEffect(IMPALE_SLOW);
        }

        if (statusDisplay != null && isStatusActive()) {
            updateStatus();
        }

        if ((statusDisplay == null || statusDisplay.isDead()) && isStatusActive()) {
            restartStatusDisplay();
        }

        EntitySnapshotMap.INSTANCE.snapshot(uuid, self.getBoundingBox(), self.getYaw(), self.getPitch());
    }

    /** Apply a potion effect to the underlying entity. */
    public void addPotionEffect(PotionEffect effect) {
        self.addPotionEffect(IMPALE_SLOW);
    }

    protected void updateStatus() {
        updateStatusDisplayText();
    }

    protected void updateStatusDisplayText() {
        int shards = (int) aspects.shardsCur();
        float toughness = aspects.toughnessCur();

        if (shards == prevDisplayShards && toughness == prevDisplayToughness) {
            return;
        }

        prevDisplayShards = shards;
        prevDisplayToughness = toughness;

        int maxEffShards = (int) aspects.shardsMaxVal();
        float maxEffToughness = aspects.toughnessMaxVal();

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

        if (self() instanceof Player p) {
            p.hideEntity(Sword.getInstance(), statusDisplay);
        }
    }

    protected void restartStatusDisplay() {
        if (!self().isValid() || (statusDisplay != null && !statusDisplay.isDead()))
            return;

        setStatusActive(false);

//        if (self().getType() == EntityType.ITEM_DISPLAY || self().getType() == EntityType.ITEM) return;

        statusDisplay = (TextDisplay) self().getWorld().spawnEntity(eyeLoc().setDirection(Config.Direction.north()), EntityType.TEXT_DISPLAY);
        if (self() instanceof Player p) {
            p.hideEntity(Sword.getInstance(), statusDisplay);
        }
        statusDisplay.addScoreboardTag("remove_on_shutdown");
        statusDisplay.setNoPhysics(true);
        statusDisplay.setBillboard(Display.Billboard.CENTER);
        statusDisplay.setTransformation(
                new Transformation(
                        new Vector3f(0, 0.1f, 0),
                        new Quaternionf(),
                        new Vector3f(0.75f, 0.75f, 0.75f),
                        new Quaternionf()
                )
        );
        statusDisplay.setShadowed(true);
        statusDisplay.setBrightness(new Display.Brightness(
            btm.sword.config.Config.Display.STATUS_DISPLAY_BLOCK_BRIGHTNESS,
            btm.sword.config.Config.Display.STATUS_DISPLAY_SKY_BRIGHTNESS
        ));
        statusDisplay.setPersistent(false);

        updateStatusDisplayText();

        self().addPassenger(statusDisplay);
        statusDisplay.setBillboard(Display.Billboard.VERTICAL);

        setStatusActive(true);
    }

    /**
     * Deactivates and removes the floating status {@link org.bukkit.entity.TextDisplay} from this entity.
     */
    public void endStatusDisplay() {
        setStatusActive(false);
        removeStatusDisplay();
    }

    private void removeStatusDisplay() {
        if (statusDisplay == null) return;
        Entity display = statusDisplay;
        statusDisplay = null;

        if (!Sword.getInstance().isEnabled()) return;

        // This Bukkit Runnable is fine. Intricate canceling is required here.
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

    /**
     * Called shortly after this entity is registered in {@link btm.sword.system.entity.SwordEntityArbiter}.
     * Schedules the initial status display spawn.
     */
    public void onRegister() {
        SwordScheduler.runBukkitTaskLater(this::restartStatusDisplay, 100, TimeUnit.MILLISECONDS);
    }

    /**
     * Called when this entity is spawned or re-spawned.
     * Resets resources and tick counter.
     */
    public void onSpawn() {
        ticks = 0;
        setShouldTick(true);
        resetResources();
        TimeArbiter.movementSpeedApplication.accept(this);
    }

    /**
     * Clean up for use in {@link btm.sword.listeners.EntityListener#entityRemoveEvent(EntityRemoveFromWorldEvent)}.
     */
    public void onDeath() {
        endStatusDisplay();
        setShouldTick(false);
        aspects.stopAllResourceTasks();
        if (!(self instanceof Player)) destroyed = true;
    }

    /**
     * Called when this entity's shard count reaches zero.
     * Sets the {@link #dead} flag. Override in subclasses to trigger additional death effects.
     */
    public void onZeroHealth() {
        dead = true;
    }

    /**
     * Gets the underlying {@link LivingEntity} wrapped by this SwordEntity.
     *
     * @return the Bukkit living entity
     */
    public LivingEntity self() {
        return self;
    }

    /**
     * Returns {@code true} if the given {@link LivingEntity} has the same UUID as this entity.
     *
     * @param entity the entity to compare against
     * @return {@code true} if the UUIDs match
     */
    public boolean isSelf(LivingEntity entity) {
        return self().getUniqueId().equals(entity.getUniqueId());
    }

    /**
     * Returns the {@link World} the underlying entity currently occupies.
     *
     * @return the entity's current world
     */
    public World world() {
        return self.getWorld();
    }

    /**
     * Returns the normalized direction vector of this entity's eye location.
     *
     * @return the eye-look direction as a {@link Vector}
     */
    public Vector dir() {
        return eyeLoc().getDirection();
    }

    /**
     * Returns the eye-level {@link Location} of this entity.
     *
     * @return the entity's eye location
     */
    public Location eyeLoc() {
        return self.getEyeLocation();
    }

    /**
     * Returns a location at {@code distance} blocks in the entity's eye-look direction from the eye location.
     *
     * @param distance distance in blocks
     * @return the offset location
     */
    public Location locFromEyeDir(double distance) {
        return eyeLoc().add(dir().multiply(distance));
    }

    /**
     * Returns a location at {@code distance} blocks in the entity's horizontal body direction from the eye location.
     * Unlike {@link #locFromEyeDir(double)}, pitch is ignored so the result stays at eye height.
     *
     * @param distance distance in blocks
     * @return the horizontally offset location
     */
    public Location locFromFlatDir(double distance) {
        return eyeLoc().add(getFlatBodyDir().multiply(distance));
    }

    /**
     * Returns the Bukkit {@link EntityType} of the underlying entity.
     *
     * @return the entity type
     */
    public EntityType type() {
        return self.getType();
    }

    /**
     * Returns {@code true} if the underlying Bukkit entity is no longer valid
     * (e.g., removed from the world).
     *
     * @return {@code true} if invalid
     */
    public boolean isInvalid() {
        return !self().isValid();
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
    public void addImpalement(Impalement impalement) {
        Debug.combat("addImpalement");
        impalements.add(impalement);
    }

    /**
     * Decrements the count of impalements on this entity.
     */
    public void removeImpalement(Impalement impalement) {
        impalements.remove(impalement);
    }

    /**
     * Checks if this entity is currently impaled (has one or more impalements).
     *
     * @return true if impaled, false otherwise
     */
    public boolean isImpaled() {
        return !impalements.isEmpty();
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

    protected boolean shouldDeferDeath() {
        return false;
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
    public void hit(Combatant source,
                    float reapedSoulfire,
                    long hitInvulnerableTickDuration,
                    int baseNumShards,
                    float baseToughnessDamage,
                    float baseSoulfireReduction,
                    Vector knockbackVelocity,
                    Affliction... afflictions) {
        if (source.getCachedTeam() != null && source.getCachedTeam() == cachedTeam) {
            return;
        }

        if (hit)
            return;
        else
            hit = true;

        SoulfireManager.transferSoulfire(source, this, reapedSoulfire);

        this.hitInvulnerableTickDuration = hitInvulnerableTickDuration;

        self.damage(0.01);

        Prefab.Particles.TEST_HIT.display(getChestLocation());
        SoundUtil.playSound(source.self(), SwordSoundType.ENTITY_PLAYER_ATTACK_STRONG,
            Config.Audio.ENTITY_HIT_CONNECT_VOLUME, Config.Audio.ENTITY_HIT_CONNECT_PITCH);

        self.setVelocity(knockbackVelocity);

        // If Toughness == 0
        if (aspects.toughness().remove(baseToughnessDamage)) {
            if (!toughnessBroken) {
                Prefab.Particles.TOUGH_BREAK_1.display(getChestLocation());
                onToughnessBroken();
            }
            self.playHurtAnimation(0);
            displayShardLoss();
            aspects.restartResourceProcessAfterDelay(AspectType.SHARDS, aspects.shards().getBaseRegenPeriod());
        }

        // remove returns true only if the value reaches or goes below 0
        if (toughnessBroken) {
            // If Shards == 0 (dead)
            if (changeShards(-baseNumShards)) {
                onZeroHealth();

                if (!shouldDeferDeath()) {
                    SwordScheduler.runBukkitTaskLater(() -> {
                        self.damage(74077740, source.self());
                        if (!self.isDead())
                            self.setHealth(0); },
                        SwordTimeUnit.MILLISECONDS_PER_TICK * 2, TimeUnit.MILLISECONDS);
                }
                return;
            }
            shardsLostDuringToughnessBreak += baseNumShards;


            if (shardsLostDuringToughnessBreak >= Config.Combat.SHARDS_LOST_PERCENT_TOUGHNESS_RESET * aspects.shards().effectiveMaxValue()) {
                aspects.toughness().setCurPercent(Config.Combat.TOUGHNESS_RECHARGE_PERCENT);
            }
        }

        aspects.soulfire().remove(baseSoulfireReduction);

        for (Affliction affliction : afflictions) {
            affliction.start(this);
        }
    }

    /**
     * Called when this entity is grabbed by a {@link Combatant}.
     * Override in subclasses to react to being grabbed (e.g., to disable AI).
     */
    public void onGrabbed() {}

    /**
     * Called when this entity is released from a grab.
     * Override in subclasses to re-enable behaviour suppressed during the grab.
     */
    public void onReleased() {}

    /**
     * Returns true if a parry hit-detection window is currently active on this entity.
     * Overridden in {@link SwordPlayer} to check the real parry window timestamp.
     *
     * @return true if an incoming BLOCKABLE hit will be parried
     */
    public boolean isInParryWindow() {
        return false;
    }

    /**
     * Applies a hit to this entity using a pre-packaged {@link HitValuePacket}.
     * <p>
     * Handles blocking/parrying for {@link SwordPlayer} defenders before delegating
     * to the full {@link #hit(Combatant, float, long, int, float, float, Vector, Affliction...)} overload.
     * Channel-interrupted state is set on the defender if they are channelling at the moment of impact.
     * </p>
     *
     * @param source            the {@link Combatant} dealing the hit
     * @param v                 the packaged hit values
     * @param knockbackVelocity velocity to apply as knockback
     * @param afflictions       optional afflictions to apply
     */
    public void hit(Combatant source, HitValuePacket v, Vector knockbackVelocity, Affliction... afflictions) {
        if (this instanceof SwordPlayer defender &&
                defender.getActivationContext() == ActivationContext.CHANNELING) {
            defender.setChannelInterrupted(true);
        }

        if (this instanceof SwordPlayer defender && defender.isBlocking()) {
            BlockAction.BlockResult result = BlockAction.resolveBlock(source, defender, v);
            switch (result) {
                case BLOCKED, PARRIED -> { return; }
                case SHIELD_PASSED -> {
                    HitValuePacket scaled = BlockAction.applyBypassScale(v);
                    hit(source,
                        scaled.reapedSoulfire(),
                        scaled.invulnerableTicks(),
                        scaled.shardDamage(),
                        scaled.toughnessDamage(),
                        scaled.soulfireLoss(),
                        knockbackVelocity,
                        afflictions);
                    return;
                }
                default -> { /* NOT_BLOCKED: fall through to normal hit */ }
            }
        }
        hit(source,
            v.reapedSoulfire(),
            v.invulnerableTicks(),
            v.shardDamage(),
            v.toughnessDamage(),
            v.soulfireLoss(),
            knockbackVelocity,
            afflictions);
    }

    /**
     * Displays visual effects related to shard loss.
     * Intended to be overridden in subclasses.
     */
    public void displayShardLoss() {
        // TODO: later
    }

    /**
     * Assigns this entity to the given {@link SwordTeam} by updating its Bukkit scoreboard tags.
     * Any previously held sword team tag is removed first.
     *
     * @param team the team to join
     */
    public void joinTeam(SwordTeam team) {
        if (cachedTeam != null) {
            self.removeScoreboardTag(cachedTeam.tag());
        }
        self.addScoreboardTag(team.tag());
        cachedTeam = team;
    }

    /**
     * Returns this entity's current {@link SwordTeam} from the cached field set in
     * {@link #joinTeam(SwordTeam)}, or {@code null} if no team has been assigned.
     *
     * @return the team, or {@code null}
     */
    public SwordTeam getTeam() {
        return cachedTeam;
    }

    /**
     * Adjusts this entity's shard count by the given amount.
     * Negative values remove shards; positive values add them.
     *
     * @param amount shard delta (may be negative)
     * @return {@code true} if shards reached or fell below zero (entity is dead)
     */
    public boolean changeShards(int amount) {
        if (amount < 0) {
            return aspects.shards().remove(-1 * amount);
        }
        else {
            aspects.shards().add(amount);
            return false;
        }
    }

    /**
     * Adjusts this entity's soulfire by the given amount.
     * Negative values remove soulfire; positive values add it.
     *
     * @param amount soulfire delta (may be negative)
     * @return {@code true} if soulfire reached or fell below zero
     */
    public boolean changeSoulfire(float amount) {
        if (amount < 0) {
            return aspects.soulfire().remove(-1 * amount);
        }
        else {
            aspects.soulfire().add(amount);
            return false;
        }
    }

    /**
     * Resets this entity's combat resources (shards, toughness, soulfire) to their defaults.
     * Also sends a message to the entity displaying current resource values.
     */
    public void resetResources() {
        aspects.shards().reset();
        aspects.toughness().reset();
        aspects.form().setCur(0);
        aspects.soulfire().setCur(0); // start with 0 soulfire
        message("Reset resources:\n" + aspects.curResources());
    }

    /**
     * Called when the entity's toughness breaks. Adjusts effectiveness percentages and
     * starts a repeating task to monitor toughness recharge and reset state.
     */
    public void onToughnessBroken() {
        toughnessBroken = true;
        aspects.toughness().setEffAmountPercent(Config.Entity.HIT_TOUGH_BREAK_RECHARGE_AMOUNT_PERCENT);
        aspects.toughness().setEffPeriodPercent(Config.Entity.HIT_TOUGH_BREAK_RECHARGE_PERIOD_PERCENT);
        TimeArbiter.runTimeIndependentBukkitTaskOnTimer(
            null,
            null,
            0, 100,
            SwordEntity.class, "onToughnessBroken",
            new PredicateRunnablePair(
                () -> self == null || self.isDead(),
                () -> {
                    aspects.toughness().setEffAmountPercent(1f);
                    aspects.toughness().setEffPeriodPercent(1f);
                    toughnessBroken = false;
                }
            ),
            new PredicateRunnablePair(
                () -> aspects.toughness().curPercent() > Config.Entity.HIT_TOUGH_BREAK_RECHARGE_CUTOFF_PERCENT,
                () -> {
                    aspects.toughness().setEffAmountPercent(1f);
                    aspects.toughness().setEffPeriodPercent(1f);
                    toughnessBroken = false;
                    Location c = getChestLocation();
                    Prefab.Particles.TOUGH_RECHARGE_1.display(c);
                    Prefab.Particles.TOUGH_RECHARGE_2.display(c);
                }
            )
        );
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
     * Sends a chat message component to this entity if it is a player.
     *
     * @param message the {@link Component} message to send
     */
    public void message(Component message) {
        self.sendMessage(message);
    }

    /**
     * Sends a debug message to this entity's chat and to the server console,
     * gated by {@link Config.Debug#LOGGING_VERBOSE_INVENTORY}.
     *
     * @param message the inventory debug string to emit
     */
    public void inventoryInfo(String message) {
        if (Config.Debug.LOGGING_VERBOSE_INVENTORY) {
            this.message(message);
            Sword.print("[Inventory][" + self.getName() + "] " + message);
        }
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
//                if (slot >= 36 && slot <= 39) continue;

                ItemStack slotItem = contents[slot];
                if (slotItem == null || slotItem.getType().isAir()) {
                    inv.setItem(slot, itemStack);
                    return;
                }
            }

            // TODO: Convert this into a StuckItem

            if (!itemStack.isEmpty()) {
                Vector dropVel = Config.Direction.down().multiply(0.5);

                DroppedItem stuck = new DroppedItem(getChestLocation(), dropVel, itemStack);
                stuck.register();
            }
        }
        else {
            Objects.requireNonNull(self.getEquipment()).setItemInOffHand(itemStack);
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
        return main ?
            Objects.requireNonNull(self.getEquipment()).getItemInMainHand() :
            Objects.requireNonNull(self.getEquipment()).getItemInOffHand();
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
        setItemStackInHand(ItemStack.of(itemType), main);
    }

    /**
     * Sets the item at the specified inventory slot index.
     * For players, sets the item in the player's inventory at the given slot.
     * For non-players, {@code index == 0} maps to main hand; otherwise off hand.
     *
     * @param index the inventory slot index
     * @param item  the item to place
     */
    public void setItemInInventory(int index, ItemStack item) {
        if (self() instanceof Player p) {
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
        double yawRads = Math.toRadians(eyeLoc().getYaw());
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
     * @param velocity the velocity {@link Vector} to set
     */
    public void setVelocity(Vector velocity) {
        TimeArbiter.setVelocity(self, velocity);
    }

    /**
     * Returns the first {@link SwordEntity} hit by a ray cast from the entity's eye in its look direction
     * up to {@code range} blocks away, excluding the entity itself.
     *
     * @param range maximum ray length in blocks
     * @return the targeted {@link SwordEntity}, or {@code null} if nothing is in range
     */
    public SwordEntity getTargetedEntity(double range) {
        LivingEntity target = (LivingEntity) HitboxUtil.ray(
                eyeLoc(), dir(), range, 1,
                entity -> entity instanceof LivingEntity e && !isSelf(e) && e.isValid());

        return target == null ? null : SwordEntityArbiter.getOrAdd(target);
    }

    /**
     * Returns the right vector of this entity's current directional basis.
     *
     * @param withPitch if {@code true}, uses the eye-direction basis (includes pitch);
     *                  if {@code false}, uses the body-direction basis (yaw only)
     * @return the right basis vector
     */
    public Vector rightBasisVector(boolean withPitch) {
        if (withPitch) {
            calcEyeDirBasis();
            return currentEyeDirectionBasis.right();
        }
        calcBodyDirBasis();
        return currentBodyDirectionBasis.right();
    }

    /**
     * Returns the up vector of this entity's current directional basis.
     *
     * @param withPitch if {@code true}, uses the eye-direction basis (includes pitch);
     *                  if {@code false}, uses the body-direction basis (yaw only)
     * @return the up basis vector
     */
    public Vector upBasisVector(boolean withPitch) {
        if (withPitch) {
            calcEyeDirBasis();
            return currentEyeDirectionBasis.up();
        }
        calcBodyDirBasis();
        return currentBodyDirectionBasis.up();
    }

    /**
     * Returns the forward vector of this entity's current directional basis.
     *
     * @param withPitch if {@code true}, uses the eye-direction basis (includes pitch);
     *                  if {@code false}, uses the body-direction basis (yaw only)
     * @return the forward basis vector
     */
    public Vector forwardBasisVector(boolean withPitch) {
        if (withPitch) {
            calcEyeDirBasis();
            return currentEyeDirectionBasis.forward();
        }
        calcBodyDirBasis();
        return currentBodyDirectionBasis.forward();
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
        currentEyeDirectionBasis = VectorUtil.getBasis(eyeLoc(), dir());
        timeOfLastEyeBasisCalculation = System.currentTimeMillis();
    }

    private void updateBodyDirectionBasis() {
        currentBodyDirectionBasis = VectorUtil.getBasisWithoutPitch(self());
        timeOfLastBodyBasisCalculation = System.currentTimeMillis();
    }

    /**
     * Returns the most up-to-date eye-direction {@link Basis}, recalculating it from the current eye location.
     *
     * @return the refreshed eye-direction basis
     */
    public Basis getCurrentEyeDirectionBasis() {
        updateEyeDirectionBasis();
        return currentEyeDirectionBasis;
    }

    /**
     * Returns the most up-to-date body-direction (yaw-only) {@link Basis}, recalculating it immediately.
     *
     * @return the refreshed body-direction basis
     */
    public Basis getCurrentBodyDirectionBasis() {
        updateBodyDirectionBasis();
        return currentBodyDirectionBasis;
    }

    /**
     * Returns a clone of the chest offset vector (relative to the entity's feet).
     *
     * @return cloned chest offset {@link Vector}
     */
    public Vector getChestVector() {
        return chestVector.clone();
    }

    /**
     * Returns the current feet-level {@link Location} of this entity.
     *
     * @return the entity's current location
     */
    public Location getLocation() {
        return self().getLocation();
    }

    /**
     * Teleports this entity to the specified {@link Location} via {@link btm.sword.control.EntityController}.
     *
     * @param location the target location
     */
    public void teleport(Location location) {
        EntityController.teleport(self, location);
    }
}
