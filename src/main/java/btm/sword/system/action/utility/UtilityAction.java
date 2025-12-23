package btm.sword.system.action.utility;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.RayTraceResult;

import btm.sword.system.action.SwordAction;
import btm.sword.system.entity.SwordEntityArbiter;
import btm.sword.system.entity.base.SwordEntity;
import btm.sword.system.entity.types.Combatant;
import btm.sword.utility.entity.HitboxUtil;

public class UtilityAction extends SwordAction {
    public static void bulletTime(Combatant executor) {
        cast(executor, 1000, () -> {
            // some cool particle effects or smth
            // TODO: #84 - Implement slow-motion effect (track all vectors and slow them)
            List<PotionEffect> chillOut = List.of(
                new PotionEffect(PotionEffectType.SLOWNESS, 200, 3), // 10 seconds = 200 ticks
                new PotionEffect(PotionEffectType.SLOW_FALLING, 200, 3));
            for (SwordEntity swordEntity :
                SwordEntityArbiter.convertAllToSwordEntities(
                    HitboxUtil.sphere(executor.self(), executor.getLocation(),20,true))) {
                swordEntity.self().addPotionEffects(chillOut);
            }
        });
    }

    @SuppressWarnings("all")
    public static void death(Combatant executor) {
        cast(executor, 0, () -> {
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
        });
    }
}
