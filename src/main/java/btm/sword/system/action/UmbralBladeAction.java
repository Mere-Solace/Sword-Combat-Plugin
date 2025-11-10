package btm.sword.system.action;

import btm.sword.system.entity.base.SwordEntity;
import btm.sword.system.entity.types.Combatant;
import btm.sword.system.entity.umbral.UmbralBlade;
import btm.sword.system.entity.umbral.UmbralState;
import lombok.SneakyThrows;

public class UmbralBladeAction extends SwordAction {
    @SneakyThrows
    public static void toggleUmbralBlade(Combatant wielder) {
        UmbralBlade blade = wielder.getUmbralBlade();

        if (blade == null) return;

        switch (blade.getState()) {
            case SHEATHED -> blade.setState(UmbralState.STANDBY);
            case STANDBY -> blade.setState(UmbralState.SHEATHED);
            default -> {} // Other states (FLYING, ATTACKING, RECALLING, LODGED) cannot be toggled
        }
    }

    public static void performAttack(Combatant wielder, boolean heavy) {
        UmbralBlade blade = wielder.getUmbralBlade();
        if (blade == null || blade.getState() != UmbralState.ATTACKING) return;

        SwordEntity target = wielder.getTargetedEntity(5.0); // simple raytrace
        blade.performAttack(target, heavy);
    }

    public static void lungeOrRecall(Combatant wielder) throws InterruptedException {
        UmbralBlade blade = wielder.getUmbralBlade();
        if (blade == null) return;

        if (blade.getState() == UmbralState.ATTACKING) {
            blade.recall();
        } else {
            var target = wielder.getTargetedEntity(12.0);
            blade.lungeToTarget(target);
        }
    }
}
