package btm.sword.system.entity.ai.state;

import org.bukkit.entity.ItemDisplay;

import com.destroystokyo.paper.entity.ai.GoalType;

import btm.sword.config.Config;
import btm.sword.system.action.throwing.InteractiveItemArbiter;
import btm.sword.system.action.throwing.types.ThrownItem;
import btm.sword.system.entity.ai.HostileAIFacade;
import btm.sword.system.entity.ai.MobGoalArbiter;
import btm.sword.system.entity.ai.goal.LookAtTargetGoal;
import btm.sword.system.entity.ai.goal.RetrieveWeaponGoal;
import btm.sword.system.entity.impl.Hostile;
import btm.sword.utility.Prefab;

/**
 * Weapon-retrieval AI state for Hostile entities.
 *
 * <p>Entered when a mob's thrown weapon lands and becomes lodged in the world.
 * Adds a {@link RetrieveWeaponGoal} to pathfind toward the item's display entity.
 * Each tick checks whether the mob is within pickup range
 * ({@link Config.Hostile#MOB_RETRIEVE_PICKUP_RANGE_SQUARED}); on proximity,
 * retrieves the item back into the mob's main hand.
 *
 * <p>Exits via FSM transition once {@code lodgedThrowItem} is cleared (either by pickup
 * or by the display expiring before the mob arrives).
 */
public class RetrieveWeaponState extends HostileAIFacade {

    @Override
    public String name() {
        return "RETRIEVE_WEAPON";
    }

    @Override
    public void onEnter(Hostile h) {
        MobGoalArbiter.GOALS.addGoal(h.mob(), 1, new RetrieveWeaponGoal(h.mob(), h));
        if (h.getCurrentTarget() != null) {
            MobGoalArbiter.GOALS.addGoal(h.mob(), 2, new LookAtTargetGoal(h.mob(), h));
        }
    }

    @Override
    public void onTick(Hostile h) {
        ThrownItem lodged = h.getLodgedThrowItem();
        if (lodged == null) return;

        ItemDisplay display = lodged.getDisplay();
        if (display == null || !display.isValid()) {
            h.setLodgedThrowItem(null);
            h.onWeaponRetrieved();
            return;
        }

        double distSq = h.self().getLocation().distanceSquared(display.getLocation());
        if (distSq <= Config.Hostile.MOB_RETRIEVE_PICKUP_RANGE_SQUARED) {
            retrieveItem(h, lodged);
        }
    }

    @Override
    public void onExit(Hostile h) {
        MobGoalArbiter.GOALS.removeAllGoals(h.mob(), GoalType.MOVE);
        MobGoalArbiter.GOALS.removeAllGoals(h.mob(), GoalType.LOOK);
    }

    /**
     * Removes the lodged item from the interactive arbiter, returns it to the mob's main hand,
     * plays a pickup particle effect, and clears the lodged reference.
     *
     * @param h      the hostile mob retrieving the weapon
     * @param lodged the grounded {@link ThrownItem} to reclaim
     */
    private void retrieveItem(Hostile h, ThrownItem lodged) {
        ItemDisplay display = lodged.getDisplay();
        InteractiveItemArbiter.remove(display, false);
        lodged.setRetrieved(true);

        if (lodged.getItemStack() != null && !lodged.getItemStack().isEmpty()) {
            h.receiveRetrievedWeapon(lodged.getItemStack());
            Prefab.Particles.GRAB_CLOUD.display(display.getLocation());
        }

        lodged.dispose();
        h.setLodgedThrowItem(null);
        h.onWeaponRetrieved();
    }
}
