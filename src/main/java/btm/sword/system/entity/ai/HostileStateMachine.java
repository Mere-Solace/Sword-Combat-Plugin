package btm.sword.system.entity.ai;

import java.util.Collection;

import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import btm.sword.config.Config;
import btm.sword.system.entity.ai.state.ApproachState;
import btm.sword.system.entity.ai.state.AttackReadyState;
import btm.sword.system.entity.ai.state.AttackState;
import btm.sword.system.entity.ai.state.FleeState;
import btm.sword.system.entity.ai.state.IdleState;
import btm.sword.system.entity.ai.state.OnGuardState;
import btm.sword.system.entity.ai.state.PreAttackState;
import btm.sword.system.entity.ai.state.RetreatState;
import btm.sword.system.entity.ai.state.RetrieveWeaponState;
import btm.sword.system.entity.ai.state.SurroundState;
import btm.sword.system.entity.impl.Hostile;
import btm.sword.utility.statemachine.State;
import btm.sword.utility.statemachine.StateMachine;
import btm.sword.utility.statemachine.Transition;

/**
 * Finite state machine governing Hostile entity AI behavior.
 *
 * <p>Manages transitions between the seven AI states: Idle, Approach, Surround, PreAttack,
 * Attack, Retreat, and Flee. Uses the same {@link StateMachine} infrastructure as the
 * UmbralBlade FSM, with {@link HostileAIFacade} as the common state supertype for wildcard
 * transitions.
 */
public class HostileStateMachine extends StateMachine<Hostile> {

    /**
     * Constructs a new {@code HostileStateMachine} for the given {@link Hostile} context.
     *
     * @param context      the Hostile entity this state machine controls
     * @param initialState the initial AI state to enter
     */
    public HostileStateMachine(Hostile context, State<Hostile> initialState) {
        super(context, initialState);
        initTransitions();
    }

    @Override
    public void onAnyTransition() {
        if (Config.Debug.LOGGING_VERBOSE_HOSTILE) {
            context.broadcastMessage(20, "cur: " + currentState.name());
        }
    }

    @Override
    public void afterAnyTransition() {
        if (Config.Debug.LOGGING_VERBOSE_HOSTILE) {
            context.broadcastMessage(20, ">>> New State: " + currentState.name());
        }
    }

    @Override
    public void tick() {
        context.refreshTargetDistanceCache();
        super.tick();
    }

