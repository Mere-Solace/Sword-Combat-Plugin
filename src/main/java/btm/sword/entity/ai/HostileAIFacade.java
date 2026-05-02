package btm.sword.entity.ai;

import btm.sword.entity.mob.Hostile;
import btm.sword.runtime.statemachine.State;

/**
 * Abstract base facade for all Hostile AI states.
 * <p>
 * Serves as the common supertype for the {@link HostileStateMachine}'s wildcard transitions,
 * mirroring the pattern used by {@code UmbralStateFacade} in the UmbralBlade FSM.
 * All concrete AI states must extend this class.
 * </p>
 */
public abstract class HostileAIFacade extends State<Hostile> {
}
