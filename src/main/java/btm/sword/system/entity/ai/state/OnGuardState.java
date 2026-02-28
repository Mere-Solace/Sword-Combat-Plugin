package btm.sword.system.entity.ai.state;

import btm.sword.system.entity.ai.HostileAIFacade;
import btm.sword.system.entity.impl.Hostile;

public class OnGuardState extends HostileAIFacade {

    @Override
    public String name() {
        return "";
    }

    @Override
    public void onEnter(Hostile context) {
        // Back off, maybe throw out a block if the player approaches (Differentiating particle effect and sound for this)
        // Give the player about 3 seconds to think.
    }

    @Override
    public void onExit(Hostile context) {

    }

    @Override
    public void onTick(Hostile context) {

    }
}
