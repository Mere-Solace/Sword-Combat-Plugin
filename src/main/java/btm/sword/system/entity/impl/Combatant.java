package btm.sword.system.entity.impl;

import java.util.concurrent.TimeUnit;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import btm.sword.config.Config;
import btm.sword.system.action.ActionCaster;
import btm.sword.system.action.movement.MovementAction;
import btm.sword.system.action.throwing.types.ThrownItem;
import btm.sword.system.control.PredicateRunnablePair;
import btm.sword.system.control.SwordScheduler;
import btm.sword.system.control.TimeArbiter;
import btm.sword.system.entity.aspect.AspectType;
import btm.sword.system.entity.base.CombatProfile;
import btm.sword.system.entity.base.SwordEntity;
import btm.sword.system.entity.umbral.UmbralBlade;
import btm.sword.system.entity.umbral.input.BladeRequest;
import btm.sword.system.entity.umbral.statemachine.state.LodgedState;
import btm.sword.system.entity.umbral.statemachine.state.RecallingState;
import btm.sword.system.entity.umbral.statemachine.state.SheathedState;
import btm.sword.system.entity.umbral.statemachine.state.StandbyState;
import btm.sword.system.entity.umbral.statemachine.state.WaitingState;
import btm.sword.system.entity.umbral.statemachine.state.WieldState;
import btm.sword.system.input.ActivationContext;
import btm.sword.system.item.KeyRegistry;
import btm.sword.utility.Debug;
import btm.sword.utility.Prefab;
import lombok.Getter;
import lombok.Setter;

/**
 * Abstract class representing combat-capable entities within the Sword plugin.
 * Extends {@link SwordEntity} with combat-specific state and behaviors such as grabbing,
 * air dashes, throwing, and ability casting.
 * <p>
 * Maintains references to thrown items, ability cast tasks, and tracks input-sensitive mechanics.
 * </p>
 */
@Getter
@Setter
public abstract class Combatant extends SwordEntity {
    private BukkitTask abilityCastTask = null;

    private int airDashesPerformed;
    protected Vector dashDirection;
    protected boolean dashing;

    private boolean isGrabbing = false;
    private SwordEntity grabbedEntity;
    private boolean attemptedGrabImpale;

    private UmbralBlade umbralBlade;
    private boolean startingBlade;
    private boolean bladeEnabled = true;

    private ThrownItem thrownItem;
    private ItemStack offHandItemStackDuringThrow;
    private ItemStack mainHandItemStackDuringThrow;
    private boolean attemptingThrow;
    private boolean throwCancelled;
    private boolean throwSuccessful;

    private final AttributeInstance attrHealth;
    private final AttributeInstance attrAbsorption;
    private final AttributeInstance attrArmor;
    private final AttributeInstance attrInteractionRange;

    /**
     * Constructs a new Combatant wrapping the given {@link LivingEntity}
     * and using the specified {@link CombatProfile}.
     *
     * @param associatedEntity the Bukkit living entity to associate
     * @param combatProfile the combat profile defining combat stats and settings
     */
    public Combatant(LivingEntity associatedEntity, CombatProfile combatProfile) {
        super(associatedEntity, combatProfile);
        this.airDashesPerformed = 0;

        this.attrHealth = self().getAttribute(Attribute.MAX_HEALTH);
        // TODO: bake in this 2*assumption (1 health visually is a half heart), and also remember to update this baseValue somewhere.
        if (attrHealth != null) attrHealth.setBaseValue(2 * combatProfile.getStat(AspectType.SHARDS).getValue());

        this.attrAbsorption = self().getAttribute(Attribute.MAX_ABSORPTION);
        if (attrAbsorption != null) attrAbsorption.setBaseValue(combatProfile.getStat(AspectType.TOUGHNESS).getValue());

        this.attrArmor = self().getAttribute(Attribute.ARMOR);
        if (attrArmor != null) attrArmor.setBaseValue(combatProfile.getStat(AspectType.FORM).getValue());

        // Doesn't affect block placement range...
        this.attrInteractionRange = self().getAttribute(Attribute.ENTITY_INTERACTION_RANGE);
        // TODO: make dynamic based on circumstance?
        if (attrInteractionRange != null) attrInteractionRange.setBaseValue(0.25); // reduce reach distance
    }

    @Override
    public void onSpawn() {
        super.onSpawn();

    }

