package btm.sword.system.action.constraint;

import org.bukkit.Material;

import btm.sword.system.entity.impl.Combatant;
import btm.sword.system.entity.impl.SwordPlayer;
import btm.sword.system.entity.umbral.statemachine.state.LodgedState;
import btm.sword.system.entity.umbral.statemachine.state.RecallingState;
import btm.sword.system.entity.umbral.statemachine.state.SheathedState;
import btm.sword.system.entity.umbral.statemachine.state.StandbyState;

/**
 * Predefined {@link ActionConstraint} constants covering the most common action conditions.
 * <p>
 * These replace scattered {@code canPerform*()} checks in {@link Combatant} with composable,
 * named constraints that can be declared in the input tree builder.
 * </p>
 */
public final class CommonConstraints {

    private CommonConstraints() {}

    /** Passes when no ability cast task is blocking new actions. */
    public static final ActionConstraint NOT_CASTING =
        c -> c.getAbilityCastTask() == null;

    /** Passes when the combatant is not currently grabbing another entity. */
    public static final ActionConstraint NOT_GRABBING =
        c -> !c.isGrabbing();

    /** Passes when the combatant has not been grabbed by another entity. */
    public static final ActionConstraint NOT_GRABBED =
        c -> !c.isGrabbed();

    /**
     * Passes when the combatant is free to perform a general action:
     * not casting, not grabbing, and not grabbed.
     */
    public static final ActionConstraint CAN_ACT =
        c -> c.getAbilityCastTask() == null && !c.isGrabbing() && !c.isGrabbed();

    /** Passes when the combatant is holding any non-empty item in their main hand. */
    public static final ActionConstraint HOLDING_ITEM =
        c -> !c.getItemStackInHand(true).getType().isAir();

    /** Passes when the main-hand item is a usable weapon (not bow or crossbow). */
    public static final ActionConstraint HOLDING_WEAPON =
        c -> {
            Material type = c.getItemStackInHand(true).getType();
            return !type.isAir() && !type.equals(Material.CROSSBOW) && !type.equals(Material.BOW);
        };

    /**
     * Passes when the umbral blade is in a state compatible with soul-link melee attacks
     * (standby, recalling, sheathed, or lodged).
     */
    public static final ActionConstraint UMBRAL_LINK_ATTACK_READY =
        c -> c.getAbilityCastTask() == null && !c.isGrabbing() && !c.isGrabbed()
            && c.getUmbralBlade() != null
            && (c.getUmbralBlade().inState(RecallingState.class)
                || c.getUmbralBlade().inState(StandbyState.class)
                || c.getUmbralBlade().inState(SheathedState.class)
                || c.getUmbralBlade().inState(LodgedState.class));

    /**
     * Passes when the umbral blade is in an active, actionable state
     * (standby, recalling, lodged, or sheathed).
     */
    public static final ActionConstraint UMBRAL_ACTION_READY =
        c -> c.getAbilityCastTask() == null && !c.isGrabbing() && !c.isGrabbed()
            && c.getUmbralBlade() != null
            && (c.getUmbralBlade().inState(StandbyState.class)
                || c.getUmbralBlade().inState(RecallingState.class)
                || c.getUmbralBlade().inState(LodgedState.class)
                || c.getUmbralBlade().inState(SheathedState.class));

    /** Passes when the combatant can perform an air dash (not at max dash count). */
    public static final ActionConstraint AIR_DASH_AVAILABLE =
        Combatant::canAirDash;

    /**
     * Passes when the umbral blade is lodged in a target, enabling shadow blink.
     */
    public static final ActionConstraint BLADE_IS_LODGED =
        c -> c.getAbilityCastTask() == null && !c.isGrabbing() && !c.isGrabbed()
            && c.getUmbralBlade() != null
            && c.getUmbralBlade().inState(LodgedState.class);

    /**
     * Passes when the combatant is not currently blocking (holding shield up).
     * For non-player combatants (which cannot block), this always passes.
     */
    public static final ActionConstraint NOT_BLOCKING =
        c -> !(c instanceof SwordPlayer sp) || !sp.isBlocking();

    /**
     * Stub — always passes. Intended to block actions while the player is carrying a CTF flag.
     *
     * <p>Full enforcement (no dash, no grab, no block while carrying) is pending a global context
     * constraint system on {@code Combatant}. Until then, only the speed debuff is active.
     * See GitHub issue #TODO for the refactor tracking this.</p>
     */
    public static final ActionConstraint NOT_CARRYING_FLAG = c -> true;
}
