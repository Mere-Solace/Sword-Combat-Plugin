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
        blade.onRelease(Config.UmbralBlade.LUNGE_ON_RELEASE_VELOCITY);

        blade.getDisplay().setGlowing(true);
        blade.getDisplay().setGlowColorOverride(Config.SwordColor.UMBRAL_GLOW);
    }

    @Override
    public void onExit(UmbralBlade blade) {
        blade.setFinishedLunging(false);
        blade.getDisplay().setGlowing(false);
        blade.cleanupBeforeNewThrow();
    }

    @Override
    public void onTick(UmbralBlade blade) {

    }
}
