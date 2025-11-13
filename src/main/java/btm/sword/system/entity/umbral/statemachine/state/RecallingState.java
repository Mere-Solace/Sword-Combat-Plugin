package btm.sword.system.entity.umbral.statemachine.state;

import btm.sword.system.entity.umbral.UmbralBlade;
import btm.sword.system.entity.umbral.statemachine.UmbralState;
import btm.sword.system.statemachine.State;

/**
 * State where the UmbralBlade is being recalled to the wielder.
 * <p>
 * In this state, the blade is traveling back to the player, typically after
 * being thrown, lodged in an enemy, or left in a waiting state.
 * </p>
 * <p>
 * <b>Entry Actions:</b>
 * <ul>
 *   <li>Set display transformation for returning animation</li>
 *   <li>Stop idle movement</li>
 *   <li>Begin lerp movement back to wielder</li>
 * </ul>
 * </p>
 * <p>
 * <b>Typical Transitions:</b>
 * <ul>
 *   <li>RECALLING → SHEATHED (when blade arrives)</li>
 * </ul>
 * </p>
 *
 * @author Claude Code
 * @since 1.0
 */
public class RecallingState extends State<UmbralBlade> {
    @Override
    public String name() {
        return "RECALLING";
    }

    @Override
    public void onEnter(UmbralBlade blade) {
        blade.setDisplayTransformation(UmbralState.RECALLING);
        blade.endIdleMovement();
        blade.returnToSheath();
    }

    @Override
    public void onExit(UmbralBlade blade) {
        // Recall animation cleanup if needed
    }

    @Override
    public void onTick(UmbralBlade blade) {
        // Monitor recall progress
    }
}
