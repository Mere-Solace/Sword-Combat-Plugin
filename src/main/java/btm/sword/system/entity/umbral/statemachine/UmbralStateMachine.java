package btm.sword.system.entity.umbral.statemachine;

import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.util.Vector;

import btm.sword.config.Config;
import btm.sword.system.control.TimeArbiter;
import btm.sword.system.entity.impl.SwordPlayer;
import btm.sword.system.entity.umbral.UmbralBlade;
import btm.sword.system.entity.umbral.input.BladeRequest;
import btm.sword.system.entity.umbral.statemachine.state.AttackingHeavyState;
import btm.sword.system.entity.umbral.statemachine.state.AttackingQuickState;
import btm.sword.system.entity.umbral.statemachine.state.FinisherState;
import btm.sword.system.entity.umbral.statemachine.state.GrabImpaleState;
import btm.sword.system.entity.umbral.statemachine.state.InactiveState;
import btm.sword.system.entity.umbral.statemachine.state.LodgedState;
import btm.sword.system.entity.umbral.statemachine.state.LungingState;
import btm.sword.system.entity.umbral.statemachine.state.PreviousState;
import btm.sword.system.entity.umbral.statemachine.state.RecallingState;
import btm.sword.system.entity.umbral.statemachine.state.RecoverState;
import btm.sword.system.entity.umbral.statemachine.state.SheathedState;
import btm.sword.system.entity.umbral.statemachine.state.StandbyState;
import btm.sword.system.entity.umbral.statemachine.state.WaitingState;
import btm.sword.system.entity.umbral.statemachine.state.WieldState;
import btm.sword.utility.Debug;
import btm.sword.utility.Prefab;
import btm.sword.utility.math.VectorUtil;
import btm.sword.utility.statemachine.State;
import btm.sword.utility.statemachine.StateMachine;
import btm.sword.utility.statemachine.Transition;
import lombok.Getter;
import lombok.Setter;

public class UmbralStateMachine extends StateMachine<UmbralBlade> {
    @Getter
    private UmbralStateFacade previousState;
    @Setter
    private boolean deactivated;

    public UmbralStateMachine(UmbralBlade context, State<UmbralBlade> initialState) {
        super(context, initialState);
    }

