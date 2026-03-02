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

import btm.sword.system.action.ActionCaster;
import btm.sword.system.action.movement.MovementAction;
import btm.sword.system.action.throwing.types.ThrownItem;
import btm.sword.system.control.SwordScheduler;
import btm.sword.system.entity.aspect.AspectType;
import btm.sword.system.entity.base.CombatProfile;
import btm.sword.system.entity.base.SwordEntity;
import btm.sword.system.entity.umbral.UmbralBlade;
import btm.sword.system.entity.umbral.input.BladeRequest;
import btm.sword.system.entity.umbral.statemachine.state.LodgedState;
import btm.sword.system.entity.umbral.statemachine.state.RecallingState;
import btm.sword.system.entity.umbral.statemachine.state.SheathedState;
import btm.sword.system.entity.umbral.statemachine.state.StandbyState;
import btm.sword.system.input.ActivationContext;
import btm.sword.system.item.KeyRegistry;
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

    private boolean isGrabbing = false;
    private SwordEntity grabbedEntity;
    private boolean attemptedGrabImpale;

    private UmbralBlade umbralBlade;
    private boolean startingBlade;

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

        this.attrInteractionRange = self().getAttribute(Attribute.ENTITY_INTERACTION_RANGE);
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

    public void handleUmbralBladeTick() {
        if (!self().isValid()) return;

        if (umbralBlade == null && !isStartingBlade()) {
            setupUmbralBlade();
            return;
        }
        if (umbralBlade == null) return;

        umbralBlade.onTick();
    }

    public void setupUmbralBlade() {
        setStartingBlade(true);
        Combatant pass = this;
        SwordScheduler.runBukkitTaskLater(() -> {
            if (umbralBlade != null) return;
            message("Starting Umbral Blade");
            umbralBlade = new UmbralBlade(pass, ItemStack.of(Material.STONE_SWORD));
            setStartingBlade(false);
            }, 200, TimeUnit.MILLISECONDS
        );
    }

    public void endUmbralBlade() {
        if (umbralBlade == null) return;
        umbralBlade.dispose();
    }

    public void requestUmbralBladeState(BladeRequest request) {
        umbralBlade.request(request);
    }

    /**
     * Sets the currently active ability cast task.
     *
     * @param abilityCastTask the BukkitTask representing the ability cast
     */
    public void setCastTask(BukkitTask abilityCastTask) {
        this.abilityCastTask = abilityCastTask;
    }

    public void consumeSoulfire(float requiredSoulfire) {
        message("Current: " + String.format("%.1f", aspects.soulfireCur()) +
            " to remove: " + String.format("%.1f", requiredSoulfire));
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
        if (target instanceof SwordPlayer sp) sp.setActivationContext(ActivationContext.STUNNED);
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
            grabbedEntity.hit(this, Prefab.Attacks.grabHit,
                target.getEyeLocation().subtract(eyeLoc()).toVector());
        });
    }

    public boolean holdingUmbralItemInMainHand() {
        return isUmbralItem(getItemStackInHand(true));
    }

    public boolean holdingMenuItemInMainHand() {
        return KeyRegistry.hasKey(getItemStackInHand(true), KeyRegistry.MAIN_MENU_BUTTON_KEY);
    }

    public boolean isUmbralItem(ItemStack item) {
        return !item.isEmpty() &&
            (KeyRegistry.hasKey(item, KeyRegistry.SOUL_LINK_KEY) ||
                KeyRegistry.hasKey(item, KeyRegistry.UMBRAL_BLADE_KEY));
    }

    public boolean holdingUmbralBlade() {
        ItemStack itemStack = getItemStackInHand(true);
        return !itemStack.isEmpty() &&
            KeyRegistry.hasKey(itemStack, KeyRegistry.UMBRAL_BLADE_KEY);
    }

    public boolean holdingSoulLink() {
        ItemStack itemStack = getItemStackInHand(true);
        return !itemStack.isEmpty() &&
            KeyRegistry.hasKey(itemStack, KeyRegistry.SOUL_LINK_KEY);
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
        message("ability cast task - " + (abilityCastTask == null ? "none" : abilityCastTask.getTaskId()));
        return abilityCastTask == null && !isGrabbing && !isGrabbed();
    }

    public boolean canPerformUmbralAction() {
        return canPerformAction() &&
            (
                umbralBlade.inState(StandbyState.class) ||
                umbralBlade.inState(RecallingState.class) ||
                umbralBlade.inState(LodgedState.class) ||
                umbralBlade.inState(SheathedState.class)
            );
    }

    /**
     * Checks if this combatant can perform an air dash.
     * Combines action availability with limit on air dash count from {@link CombatProfile}.
     *
     * @return true if air dash is possible, false otherwise
     */
    public boolean canAirDash() {
        return canPerformAction() && getAirDashesPerformed() < getCombatProfile().getMaxAirDodges();
    }

    public boolean canStrafe() {
        return canPerformAction() && self().isOnGround();
    }

    /**
     * Checks if the combatant can perform a throw action.
     * Requires action availability, main hand holding an appropriate throwable item,
     * and off hand holding a shield.
     *
     * @return true if throwing is possible, false otherwise
     */
    public boolean canThrow() {
        ItemStack main = getItemStackInHand(true);
        ItemStack off = getItemStackInHand(false);

        boolean throwable =
                        !main.getType().equals(Material.CROSSBOW) &&
                        !main.getType().equals(Material.BOW) &&
                        !main.getType().isEdible() &&
                        !main.getType().isAir();

        return canPerformAction() && throwable && off.getType().equals(Material.SHIELD);
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
        return (int) Math.max(min, base - (multiplier * aspects.getAspectVal(type)) );
    }

    public boolean canPerformUmbralLinkAttack() {
        return canPerformAction() &&
            (getUmbralBlade().inState(RecallingState.class) ||
                getUmbralBlade().inState(StandbyState.class) ||
                getUmbralBlade().inState(SheathedState.class) ||
                getUmbralBlade().inState(LodgedState.class));
    }

    public boolean canPerformShadowBlink() {
        return canPerformAction() &&
            (getUmbralBlade().inState(LodgedState.class));
    }
}
