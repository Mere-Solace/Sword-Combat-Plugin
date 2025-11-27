package btm.sword.system.action.skill;

import btm.sword.system.action.SwordAction;
import btm.sword.system.entity.types.Combatant;
import btm.sword.system.entity.umbral.UmbralBlade;
import btm.sword.system.entity.umbral.input.BladeRequest;
import btm.sword.system.entity.umbral.statemachine.state.LodgedState;

public class UmbralBladeAction extends SwordAction {
    // TODO: #122 - Wielding when not holding blade should attack
    public static void wield(Combatant wielder) {
        UmbralBlade blade = wielder.getUmbralBlade();
        if (blade == null) return;

        if (wielder.holdingUmbralBlade()) {
            blade.request(BladeRequest.TOGGLE);
        }
        else if (wielder.holdingSoulLink()) {
            blade.request(BladeRequest.WIELD);
        }
        else {
            blade.request(BladeRequest.ATTACK_QUICK);
        }
    }

    public static void toggle(Combatant wielder) {
        UmbralBlade blade = wielder.getUmbralBlade();
        if (blade == null) return;

        blade.request(BladeRequest.TOGGLE);
    }

    public static void lunge(Combatant wielder) {
        UmbralBlade blade = wielder.getUmbralBlade();
        if (blade == null) return;

        if (blade.inState(LodgedState.class)) {
            blade.request(BladeRequest.RECALL);
        }
        else {
            blade.request(BladeRequest.LUNGE);
        }
    }

    public static void sweep(Combatant wielder) {
        UmbralBlade blade = wielder.getUmbralBlade();
        if (blade == null) return;

        blade.request(BladeRequest.ATTACK_HEAVY);
    }

    public static void spiralFinisher(Combatant wielder) {
        UmbralBlade blade = wielder.getUmbralBlade();
        if (blade == null) return;

        blade.request(BladeRequest.ATTACK_HEAVY);
    }
}
