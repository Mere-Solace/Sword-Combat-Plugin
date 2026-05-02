package btm.sword.action.attack;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import btm.sword.action.core.SwordAction;
import btm.sword.entity.arbiter.SwordEntityArbiter;
import btm.sword.entity.base.Combatant;
import btm.sword.entity.base.SwordEntity;
import btm.sword.util.entity.HitboxUtil;
import btm.sword.util.math.Basis;
import btm.sword.util.math.VectorUtil;
import btm.sword.util.prefab.Prefab;

/** Handles unarmed punch attacks including hitbox detection and connect feedback. */
public class PunchAction extends SwordAction {

    /** Performs a punch in the given direction, checking for hit entities along a ray from the executor's chest. */
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
                swordHit.hit(executor, Prefab.Attacks.PUNCH,
                    executor.dir().multiply(0.5).add(swordHit.self().getVelocity().multiply(0.5)));
            }
        }
    }

    /** Applies an attack cooldown and immediately executes a punch. */
    public static void throwPunch(Combatant executor, boolean right, double distance) {
        executor.applyAttackCooldown();
        punch(executor, right, distance);
    }
}
