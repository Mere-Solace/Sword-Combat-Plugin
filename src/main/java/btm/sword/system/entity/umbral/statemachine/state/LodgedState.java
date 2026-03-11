package btm.sword.system.entity.umbral.statemachine.state;

import org.bukkit.entity.LivingEntity;

import btm.sword.config.Config;
import btm.sword.system.control.TimeArbiter;
import btm.sword.system.entity.base.SwordEntity;
import btm.sword.system.entity.umbral.UmbralBlade;
import btm.sword.system.entity.umbral.statemachine.UmbralStateFacade;
import btm.sword.utility.display.DisplayUtil;

/**
 * State where the UmbralBlade is lodged in an entity or block.
 * <p>
 * In this state, the blade is stuck in a target (enemy entity or solid block)
 * after a throw, lunge, or attack. It remains attached until recalled by the
 * wielder or until the target dies/is destroyed.
 * </p>
 * <p>
 * <b>Entry Actions:</b>
 * <ul>
 *   <li>Stop all movement</li>
 *   <li>Set display transformation for lodged position</li>
 *   <li>Attach display to target entity/block via entity follow task</li>
 * </ul>
 * </p>
 * <p>
 * <b>Exit Actions:</b>
 * <ul>
 *   <li>Cancel entity follow task</li>
 *   <li>Reset flight state</li>
 *   <li>Clear glow</li>
 * </ul>
 * </p>
 * <p>
 * <b>Typical Transitions:</b>
 * <ul>
 *   <li>LODGED → RECALLING (wielder recalls the blade)</li>
 *   <li>LODGED → WAITING (target dies or block destroyed)</li>
 * </ul>
 * </p>
 *
 */
public class LodgedState extends UmbralStateFacade {

    @Override
    public String name() {
        return "LODGED";
    }

    @Override
    public void onEnter(UmbralBlade blade) {
        blade.getDisplay().setGlowing(true);
        blade.getDisplay().setGlowColorOverride(Config.SwordColor.UMBRAL_GLOW);

        SwordEntity target = blade.getHitEntity();

        blade.startImpalementTask(target);
//        if (target != null) {
//            LivingEntity le = target.self();
//            double heightOffset = Math.max(0, Math.min(blade.getCur().getY() - le.getLocation().getY(), le.getHeight()));
//            boolean followHead = !Config.Combat.IMPALEMENT_HEAD_FOLLOW_EXCEPTIONS.contains(target.type())
//                && heightOffset >= (le.getEyeLocation().getY() - le.getLocation().getY()) * Config.Combat.IMPALEMENT_HEAD_ZONE_RATIO;
//
//            followTask = DisplayUtil.itemDisplayFollow(
//                target, blade.getDisplay(),
//                blade.getVelocity().clone().normalize(),
//                heightOffset, followHead,
//                null, null, null, null
//            );
//        }
    }

    @Override
    public void onExit(UmbralBlade blade) {
        blade.getDisplay().setGlowing(false);
        blade.resetFlightState();
    }

    @Override
    public void onTick(UmbralBlade blade) {

    }
}
