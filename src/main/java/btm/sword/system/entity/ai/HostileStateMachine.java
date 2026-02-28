package btm.sword.system.entity.ai;

import java.util.Collection;
import java.util.List;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import btm.sword.config.Config;
import btm.sword.system.entity.ai.state.ApproachState;
import btm.sword.system.entity.ai.state.AttackState;
import btm.sword.system.entity.ai.state.FleeState;
import btm.sword.system.entity.ai.state.IdleState;
import btm.sword.system.entity.ai.state.PreAttackState;
import btm.sword.system.entity.ai.state.RetreatState;
import btm.sword.system.entity.ai.state.SurroundState;
import btm.sword.system.entity.impl.Hostile;
import btm.sword.utility.statemachine.State;
import btm.sword.utility.statemachine.StateMachine;
import btm.sword.utility.statemachine.Transition;

/**
 * Finite state machine governing Hostile entity AI behavior.
 * <p>
 * Manages transitions between the seven AI states:
 * Idle, Approach, Surround, PreAttack, Attack, Retreat, and Flee.
 * Uses the same {@link StateMachine} infrastructure as the UmbralBlade FSM,
 * with {@link HostileAIFacade} as the common state supertype for wildcard transitions.
 * </p>
 */
public class HostileStateMachine extends StateMachine<Hostile> {

    /**
     * Constructs a new {@code HostileStateMachine} for the given {@link Hostile} context.
     *
     * @param context the Hostile entity this state machine controls
     * @param initialState the initial AI state to enter
     */
    public HostileStateMachine(Hostile context, State<Hostile> initialState) {
        super(context, initialState);
        initTransitions();
    }

    @Override
    public void onAnyTransition() {
        context.broadcastMessage(20, "cur: " + currentState.name());
    }

    @Override
    public void afterAnyTransition() {
        context.broadcastMessage(20, ">>> New State: " + currentState.name());
    }

    /**
     * Registers all FSM transitions in evaluation order.
     * <p>
     * Wildcard transitions (from {@link HostileAIFacade}) are registered first to ensure
     * they are visible in the iteration; however, HashMap ordering is not guaranteed.
     * Conditions are designed to be mutually exclusive where necessary.
     * </p>
     */
    private void initTransitions() {
////        // 1. ANY → FleeState: health fraction drops below threshold
////        addTransition(new Transition<>(
////            HostileAIFacade.class,
////            FleeState.class,
////            h -> !(h.getAiStateMachine().getState() instanceof FleeState)
////                && h.getAttrHealth() != null
////                && h.self().getHealth() / h.getAttrHealth().getValue() < Config.Hostile.FLEE_HEALTH_FRACTION,
////            h -> h.setCurrentTarget(null)
////        ));
//
//        // 2. IdleState → ApproachState: aggro scan found a player (set in IdleState.onTick)
//        addTransition(new Transition<>(
//            IdleState.class,
//            ApproachState.class,
//            h -> h.getNearestScannedTarget() != null,
//            h -> h.setCurrentTarget(h.getNearestScannedTarget())
//        ));
//
////        // 3. ApproachState → SurroundState: enough allies are also targeting the same player
////        addTransition(new Transition<>(
////            ApproachState.class,
////            SurroundState.class,
////            h -> h.getNearbyAlliesCount() >= Config.Hostile.SURROUND_MIN_ALLIES,
////            h -> {}
////        ));
//
//        // 4. ApproachState → PreAttackState: reached attack range
//        addTransition(new Transition<>(
//            ApproachState.class,
//            PreAttackState.class,
//            h -> {
//                if (h.getCurrentTarget() == null || !h.getCurrentTarget().self().isValid()) return false;
//                return h.self().getLocation()
//                    .distanceSquared(h.getCurrentTarget().self().getLocation()) < Config.Hostile.APPROACH_DISTANCE_SQUARED;
//            },
//            h -> {}
//        ));
//
////        // 5. SurroundState → ApproachState: ally count dropped below threshold
////        addTransition(new Transition<>(
////            SurroundState.class,
////            ApproachState.class,
////            h -> h.getNearbyAlliesCount() < Config.Hostile.SURROUND_MIN_ALLIES,
////            h -> {}
////        ));
//
////        // 6. SurroundState → PreAttackState: this mob holds the front arc slot
////        addTransition(new Transition<>(
////            SurroundState.class,
////            PreAttackState.class,
////            Hostile::isFrontSlot,
////            h -> h.setFrontSlot(false)
////        ));
//
//        // 7. PreAttackState → AttackState: wind-up timer expired
//        addTransition(new Transition<>(
//            PreAttackState.class,
//            AttackState.class,
//            h -> h.getPreAttackTimer() <= 0,
//            h -> {}
//        ));
//
//        // 8. AttackState → RetreatState: attack landed
//        addTransition(new Transition<>(
//            AttackState.class,
//            RetreatState.class,
//            Hostile::isAttackDone,
//            h -> {}
//        ));
//
//        // 9. AttackState → ApproachState: target escaped aggro range
//        addTransition(new Transition<>(
//            AttackState.class,
//            ApproachState.class,
//            h -> {
//                if (h.getCurrentTarget() == null || !h.getCurrentTarget().self().isValid()) return true;
//                return h.self().getLocation()
//                    .distanceSquared(h.getCurrentTarget().self().getLocation()) > Config.Hostile.AGGRO_RANGE_SQUARED;
//            },
//            h -> {}
//        ));
//
//        // 10. RetreatState → ApproachState: cooldown elapsed and target still in aggro range
//        addTransition(new Transition<>(
//            RetreatState.class,
//            ApproachState.class,
//            h -> {
//                if (h.getRetreatTimer() > 0) return false;
//                if (h.getCurrentTarget() == null || !h.getCurrentTarget().self().isValid()) return false;
//                return h.self().getLocation()
//                    .distanceSquared(h.getCurrentTarget().self().getLocation()) < Config.Hostile.AGGRO_RANGE_SQUARED;
//            },
//            h -> {}
//        ));
//
//        // 11. RetreatState → IdleState: cooldown elapsed and target left aggro range
//        addTransition(new Transition<>(
//            RetreatState.class,
//            IdleState.class,
//            h -> {
//                if (h.getRetreatTimer() > 0) return false;
//                if (h.getCurrentTarget() == null || !h.getCurrentTarget().self().isValid()) return true;
//                return h.self().getLocation()
//                    .distanceSquared(h.getCurrentTarget().self().getLocation()) >= Config.Hostile.AGGRO_RANGE_SQUARED;
//            },
//            h -> h.setCurrentTarget(null)
//        ));
//
//        // 12. FleeState → IdleState: no player within aggro range
//        addTransition(new Transition<>(
//            FleeState.class,
//            IdleState.class,
//            h -> {
//                double aggroRadius = Math.sqrt(Config.Hostile.AGGRO_RANGE_SQUARED);
//                Collection<Entity> nearby = h.self().getWorld().getNearbyEntities(
//                    h.self().getLocation(), aggroRadius, aggroRadius, aggroRadius,
//                    e -> e instanceof Player
//                );
//                return nearby.isEmpty();
//            },
//            h -> {}
//        ));
    }
}
