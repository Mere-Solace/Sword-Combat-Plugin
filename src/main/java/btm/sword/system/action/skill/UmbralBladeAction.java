package btm.sword.system.action.skill;

import static btm.sword.system.action.attack.PunchAction.throwPunch;

import java.util.List;
import java.util.concurrent.TimeUnit;

import btm.sword.system.control.TimeArbiter;

import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import btm.sword.Sword;
import btm.sword.system.action.SwordAction;
import btm.sword.system.control.SwordScheduler;
import btm.sword.system.entity.base.SwordEntity;
import btm.sword.system.entity.types.Combatant;
import btm.sword.system.entity.umbral.UmbralBlade;
import btm.sword.system.entity.umbral.input.BladeRequest;
import btm.sword.system.entity.umbral.statemachine.state.LodgedState;
import btm.sword.utility.Prefab;
import btm.sword.utility.display.DrawUtil;
import btm.sword.utility.math.Basis;

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

    public static void basicAttackWithLink(Combatant wielder, int comboStep) {
        UmbralBlade blade = wielder.getUmbralBlade();
        if (blade == null) return;

        if (wielder.getAspects().soulfireCur() >= 10f && !blade.inState(LodgedState.class)) {
            blade.requestQuickAttack(comboStep);
            blade.request(BladeRequest.ATTACK_QUICK);
            return;
        }

        throwPunch(wielder, comboStep == 1 || comboStep == 3,-1);
    }

    public static void spiralFinisher(Combatant wielder) {
        UmbralBlade blade = wielder.getUmbralBlade();
        if (blade == null) return;

        blade.request(BladeRequest.ATTACK_HEAVY);
    }

    public static void shadowBlink(Combatant wielder) {
        UmbralBlade blade = wielder.getUmbralBlade();
        if (blade == null) return;

        SwordEntity target = wielder.getTargetedEntity(40);

        if (target == null || target.isDead() ||
            wielder.getAspects().soulfireCur() < 40.0f) {
            // do smth idk
            return;
        }

        wielder.consumeSoulfire(40.0f);
        cast(wielder, 250, () -> performBlink(wielder, target));
    }

    private static void performBlink(Combatant wielder, SwordEntity target) {
        Prefab.Sounds.SHADOW_BLINK.playForAllInRadius(wielder.self());

        Vector to = target.getChestLocation().toVector().subtract(wielder.getChestLocation().toVector());

        // Second call is correct, want more dense particles
        Prefab.Particles.UMBRAL_BLADE_POOF.display(wielder.getChestLocation());
        Prefab.Particles.UMBRAL_BLADE_POOF.display(wielder.getChestLocation());

        DrawUtil.secant(List.of(Prefab.Particles.SOULFIRE_POOF, Prefab.Particles.UMBRAL_FLAME),
            wielder.getChestLocation(),
            wielder.getChestLocation().toVector().midpoint(
                wielder.getChestLocation().add(to).toVector()).toLocation(wielder.world()),
            0.5f);

        Vector n = to.normalize();
        Vector facing = n.clone().multiply(-1);

        Basis basis = new Basis(wielder.eyeLoc(), false);

        wielder.teleport(target.getChestLocation()
                .add(basis.forward().multiply(target.getAverageSize() * 0.5))
            .add(n.clone().multiply(target.getBodyLength() * 0.25))
            .setDirection(facing));
        SwordScheduler.runBukkitTaskLater(() -> {
            Prefab.Sounds.SHADOW_BLINK.playForAllInRadius(target.self());
            Prefab.Particles.SOULFIRE_POOF.display(wielder.getLocation());
            Prefab.Particles.UMBRAL_FLAME.display(wielder.getLocation());

            wielder.resetAirDashesPerformed(); // give em tools to be cool coming down

            TimeArbiter.runFixedIterationTaskTimer(
                null,
                () -> {
                    target.setVelocity(new Vector());
                    wielder.setVelocity(new Vector());

                    wielder.getUmbralBlade().onGrab(wielder);
                },
                0,50,10,
                UmbralBladeAction.class, "performBlink"
            );
        }, 60, TimeUnit.MILLISECONDS);
    }
}
