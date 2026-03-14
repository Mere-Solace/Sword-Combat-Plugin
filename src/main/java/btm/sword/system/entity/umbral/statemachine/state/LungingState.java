package btm.sword.system.entity.umbral.statemachine.state;

import btm.sword.config.Config;
import btm.sword.system.attack.style.AttackType;
import btm.sword.system.entity.umbral.UmbralBlade;
import btm.sword.system.entity.umbral.statemachine.UmbralStateFacade;

public class LungingState extends UmbralStateFacade {
    @Override
    public String name() {
        return "LUNGING";
    }

    @Override
    public void onEnter(UmbralBlade blade) {
        blade.setHitEntity(null);
        blade.setFinishedLunging(false);
        blade.setTimeCutoff(Config.UmbralBlade.LUNGE_TIME_CUTOFF);
        blade.setTimeScalingFactor(Config.UmbralBlade.LUNGE_TIME_SCALING_FACTOR);
        blade.setCtrlPointsForLunge(AttackType.LUNGE1.controlVectors());
        blade.initFlight(Config.UmbralBlade.LUNGE_ON_RELEASE_VELOCITY);

    }

    @Override
    public void onExit(UmbralBlade blade) {
        blade.setFinishedLunging(false);
    }

    @Override
    public void onTick(UmbralBlade blade) {
        if (blade.stepFlight()) {
            blade.onEnd();
        }
    }
}
