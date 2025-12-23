package btm.sword.system.action.throwing;

import org.bukkit.util.Vector;

import btm.sword.system.action.SwordAction;
import btm.sword.system.entity.base.SwordEntity;
import btm.sword.system.entity.types.Combatant;

public class UmbralItemThrowAction extends SwordAction {

    public static void umbralLungePreparation(Combatant executor) {
        // TODO: #149 implement

        // if blade, dash lunge forth and pile-drive the enemy
        SwordEntity target = executor.getTargetedEntity(12);

        if (executor.holdingUmbralBlade()) {
            Vector lungeDirection;

            if (target == null) {

            } else {

            }
        }
    }
}
