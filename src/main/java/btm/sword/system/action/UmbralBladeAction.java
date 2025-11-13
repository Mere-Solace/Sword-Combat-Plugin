package btm.sword.system.action;

import btm.sword.system.entity.types.Combatant;
import btm.sword.system.entity.umbral.UmbralBlade;

public class UmbralBladeAction extends SwordAction {
    public static void wieldUmbralBlade(Combatant wielder) {
        UmbralBlade blade = wielder.getUmbralBlade();
        if (blade == null) return;

        blade.requestWield();
    }

    public static void toggleUmbralBlade(Combatant wielder) {
        UmbralBlade blade = wielder.getUmbralBlade();
        if (blade == null) return;

        blade.requestToggle();
    }

    public static void performAttack(Combatant wielder) {
        UmbralBlade blade = wielder.getUmbralBlade();
        if (blade == null) return;

        blade.requestAttackQuick();
    }

    public static void recallBlade(Combatant wielder) {
        UmbralBlade blade = wielder.getUmbralBlade();
        if (blade == null) return;

        blade.requestRecall();
    }
}
