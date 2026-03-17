package btm.sword.system.action.utility;


import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.RayTraceResult;

import btm.sword.system.action.SwordAction;
import btm.sword.system.entity.SwordEntityArbiter;
import btm.sword.system.entity.impl.Combatant;

public class UtilityAction extends SwordAction {

    @SuppressWarnings("all")
    public static void death(Combatant executor) {
        LivingEntity ex = executor.self();
        Location l = executor.eyeLoc();
        RayTraceResult ray = ex.getWorld().rayTraceEntities(l, l.getDirection(), 6,
            entity -> entity instanceof LivingEntity livingEntity && !executor.isSelf(livingEntity));
        if (ray != null && ray.getHitEntity() != null) {
            Entity target = ray.getHitEntity();
            if (target instanceof LivingEntity livingEntity) {
                try {
                    SwordEntityArbiter.getOrAdd(livingEntity).hit(
                        executor, 100,
                        0, 7777777, 7777777,
                        1, l.getDirection().multiply(100));
                } catch (NullPointerException e) {
                    // some nonsense occurred
                }
            }
            else {
                target.getWorld().createExplosion(target.getLocation(), 5, true, true);
            }
        }
    }
}