    /**
     * Called when the entity dies.
     * Cleans up the sheathed sword display entity.
     */
    @Override
    public void onDeath() {
        super.onDeath();

        if (umbralBlade == null) return;
        if (umbralBlade.getDisplay().isValid()) {
            Prefab.Particles.UMBRAL_BLADE_POOF.display(umbralBlade.getDisplay().getLocation());
        }
        if (umbralBlade.getDisplay() == null || !umbralBlade.getDisplay().isValid()) {
            message("Display is null.");
        }
        umbralBlade.dispose();
        // TODO: #122 - On death umbral blade logic... What should happen here?
    }

    @Override
    public void onZeroHealth() {
        super.onZeroHealth();
        if (umbralBlade != null && umbralBlade.getDisplay().isValid()) {
            Prefab.Particles.UMBRAL_BLADE_POOF.display(umbralBlade.getDisplay().getLocation());
            umbralBlade.dispose();
        }
    }

    @Override
    protected void onTick() {
        super.onTick();
        handleUmbralBladeTick();
    }

    /**
     * Drives the per-tick lifecycle of this combatant's {@link UmbralBlade}.
     * <p>
     * Skipped entirely when the blade has been deactivated via {@link #deactivateUmbralBlade()}.
     * If the blade has not been created yet and a creation attempt is not already in-flight,
     * calls {@link #setupUmbralBlade()} to begin the deferred spawn. Once the blade exists,
     * delegates to {@link UmbralBlade#onTick()}.
     * </p>
     */
    public void handleUmbralBladeTick() {
        if (!self().isValid() || !bladeEnabled) return;

        if (umbralBlade == null && !isStartingBlade()) {
            setupUmbralBlade();
            return;
        }
        if (umbralBlade == null) return;

        umbralBlade.onTick();
    }

    /**
     * Schedules creation of the {@link UmbralBlade} for this combatant after a 200 ms delay.
     * The delay avoids spawning the display entity on the first server tick.
     * Override in subclasses to alter blade setup behaviour (e.g., {@link btm.sword.system.entity.impl.Hostile}
     * immediately deactivates the blade to suppress the visual).
     */
    public void setupUmbralBlade() {
        setStartingBlade(true);
        SwordScheduler.runBukkitTaskLater(() -> {
            if (!bladeEnabled || !self().isValid() || umbralBlade != null) {
                setStartingBlade(false);
                return;
            }
            message("Starting Umbral Blade");
            umbralBlade = new UmbralBlade(this, ItemStack.of(Material.STONE_SWORD));
            setStartingBlade(false);
            }, 200, TimeUnit.MILLISECONDS
        );
    }

    /**
     * Permanently deactivates this combatant's {@link UmbralBlade}: disposes and removes the
     * display entity from the world, and prevents {@link #handleUmbralBladeTick()} from
     * spawning a new one. Call {@link #activateUmbralBlade()} to re-enable spawning.
     */
    public void deactivateUmbralBlade() {
        bladeEnabled = false;
        startingBlade = false;
        if (umbralBlade != null) {
            umbralBlade.dispose();
            umbralBlade = null;
        }
    }

    /**
     * Re-enables blade spawning after a {@link #deactivateUmbralBlade()} call.
     * The blade will be created on the next {@link #handleUmbralBladeTick()} via
     * the normal deferred-spawn path.
     */
    public void activateUmbralBlade() {
        bladeEnabled = true;
    }

    /**
     * Disposes the active {@link UmbralBlade}, removing its display entity and releasing all
     * resources. Does nothing if the blade has not been created.
     * <p>
     * Unlike {@link #deactivateUmbralBlade()}, this does not prevent the blade from being
     * respawned on the next tick — use that method when permanent suppression is needed.
     * </p>
     */
    public void endUmbralBlade() {
        if (umbralBlade == null) return;
        umbralBlade.dispose();
        umbralBlade = null;
    }

    /**
     * Forwards a {@link BladeRequest} to this combatant's {@link UmbralBlade},
     * queuing it in the blade's input buffer for the next FSM tick.
     *
     * @param request the blade request to enqueue
     */
    public void requestUmbralBladeState(BladeRequest request) {
        TimeArbiter.runFixedIterationTaskTimer(
            null,
            null,
            0, 25, 160,
            Combatant.class, "requestUmbralBladeState",
            null,
            new PredicateRunnablePair(
                () -> umbralBlade != null,
                () -> umbralBlade.request(request)
            )
        );
    }

