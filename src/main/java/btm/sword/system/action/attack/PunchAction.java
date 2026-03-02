package btm.sword.system.action.attack;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import btm.sword.config.Config;
import btm.sword.system.action.SwordAction;
import btm.sword.system.entity.SwordEntityArbiter;
import btm.sword.system.entity.aspect.AspectType;
import btm.sword.system.entity.base.SwordEntity;
import btm.sword.system.entity.impl.Combatant;
import btm.sword.utility.Prefab;
import btm.sword.utility.entity.HitboxUtil;
import btm.sword.utility.math.Basis;
import btm.sword.utility.math.VectorUtil;

public class PunchAction extends SwordAction {

    public static void punch(Combatant executor, boolean right, double distance) {
        double dist = distance < 0 ? 2.5 : distance;
        double spacing = 0.33;

        Prefab.Sounds.PUNCH_ATTEMPT.playForAllInRadius(executor.self());

        Basis basis = VectorUtil.getBasis(executor.eyeLoc(), executor.dir());

        Location originLoc = executor.getChestLocation()
            .add(right ? basis.right().multiply(spacing) : basis.right().multiply(-spacing));

        Location hitLoc = originLoc.clone().add(basis.forward().multiply(dist));

        Prefab.Particles.PUNCH.display(hitLoc);

        Entity hit = HitboxUtil.ray(originLoc, executor.dir(), dist, 1.2,
            entity -> entity instanceof LivingEntity l &&
                !executor.isSelf(l) &&
                SwordEntityArbiter.getOrAdd(l) != null);

        if (hit != null) {
            SwordEntity swordHit = SwordEntityArbiter.getOrAdd((LivingEntity) hit);
            if (swordHit != null) {
                Prefab.Particles.PUNCH_CONNECT.display(hitLoc);
                Prefab.Sounds.PUNCH_CONNECT.playForAllInRadius(executor.self());
                // subtract current velocity for more realistic juggling, rather than just bouncing em in the air forever
                swordHit.hit(executor, Prefab.Attacks.punch,
                    executor.dir().multiply(0.5).add(swordHit.self().getVelocity().multiply(0.5)));
            }
        }
    }

    public static void throwPunch(Combatant executor, boolean right, double distance) {
        executor.setTimeOfLastAttack(System.currentTimeMillis());
        int cooldown = (int) executor.calcValueReductive(AspectType.FINESSE,
            Config.Combat.ATTACKS_CAST_TIMING_MIN_DURATION,
            Config.Combat.ATTACKS_CAST_TIMING_MAX_DURATION,
            Config.Combat.ATTACKS_CAST_TIMING_REDUCTION_RATE);
        executor.setDurationOfLastAttack(cooldown);

        punch(executor, right, distance);
    }
}