    /**
     * Registers all FSM transitions. Called once after construction.
     *
     */
    public void initTransitions() {
        // =====================================================================
        // UNIVERSAL — wildcard transitions
        // =====================================================================

        // Enter inactive from ANYTHING (spectator mode or DEACTIVATE)
        addTransition(new Transition<>(
            UmbralStateFacade.class,
            InactiveState.class,
            b -> (b.getThrower().self() instanceof SwordPlayer sp &&
                sp.player().getGameMode().equals(GameMode.SPECTATOR)) ||
                b.isRequested(BladeRequest.DEACTIVATE),
            b -> {}
        ));

        // Enter recover from ANYTHING when display is invalid
        addTransition(new Transition<>(
            UmbralStateFacade.class,
            RecoverState.class,
            b -> b.getDisplay() == null || b.getDisplay().isDead() || !b.getDisplay().isValid(),
            b -> {}
        ));

        // =====================================================================
        // INACTIVE
        // =====================================================================
        addTransition(new Transition<>(
            InactiveState.class,
            StandbyState.class,
            b -> b.isRequested(BladeRequest.ACTIVATE_TO_PREVIOUS),
            b -> {}
        ));

        // =====================================================================
        // RECOVER
        // =====================================================================
        addTransition(new Transition<>(
            RecoverState.class,
            StandbyState.class,
            b -> (b.getDisplay() != null && !b.getDisplay().isDead() && b.getDisplay().isValid()) ||
                b.isRequested(BladeRequest.RESUME_FROM_REPAIR),
            b -> {}
        ));

        // =====================================================================
        // SHEATHED
        // =====================================================================
        addTransition(new Transition<>(
            SheathedState.class,
            StandbyState.class,
            b -> b.isRequestedAndActive(BladeRequest.TOGGLE),
            b -> {}
        ));

        addTransition(new Transition<>(
            SheathedState.class,
            WieldState.class,
            b -> b.isRequestedAndActive(BladeRequest.WIELD),
            b -> {}
        ));

        addTransition(new Transition<>(
            SheathedState.class,
            AttackingQuickState.class,
            b -> b.isRequestedAndActive(BladeRequest.ATTACK_QUICK),
            b -> {}
        ));

        addTransition(new Transition<>(
            SheathedState.class,
            AttackingHeavyState.class,
            b -> b.isRequestedAndActive(BladeRequest.ATTACK_HEAVY),
            b -> {}
        ));

        addTransition(new Transition<>(
            SheathedState.class,
            LungingState.class,
            b -> b.isRequestedAndActive(BladeRequest.LUNGE),
            b -> {}
        ));

        // =====================================================================
        // STANDBY
        // =====================================================================
        addTransition(new Transition<>(
            StandbyState.class,
            SheathedState.class,
            b -> b.isRequestedAndActive(BladeRequest.TOGGLE),
            b -> {}
        ));

        addTransition(new Transition<>(
            StandbyState.class,
            WieldState.class,
            b -> b.isRequestedAndActive(BladeRequest.WIELD),
            b -> {}
        ));

        addTransition(new Transition<>(
            StandbyState.class,
            AttackingQuickState.class,
            b -> b.isRequestedAndActive(BladeRequest.ATTACK_QUICK),
            b -> {}
        ));

        addTransition(new Transition<>(
            StandbyState.class,
            AttackingHeavyState.class,
            b -> b.isRequestedAndActive(BladeRequest.ATTACK_HEAVY),
            b -> {}
        ));

        addTransition(new Transition<>(
            StandbyState.class,
            LungingState.class,
            b -> b.isRequestedAndActive(BladeRequest.LUNGE),
            b -> {}
        ));

        addTransition(new Transition<>(
            StandbyState.class,
            GrabImpaleState.class,
            b -> b.isRequestedAndActive(BladeRequest.GRAB_IMPALE),
            b -> {}
        ));

        addTransition(new Transition<>(
            StandbyState.class,
            FinisherState.class,
            b -> b.isRequestedAndActive(BladeRequest.FINISHER),
            b -> {}
        ));

        addTransition(new Transition<>(
            StandbyState.class,
            WaitingState.class,
            b -> b.isRequestedAndActive(BladeRequest.WAITING),
            b -> {}
        ));

        // =====================================================================
        // FINISHER
        // =====================================================================
        addTransition(new Transition<>(
            FinisherState.class,
            RecallingState.class,
            b -> b.isSkillFinished() || b.isRequestedAndActive(BladeRequest.STANDBY),
            b -> b.setSkillFinished(false)
        ));

        // =====================================================================
        // WIELD
        // =====================================================================
        addTransition(new Transition<>(
            WieldState.class,
            StandbyState.class,
            b -> b.isRequestedAndActive(BladeRequest.TOGGLE),
            b -> {}
        ));

        // =====================================================================
        // ATTACKING (Quick + Heavy)
        // =====================================================================
        addTransition(new Transition<>(
            AttackingQuickState.class,
            RecallingState.class,
            UmbralBlade::isAttackCompleted,
            b -> {}
        ));

        addTransition(new Transition<>(
            AttackingHeavyState.class,
            RecallingState.class,
            UmbralBlade::isAttackCompleted,
            b -> {}
        ));

        // =====================================================================
        // GRAB IMPALE
        // =====================================================================
        addTransition(new Transition<>(
            GrabImpaleState.class,
            LodgedState.class,
            b -> b.getHitEntity() != null,
            b -> {
                Vector kb = VectorUtil.getProjOntoPlane(b.getVelocity(), Config.Direction.UP())
                    .multiply(Config.Combat.THROWN_DAMAGE_SWORD_AXE_KNOCKBACK_AIRBORNE);
                b.getHitEntity().hit(b.getThrower(), Prefab.Attacks.umbralImpale, kb);
            }
        ));

        addTransition(new Transition<>(
            GrabImpaleState.class,
            RecallingState.class,
            UmbralBlade::isFinishedLunging,
            b -> {}
        ));

        // =====================================================================
        // WAITING
        // =====================================================================
        addTransition(new Transition<>(
            WaitingState.class,
            RecallingState.class,
            b -> b.isRequestedAndActive(BladeRequest.RECALL),
            b -> {}
        ));

        addTransition(new Transition<>(
            WaitingState.class,
            StandbyState.class,
            b -> b.isRequestedAndActive(BladeRequest.STANDBY),
            b -> {}
        ));

        addTransition(new Transition<>(
            WaitingState.class,
            WieldState.class,
            b -> b.isRequestedAndActive(BladeRequest.WIELD),
            b -> {}
        ));

        addTransition(new Transition<>(
            WaitingState.class,
            LungingState.class,
            b -> b.isRequestedAndActive(BladeRequest.LUNGE),
            b -> {}
        ));

        addTransition(new Transition<>(
            WaitingState.class,
            AttackingQuickState.class,
            b -> b.isRequestedAndActive(BladeRequest.ATTACK_QUICK),
            b -> {}
        ));

        addTransition(new Transition<>(
            WaitingState.class,
            AttackingHeavyState.class,
            b -> b.isRequestedAndActive(BladeRequest.ATTACK_HEAVY),
            b -> {}
        ));

        // =====================================================================
        // RECALLING / RETURNING
        // =====================================================================
        addTransition(new Transition<>(
            RecallingState.class,
            SheathedState.class,
            b -> b.isRequestedAndActive(BladeRequest.SHEATH),
            b -> {}
        ));

        addTransition(new Transition<>(
            RecallingState.class,
            StandbyState.class,
            b -> b.isRequestedAndActive(BladeRequest.STANDBY),
            b -> {}
        ));

        addTransition(new Transition<>(
            RecallingState.class,
            WieldState.class,
            b -> b.isRequestedAndActive(BladeRequest.WIELD),
            b -> {}
        ));

        addTransition(new Transition<>(
            RecallingState.class,
            LungingState.class,
            b -> b.isRequestedAndActive(BladeRequest.LUNGE), // TODO: #122 - Test this transition
            b -> {}
        ));

        addTransition(new Transition<>(
            RecallingState.class,
            AttackingQuickState.class,
            b -> b.isRequestedAndActive(BladeRequest.ATTACK_QUICK),
            b -> {}
        ));

        addTransition(new Transition<>(
            RecallingState.class,
            AttackingHeavyState.class,
            b -> b.isRequestedAndActive(BladeRequest.ATTACK_HEAVY),
            b -> {}
        ));

        addTransition(new Transition<>(
            RecallingState.class,
            WaitingState.class,
            b -> b.isRequestedAndActive(BladeRequest.WAITING),
            b -> {}
        ));

        // =====================================================================
        // LODGED
        // =====================================================================
        addTransition(new Transition<>(
            LodgedState.class,
            RecallingState.class,
            b -> b.getHitEntity() == null ||
                b.getHitEntity().isInvalid() ||
                b.isRequestedAndActive(BladeRequest.RECALL),
            b -> {
                Location bladeLoc = b.getDisplay().getLocation();
                TimeArbiter.teleportDisplay(b.getDisplay(),
                    bladeLoc.clone().subtract(bladeLoc.getDirection().multiply(6)),
                    null, 10,
                    UmbralStateMachine.class, 302);
                if (b.getHitEntity() != null) {
                    b.getHitEntity().setVelocity(b.getDisplay().getLocation().getDirection().multiply(-0.75));
                }
            }
        ));

        addTransition(new Transition<>(
            LodgedState.class,
            WieldState.class,
            b -> b.isRequestedAndActive(BladeRequest.WIELD),
            b -> {}
        ));

        addTransition(new Transition<>(
            LodgedState.class,
            StandbyState.class,
            b -> b.isRequestedAndActive(BladeRequest.STANDBY),
            b -> {}
        ));

        addTransition(new Transition<>(
            LodgedState.class,
            AttackingHeavyState.class,
            b -> b.isRequestedAndActive(BladeRequest.ATTACK_HEAVY),
            b -> {}
        ));

        // =====================================================================
        // LUNGING
        // =====================================================================
        addTransition(new Transition<>(
            LungingState.class,
            LodgedState.class,
            b -> b.getHitEntity() != null,
            b -> {
                Vector kb = VectorUtil.getProjOntoPlane(b.getVelocity(), Config.Direction.UP())
                    .multiply(Config.Combat.THROWN_DAMAGE_SWORD_AXE_KNOCKBACK_AIRBORNE);
                b.getHitEntity().hit(b.getThrower(), Prefab.Attacks.umbralImpale, kb);
            }
        ));

        addTransition(new Transition<>(
            LungingState.class,
            RecallingState.class,
            UmbralBlade::isFinishedLunging,
            b -> {}
        ));

        addTransition(new Transition<>(
            LungingState.class,
            RecallingState.class,
            b -> b.isRequestedAndActive(BladeRequest.STANDBY),
            b -> {}
        ));
    }

