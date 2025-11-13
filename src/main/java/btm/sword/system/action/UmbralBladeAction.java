package btm.sword.system.action;

import btm.sword.system.entity.types.Combatant;
import btm.sword.system.entity.umbral.UmbralBlade;
import btm.sword.system.entity.umbral.state.UmbralState;

public class UmbralBladeAction extends SwordAction {
    public static void wieldUmbralBlade(Combatant wielder) {
        UmbralBlade blade = wielder.getUmbralBlade();
        if (blade == null) return;

        switch (blade.getState()) {
            case SHEATHED -> blade.setState(UmbralState.WIELD);
            default -> { } // Other states (FLYING, ATTACKING, RECALLING, LODGED) cannot be toggled
        }
    }
    // TODO, yeah I need a better state machine system for all this, switch statements are everywhere.
    public static void toggleUmbralBlade(Combatant wielder) {
        UmbralBlade blade = wielder.getUmbralBlade();
        if (blade == null) return;

        switch (blade.getState()) {
            case SHEATHED, WIELD -> blade.setState(UmbralState.STANDBY);
            case STANDBY -> blade.setState(UmbralState.SHEATHED);
            default -> { } // Other states (FLYING, ATTACKING, RECALLING, LODGED) cannot be toggled
        }
    }

    public static void performAttack(Combatant wielder) {
        UmbralBlade blade = wielder.getUmbralBlade();
        if (blade == null) return;

        blade.setState(UmbralState.ATTACKING_QUICK);
    }
}