    /**
     * Sets the currently active ability cast task.
     *
     * @param abilityCastTask the BukkitTask representing the ability cast
     */
    public void setCastTask(BukkitTask abilityCastTask) {
        this.abilityCastTask = abilityCastTask;
    }

    /**
     * Deducts the specified amount of soulfire from this combatant and restarts the
     * soulfire regeneration task after the base regen delay.
     *
     * @param requiredSoulfire the amount of soulfire to consume
     */
    public void consumeSoulfire(float requiredSoulfire) {
        Debug.combat("soulfire cur=" + String.format("%.1f", aspects.soulfireCur())
            + " cost=" + String.format("%.1f", requiredSoulfire));
        aspects.soulfire().remove(requiredSoulfire);
        aspects.soulfire().restartRegenTaskLater(aspects.soulfire().getBaseRegenPeriod());
    }

    /**
     * Initiates a grab action on the specified target {@link SwordEntity}.
     * Applies minor damage to the target, sets grab states, and displays a particle effect.
     *
     * @param target the SwordEntity that is being grabbed
     */
    public void onGrab(SwordEntity target) {
        attemptedGrabImpale = false;

        LivingEntity t = target.self();
        setGrabbing(true);
        target.setGrabbed(true);
        target.onGrabbed();
        if (target instanceof SwordPlayer sp) sp.setActivationContext(ActivationContext.INCAPACITATED);
        setGrabbedEntity(target);
        Prefab.Particles.GRAB_CLOUD.display(t.getLocation().add(new Vector(0, 1, 0)));
        Prefab.Sounds.PUNCH_CONNECT.playForAllInRadius(self);
    }

    /**
     * Releases the currently grabbed entity without further action.
     */
    public void onGrabLetGo() {
        isGrabbing = false;
        grabbedEntity.setGrabbed(false);
        grabbedEntity.onReleased();
        if (grabbedEntity instanceof SwordPlayer sp) sp.setActivationContext(ActivationContext.NORMAL);
    }

    /**
     * Throws the currently grabbed entity, applying movement and damage effects.
     * Resets grab state and calls {@link MovementAction#toss(Combatant, SwordEntity)}.
     */
    public void onGrabThrow() {
//        onGrabHit();

        isGrabbing = false;
        grabbedEntity.setGrabbed(false);
        grabbedEntity.onReleased();
        if (grabbedEntity instanceof SwordPlayer sp) sp.setActivationContext(ActivationContext.NORMAL);
        MovementAction.toss(this, grabbedEntity);
    }

    /**
     * Performs the hit action during a grab, dealing a fixed amount of damage to the grabbed entity
     * and displaying associated particle effects.
     */
    public void onGrabHit() {
        if (holdingSoulLink() && !umbralBlade.inState(SheathedState.class) && !attemptedGrabImpale) {
            attemptedGrabImpale = true;
            getUmbralBlade().request(BladeRequest.GRAB_IMPALE);
        }

        LivingEntity target = grabbedEntity.self();

        target.setVelocity(dir().multiply(2));

        ActionCaster.cast(this, 75, () -> {
            Location hitLoc = target.getLocation().add(0, target.getEyeHeight() * 0.5, 0);
            Prefab.Particles.PUNCH_CONNECT.display(hitLoc);
            grabbedEntity.hit(this, Prefab.Attacks.GRAB_HIT,
                target.getEyeLocation().subtract(eyeLoc()).toVector().multiply(0.1)); //TODO: config all these values
        });
    }


    /**
     * Returns {@code true} if this combatant's main hand is empty or contains only air.
     *
     * @return {@code true} if main hand is empty
     */
    public boolean holdingNothing() {
        ItemStack inMainHand = getItemStackInHand(true);
        return inMainHand.isEmpty() || inMainHand.getType().isAir();
    }

    /**
     * Returns {@code true} if the item in the main hand is tagged as an Umbral item
     * (either the {@link btm.sword.system.item.KeyRegistry#SOUL_LINK_KEY} or
     * {@link btm.sword.system.item.KeyRegistry#UMBRAL_BLADE_KEY}).
     *
     * @return {@code true} if holding an umbral-tagged item
     */
    public boolean holdingUmbralItemInMainHand() {
        return isUmbralItem(getItemStackInHand(true));
    }

    /**
     * Returns {@code true} if the item in the main hand is tagged as the main menu button.
     *
     * @return {@code true} if the main-menu item is held
     */
    public boolean holdingMenuItemInMainHand() {
        return KeyRegistry.hasKey(getItemStackInHand(true), KeyRegistry.MAIN_MENU_BUTTON_KEY);
    }

