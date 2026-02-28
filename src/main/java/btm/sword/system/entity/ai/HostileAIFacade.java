package btm.sword.system.entity.ai;

import btm.sword.system.entity.impl.Hostile;
import btm.sword.utility.statemachine.State;

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