    /**
     * Registers all 20 FSM transitions.
     *
     * <p>Wildcard transitions (from {@link HostileAIFacade}) are registered first.
     * Conditions are designed to be mutually exclusive where necessary. See the README
     * and {@code docs/systems/hostile-ai.md} for the full transition table.
     */
    private void initTransitions() {
        // 1. ANY → FleeState: health fraction drops below threshold
        addTransition(new Transition<>(
            HostileAIFacade.class,
            FleeState.class,
            h -> {
                if (h.getAiStateMachine().getState() instanceof FleeState) return false;
                var attr = h.self().getAttribute(Attribute.MAX_HEALTH);
                if (attr == null) return false;
                return h.self().getHealth() / attr.getValue() < Config.Hostile.FLEE_HEALTH_FRACTION;
            },
            h -> {
                h.setCurrentTarget(null);
                h.setAggroTarget(null);
            }
        ));

        // 2. ANY → RetrieveWeaponState: mob's thrown weapon has landed and needs retrieval
        addTransition(new Transition<>(
            HostileAIFacade.class,
            RetrieveWeaponState.class,
            h -> {
                if (h.getAiStateMachine().getState() instanceof RetrieveWeaponState) return false;
                if (h.getAiStateMachine().getState() instanceof FleeState) return false;
                if (h.isAttemptingThrow()) return false;
                return h.getLodgedThrowItem() != null
                    && h.getLodgedThrowItem().getDisplay() != null
                    && h.getLodgedThrowItem().getDisplay().isValid();
            },
            h -> {}
        ));

        // 3. ANY → IdleState: current target switched to creative or spectator mid-combat
        addTransition(new Transition<>(
            HostileAIFacade.class,
            IdleState.class,
            h -> {
                if (h.getAiStateMachine().getState() instanceof IdleState) return false;
                if (h.getCurrentTarget() == null) return false;
                return Hostile.isInvulnerableGameMode(h.getCurrentTarget());
            },
            h -> {
                h.setCurrentTarget(null);
                h.setAggroTarget(null);
            }
        ));

        // 3. IdleState → ApproachState: aggro scan found a player
        addTransition(new Transition<>(
            IdleState.class,
            ApproachState.class,
            h -> h.getNearestScannedTarget() != null,
            h -> {
                h.setCurrentTarget(h.getNearestScannedTarget());
                h.setAggroTarget(h.getNearestScannedTarget());
            }
        ));

        // 4. ApproachState → SurroundState: enough allies targeting the same player
        addTransition(new Transition<>(
            ApproachState.class,
            SurroundState.class,
            h -> h.getNearbyAlliesCount() >= Config.Hostile.SURROUND_MIN_ALLIES,
            h -> {}
        ));

        // 5. ApproachState → PreAttackState: reached attack range
        addTransition(new Transition<>(
            ApproachState.class,
            PreAttackState.class,
            h -> {
                if (h.getCurrentTarget() == null || !h.getCurrentTarget().self().isValid()) return false;
                return h.getCachedDistSqToTarget() < Config.Hostile.APPROACH_DISTANCE_SQUARED;
            },
            h -> {}
        ));

        // 6. SurroundState → ApproachState: ally count dropped below threshold
        addTransition(new Transition<>(
            SurroundState.class,
            ApproachState.class,
            h -> h.getNearbyAlliesCount() < Config.Hostile.SURROUND_MIN_ALLIES,
            h -> {}
        ));

        // 7. SurroundState → PreAttackState: this mob holds the front arc slot
        addTransition(new Transition<>(
            SurroundState.class,
            PreAttackState.class,
            Hostile::isFrontSlot,
            h -> h.setFrontSlot(false)
        ));

        // 8. PreAttackState → AttackState: wind-up timer expired and mob is not incapacitated
        addTransition(new Transition<>(
            PreAttackState.class,
            AttackState.class,
            h -> h.getPreAttackTimer() <= 0 && !h.isIncapacitated(),
            h -> {}
        ));

        // 9. AttackState → OnGuardState: attack landed, roll=0, target in aggro range
        addTransition(new Transition<>(
            AttackState.class,
            OnGuardState.class,
            h -> {
                if (!h.isAttackDone() || h.getAttackPostRoll() != 0) return false;
                if (h.getCurrentTarget() == null || !h.getCurrentTarget().self().isValid()) return false;
                return h.getCachedDistSqToTarget() <= Config.Hostile.AGGRO_RANGE_SQUARED;
            },
            h -> {}
        ));

        // 10. AttackState → AttackReadyState: attack landed, roll=1, target in aggro range
        addTransition(new Transition<>(
            AttackState.class,
            AttackReadyState.class,
            h -> {
                if (!h.isAttackDone() || h.getAttackPostRoll() != 1) return false;
                if (h.getCurrentTarget() == null || !h.getCurrentTarget().self().isValid()) return false;
                return h.getCachedDistSqToTarget() <= Config.Hostile.AGGRO_RANGE_SQUARED;
            },
            h -> {}
        ));

        // 11. AttackState → AttackState (combo): attack landed, roll=2, target in aggro range
        addTransition(new Transition<>(
            AttackState.class,
            AttackState.class,
            h -> {
                if (!h.isAttackDone() || h.getAttackPostRoll() != 2) return false;
                if (h.getCurrentTarget() == null || !h.getCurrentTarget().self().isValid()) return false;
                return h.getCachedDistSqToTarget() <= Config.Hostile.AGGRO_RANGE_SQUARED;
            },
            h -> h.setCombo(true)
        ));

        // 12. AttackState → IdleState: attack landed but target left range or is invalid
        addTransition(new Transition<>(
            AttackState.class,
            IdleState.class,
            h -> {
                if (!h.isAttackDone()) return false;
                if (h.getCurrentTarget() == null || !h.getCurrentTarget().self().isValid()) return true;
                return h.getCachedDistSqToTarget() > Config.Hostile.AGGRO_RANGE_SQUARED;
            },
            h -> {
                h.setCurrentTarget(null);
                if (h.getAggroTarget() != null && !h.getAggroTarget().self().isValid()) {
                    h.setAggroTarget(null);
                }
            }
        ));

        // 13. (removed) AttackState → ApproachState on target escape is obsolete:
        //     attack now fires immediately on entry, so attackDone is always true before any tick.

        // 14. OnGuardState → PreAttackState: on-guard timer expired and target still in range
        addTransition(new Transition<>(
            OnGuardState.class,
            PreAttackState.class,
            h -> {
                if (h.getOnGuardTimer() > 0) return false;
                if (h.getCurrentTarget() == null || !h.getCurrentTarget().self().isValid()) return false;
                return h.getCachedDistSqToTarget() <= Config.Hostile.AGGRO_RANGE_SQUARED;
            },
            h -> {}
        ));

        // 15. OnGuardState → IdleState: on-guard timer expired and target left or is invalid
        addTransition(new Transition<>(
            OnGuardState.class,
            IdleState.class,
            h -> {
                if (h.getOnGuardTimer() > 0) return false;
                if (h.getCurrentTarget() == null || !h.getCurrentTarget().self().isValid()) return true;
                return h.getCachedDistSqToTarget() > Config.Hostile.AGGRO_RANGE_SQUARED;
            },
            h -> {
                h.setCurrentTarget(null);
                if (h.getAggroTarget() != null && !h.getAggroTarget().self().isValid()) {
                    h.setAggroTarget(null);
                }
            }
        ));

        // 16. AttackReadyState → AttackState: hold timer expired, target valid and in range
        addTransition(new Transition<>(
            AttackReadyState.class,
            AttackState.class,
            h -> {
                if (h.getAttackReadyTimer() > 0) return false;
                if (h.getCurrentTarget() == null || !h.getCurrentTarget().self().isValid()) return false;
                return h.getCachedDistSqToTarget() <= Config.Hostile.AGGRO_RANGE_SQUARED;
            },
            h -> {}
        ));

        // 17. AttackReadyState → IdleState: hold timer expired, target left or is invalid
        addTransition(new Transition<>(
            AttackReadyState.class,
            IdleState.class,
            h -> {
                if (h.getAttackReadyTimer() > 0) return false;
                if (h.getCurrentTarget() == null || !h.getCurrentTarget().self().isValid()) return true;
                return h.getCachedDistSqToTarget() > Config.Hostile.AGGRO_RANGE_SQUARED;
            },
            h -> {
                h.setCurrentTarget(null);
                if (h.getAggroTarget() != null && !h.getAggroTarget().self().isValid()) {
                    h.setAggroTarget(null);
                }
            }
        ));

        // 12. RetreatState → ApproachState: cooldown elapsed and target still in aggro range
        addTransition(new Transition<>(
            RetreatState.class,
            ApproachState.class,
            h -> {
                if (h.getRetreatTimer() > 0) return false;
                if (h.getCurrentTarget() == null || !h.getCurrentTarget().self().isValid()) return false;
                return h.getCachedDistSqToTarget() < Config.Hostile.AGGRO_RANGE_SQUARED;
            },
            h -> {}
        ));

        // 11. RetreatState → IdleState: cooldown elapsed and target left aggro range
        addTransition(new Transition<>(
            RetreatState.class,
            IdleState.class,
            h -> {
                if (h.getRetreatTimer() > 0) return false;
                if (h.getCurrentTarget() == null || !h.getCurrentTarget().self().isValid()) return true;
                return h.getCachedDistSqToTarget() >= Config.Hostile.AGGRO_RANGE_SQUARED;
            },
            h -> {
                h.setCurrentTarget(null);
                // Preserve aggroTarget so the mob re-engages if the player re-enters range.
                // Only clear it when the entity is no longer valid (dead / despawned).
                if (h.getAggroTarget() != null && !h.getAggroTarget().self().isValid()) {
                    h.setAggroTarget(null);
                }
            }
        ));

        // 14. FleeState → IdleState: no player within aggro range
        addTransition(new Transition<>(
            FleeState.class,
            IdleState.class,
            h -> {
                double aggroRadius = Math.sqrt(Config.Hostile.AGGRO_RANGE_SQUARED);
                Collection<Entity> nearby = h.self().getWorld().getNearbyEntities(
                    h.self().getLocation(), aggroRadius, aggroRadius, aggroRadius,
                    e -> e instanceof Player
                );
                return nearby.isEmpty();
            },
            h -> {}
        ));

        // 15. RetrieveWeaponState → ApproachState: weapon retrieved, target still in aggro range
        addTransition(new Transition<>(
            RetrieveWeaponState.class,
            ApproachState.class,
            h -> {
                if (h.getLodgedThrowItem() != null) return false;
                if (h.getCurrentTarget() == null || !h.getCurrentTarget().self().isValid()) return false;
                return h.getCachedDistSqToTarget() <= Config.Hostile.AGGRO_RANGE_SQUARED;
            },
            h -> {}
        ));

        // 16. RetrieveWeaponState → IdleState: weapon retrieved (or expired), target gone or out of range
        addTransition(new Transition<>(
            RetrieveWeaponState.class,
            IdleState.class,
            h -> {
                if (h.getLodgedThrowItem() != null) return false;
                if (h.getCurrentTarget() == null || !h.getCurrentTarget().self().isValid()) return true;
                return h.getCachedDistSqToTarget() > Config.Hostile.AGGRO_RANGE_SQUARED;
            },
            h -> {
                h.setCurrentTarget(null);
                if (h.getAggroTarget() != null && !h.getAggroTarget().self().isValid()) {
                    h.setAggroTarget(null);
                }
            }
        ));
    }
}