    /**
     * Returns {@code true} if the given {@link ItemStack} carries an Umbral item tag
     * (soul-link or umbral-blade key).
     *
     * @param item the item to inspect
     * @return {@code true} if the item is an umbral-tagged item
     */
    public boolean isUmbralItem(ItemStack item) {
        return !item.isEmpty() &&
            (KeyRegistry.hasKey(item, KeyRegistry.SOUL_LINK_KEY) ||
                KeyRegistry.hasKey(item, KeyRegistry.UMBRAL_BLADE_KEY));
    }

    /**
     * Returns {@code true} if the item in the main hand is tagged as the physical
     * {@link UmbralBlade} item (not the soul-link tether item).
     *
     * @return {@code true} if holding the umbral blade item
     */
    public boolean holdingUmbralBlade() {
        ItemStack itemStack = getItemStackInHand(true);
        return !itemStack.isEmpty() && KeyRegistry.hasKey(itemStack, KeyRegistry.UMBRAL_BLADE_KEY);
    }

    /**
     * Returns {@code true} if the item in the main hand is tagged as the soul-link tether item.
     *
     * @return {@code true} if holding the soul-link item
     */
    public boolean holdingSoulLink() {
        ItemStack itemStack = getItemStackInHand(true);
        return !itemStack.isEmpty() && KeyRegistry.hasKey(itemStack, KeyRegistry.SOUL_LINK_KEY);
    }

    /**
     * Checks if this combatant can perform an action.
     * <p>
     * This returns {@code true} if the combatant is not currently casting an ability,
     * is not grabbing another entity, and is not grabbed themselves.
     * </p>
     *
     * @return true if able to perform actions, false otherwise
     */
    public boolean canPerformAction() {
        return abilityCastTask == null && !isGrabbing && !isGrabbed();
    }

    /**
     * Returns {@code true} if this combatant can perform a channel-heal action.
     * Requires the base {@link #canPerformAction()} check to pass, the blade to be
     * in the wield state, sufficient soulfire, and at least one missing shard.
     *
     * @return {@code true} if a heal can be initiated
     */
    public boolean canPerformHealAction() {
        return canPerformAction() &&
            umbralBlade != null &&
            umbralBlade.inState(WieldState.class) &&
            aspects.soulfireCur() >= Config.Combat.CHANNEL_SOULFIRE_COST &&
            aspects.shards().belowMax();
    }

    /**
     * Returns {@code true} if this combatant can perform a wield action.
     * While in {@link WaitingState}, checks that the blade is within grab range instead.
     * Otherwise, requires the base action check and the blade to be in standby, sheathed, or wield state.
     *
     * @return {@code true} if a wield action can be initiated
     */
    public boolean canPerformWieldAction() {
        if (umbralBlade.inState(WaitingState.class)) {
            return inRangeOfUmbralBlade(Config.Movement.DASH_GRAB_DISTANCE_SQUARED);
        }

        return canPerformAction() && (
                umbralBlade.inState(StandbyState.class) ||
                umbralBlade.inState(SheathedState.class) ||
                umbralBlade.inState(WieldState.class)
            );
    }

    /**
     * Returns {@code true} if the umbral blade's display entity is within the specified squared distance.
     *
     * @param range the maximum allowed squared distance
     * @return {@code true} if the blade is within range
     */
    public boolean inRangeOfUmbralBlade(double range) {
        return  umbralBlade.getDisplay().getLocation().toVector()
                    .subtract(getLocation().toVector())
                    .lengthSquared() < range;
    }

    /**
     * Returns {@code true} if this combatant can perform an umbral throw or recall action.
     * Requires the base action check and the blade to be in standby, recalling, lodged, sheathed,
     * or waiting state.
     *
     * @return {@code true} if an umbral action can be initiated
     */
    public boolean canPerformUmbralAction() {
        return canPerformAction() &&
            (
                umbralBlade.inState(StandbyState.class) ||
                umbralBlade.inState(RecallingState.class) ||
                umbralBlade.inState(LodgedState.class) ||
                umbralBlade.inState(SheathedState.class) ||
                umbralBlade.inState(WaitingState.class)
            );
    }

    /**
     * Checks if this combatant can perform an air dash.
     * Combines action availability with limit on air dash count from {@link CombatProfile}.
     *
     * @return true if air dash is possible, false otherwise
     */
    public boolean canAirDash() {
        return canPerformAction() && (isSubmergedInLiquid()
            || getAirDashesPerformed() < getCombatProfile().getMaxAirDodges());
    }