    @Override
    public void onAnyTransition() {

    }

    @Override
    public void tick() {
        if (deactivated) return;

        currentState.onTick(context);
        for (var t : transitions.keySet()) {
            if (t.from().isAssignableFrom(currentState.getClass())
                && t.condition().test(context)) {

                t.onTransition().accept(context);
                if (t.to() == PreviousState.class) {
                    setState(previousState);
                } else {
                    setState(createState(t.to()));
                }
                return;
            }
        }
    }

    @Override
    public void setState(State<UmbralBlade> next) {
        previousState = (UmbralStateFacade) currentState;
        Debug.system(currentState.getClass().getSimpleName() + " -> " + next.getClass().getSimpleName());
        super.setState(next);

        applyGlowForState(next, context);

        @SuppressWarnings("unchecked")
        Class<? extends State<UmbralBlade>> stateClass = (Class<? extends State<UmbralBlade>>) next.getClass();
        context.setDisplayTransformation(stateClass);
    }

    private static void applyGlowForState(State<UmbralBlade> state, UmbralBlade blade) {
        if (blade.getDisplay() == null) return;
        Color color = glowColorForState(state.getClass());
        blade.getDisplay().setGlowing(color != null);
        if (color != null) {
            blade.getDisplay().setGlowColorOverride(color);
        }
    }

    private static Color glowColorForState(Class<?> stateClass) {
        if (stateClass == StandbyState.class) return Config.SwordColor.STANDBY_GLOW;
        if (stateClass == AttackingQuickState.class) return Config.SwordColor.ATTACK_QUICK_GLOW;
        if (stateClass == AttackingHeavyState.class) return Config.SwordColor.FEROCIOUS_SWEEP;
        if (stateClass == LungingState.class) return Config.SwordColor.LUNGE_GLOW;
        if (stateClass == LodgedState.class) return Config.SwordColor.LODGED_GLOW;
        if (stateClass == GrabImpaleState.class) return Config.SwordColor.GRAB_IMPALE_GLOW;
        if (stateClass == RecallingState.class) return Config.SwordColor.RECALL_GLOW;
        if (stateClass == WaitingState.class) return Config.SwordColor.UMBRAL_GLOW;
        return null;
    }
}