    /**
     * Returns whether this combatant is currently moving through liquid.
     * Liquid dashes are treated separately from true aerial dashes and should not
     * consume the in-air dash budget.
     *
     * @return {@code true} when the combatant is in water or lava
     */
    public boolean isSubmergedInLiquid() {
        return self().isInWater() || self().isInLava();
    }

    /**
     * Marks this combatant as dashing and schedules the flag to be cleared after {@code duration} ms.
     *
     * @param duration how long the dashing state lasts, in milliseconds
     */
    public void setDashing(int duration) {
        dashing = true;
        SwordScheduler.runBukkitTaskLater(
            () -> dashing = false,
            duration, TimeUnit.MILLISECONDS
        );
    }

    /**
     * Resets the count of air dashes performed to zero.
     */
    public void resetAirDashesPerformed() {
        this.airDashesPerformed = 0;
    }

    /**
     * Increments the count of air dashes performed by one.
     */
    public void increaseAirDashesPerformed() {
        airDashesPerformed++;
    }

    /**
     * Calculates an additive value for a stat based on an {@link AspectType}.
     *
     * @param stat the aspect type representing the stat
     * @param max maximum allowed result value
     * @param base base value before addition
     * @param multiplier multiplier applied to the aspect value before addition
     * @return the calculated additive value capped at max
     */
    public double calcValueAdditive(AspectType stat, double max, double base, double multiplier) {
        return Math.min(max, base + (multiplier * aspects.getAspectVal(stat)));
    }

    /**
     * Calculates a reductive value for a stat based on an {@link AspectType}.
     *
     * @param stat the aspect type representing the stat
     * @param min minimum allowed result value
     * @param base base value before reduction
     * @param multiplier multiplier applied to the aspect value before reduction
     * @return the calculated reductive value floored at min
     */
    public double calcValueReductive(AspectType stat, double min, double base, double multiplier) {
        return Math.max(min, base - (multiplier * aspects.getAspectVal(stat)));
    }

    /**
     * Calculates a cooldown duration in milliseconds based on an {@link AspectType}.
     *
     * @param type the aspect type affecting cooldown
     * @param min minimum cooldown in milliseconds
     * @param base base cooldown in milliseconds
     * @param multiplier multiplier applied to aspect value for reduction
     * @return the calculated cooldown duration floored at min
     */
    public int calcCooldown(AspectType type, double min, double base, double multiplier) {
        return (int) Math.max(min, base - (multiplier * aspects.getAspectVal(type)));
    }

    /**
     * Calculates and applies the standard attack cooldown based on FINESSE.
     * Sets both {@code timeOfLastAttack} and {@code durationOfLastAttack} so the
     * input tree's cooldown check ({@code getDurationOfLastAttack}) works correctly.
     */
    public void applyAttackCooldown() {
        setTimeOfLastAttack(System.currentTimeMillis());
        int cooldown = (int) calcValueReductive(AspectType.FINESSE,
            Config.Combat.ATTACKS_CAST_TIMING_MIN_DURATION,
            Config.Combat.ATTACKS_CAST_TIMING_MAX_DURATION,
            Config.Combat.ATTACKS_CAST_TIMING_REDUCTION_RATE);
        setDurationOfLastAttack((int) (cooldown * Config.Combat.ATTACKS_COOLDOWN_MULT));
    }

    /**
     * Returns {@code true} if this combatant can perform an umbral link attack
     * (a combo that requires the blade to be tethered or recalled).
     * Requires the base action check and the blade to be in recalling, standby, sheathed, or lodged state.
     *
     * @return {@code true} if an umbral link attack can be initiated
     */
    public boolean canPerformUmbralLinkAttack() {
        return canPerformAction() &&
            (getUmbralBlade().inState(RecallingState.class) ||
                getUmbralBlade().inState(StandbyState.class) ||
                getUmbralBlade().inState(SheathedState.class) ||
                getUmbralBlade().inState(LodgedState.class));
    }

    /**
     * Returns {@code true} if this combatant can perform a shadow blink teleport.
     * Requires the base action check and the blade to be in the lodged state.
     *
     * @return {@code true} if a shadow blink can be performed
     */
    public boolean canPerformShadowBlink() {
        return canPerformAction() &&
            (getUmbralBlade().inState(LodgedState.class));
    }
}
