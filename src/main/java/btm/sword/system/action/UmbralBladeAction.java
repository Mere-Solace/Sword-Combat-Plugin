package btm.sword.system.action;

import static btm.sword.system.action.attack.PunchAction.throwPunch;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.bukkit.util.Vector;

import btm.sword.config.Config;
import btm.sword.system.action.attack.AttackAction;
import btm.sword.system.attack.Attack;
import btm.sword.system.attack.style.AttackType;
import btm.sword.system.control.PredicateRunnablePair;
import btm.sword.system.control.SwordScheduler;
import btm.sword.system.control.TimeArbiter;
import btm.sword.system.display.BossBarManager;
import btm.sword.system.display.SwordBossBar;
import btm.sword.system.entity.base.SwordEntity;
import btm.sword.system.entity.impl.Combatant;
import btm.sword.system.entity.impl.SwordPlayer;
import btm.sword.system.entity.umbral.UmbralBlade;
import btm.sword.system.entity.umbral.input.BladeRequest;
import btm.sword.system.entity.umbral.statemachine.state.LodgedState;
import btm.sword.system.entity.umbral.statemachine.state.WieldState;
import btm.sword.system.input.ActivationContext;
import btm.sword.system.input.InputType;
import btm.sword.utility.Debug;
import btm.sword.utility.Prefab;
import btm.sword.utility.display.DrawUtil;
import btm.sword.utility.math.Basis;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class UmbralBladeAction extends SwordAction {
    public static void wield(Combatant wielder) {

        Debug.umbral("wield called");

        UmbralBlade blade = wielder.getUmbralBlade();
        if (blade == null) return;

        if (blade.inState(WieldState.class)) {
            blade.request(BladeRequest.TOGGLE);
        }
        else {
            blade.request(BladeRequest.WIELD);
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

    public static void wieldedUmbralBladeAttack(Combatant wielder, int comboStep) {
        if (!(wielder instanceof SwordPlayer sp)) return;

        UmbralBlade blade = sp.getUmbralBlade();

        Debug.umbral("performing wielded umbral blade attack. reclaim type="+blade.getReclaimType());

        switch (blade.getReclaimType()) {
            case NONE -> AttackAction.basicAttack(wielder, comboStep);
            case CIRCULAR_SLASH -> {
                circularReclaimSlash(wielder);
                blade.setReclaimType(UmbralBlade.ReclaimType.NONE);
            }
            case EVISCERATE -> {

            }
            case FORWARD_RUSH -> {
                // TODO: implement forward rush & way to trigger.
                blade.setReclaimType(UmbralBlade.ReclaimType.NONE);
            }
            default -> {} // Shouldn't reach
        }
    }

    public static void circularReclaimSlash(Combatant wielder) {
        new Attack(wielder.getItemStackInHand(true), // TODO: Store somewhere
            AttackType.BLADE_RETRIEVAL_CIRCULAR_SLASH,
            false,
            Config.Combat.CIRCULAR_SLASH_DURATION_MS, Config.Combat.CIRCULAR_SLASH_ITERATIONS,
            0, 1).execute(wielder);
    }

    public static void basicAttackWithLink(Combatant wielder, int comboStep) {
        UmbralBlade blade = wielder.getUmbralBlade();
        if (blade == null) return;

        if (wielder.getAspects().soulfireCur() >= (float) Config.Combat.LINK_ATTACK_SOULFIRE_COST && !blade.inState(LodgedState.class)) {
            blade.requestQuickAttack(comboStep);
            return;
        }

        if (comboStep != 1 && wielder instanceof SwordPlayer sp) {
            sp.resetTree();
            sp.act(InputType.LEFT);
            return;
        }
        throwPunch(wielder, comboStep == 1 || comboStep == 3,-1);
    }

    public static void beginHealChannel(Combatant wielder) {
        Debug.umbral("heal channeling");

        if (wielder instanceof SwordPlayer sp) {
            sp.cancelBlockDrainTask();
            sp.setBlocking(false);
        }

        UmbralBlade blade = wielder.getUmbralBlade();
        if (blade == null || !blade.inState(WieldState.class)) return;
        if (!(wielder instanceof SwordPlayer sp)) return;

        float cost = (float) Config.Combat.CHANNEL_SOULFIRE_COST;

        if (wielder.getAspects().soulfireCur() < cost) return;

        sp.setActivationContext(ActivationContext.CHANNELING);
        sp.setChannelInterrupted(false);

        final AtomicInteger iterationsPassed = new AtomicInteger(0);
        int healingDuration = (int) Config.Combat.CHANNEL_DURATION_MS;
        int period = Config.Combat.CHANNEL_HEAL_PERIOD;
        final int iterationsRequired = healingDuration / period;
        final float soulfirePerIteration = cost / iterationsRequired;

        final AtomicBoolean overspend = new AtomicBoolean(false);
        final AtomicBoolean healComplete = new AtomicBoolean(false);

        SwordBossBar channelBar = BossBarManager.create(
            Component.text("Healing", NamedTextColor.GREEN),
            0f,
            BossBar.Color.GREEN
        );
        channelBar.addViewer(sp.getPlayer());

        Runnable cleanupChannel = () -> {
            sp.setHealChannelTask(null);
            sp.setActivationContext(ActivationContext.NORMAL);
            channelBar.remove();
        };

        sp.setHealChannelTask(TimeArbiter.runTimeBoundBukkitTaskOnTimer(
            null,
            () -> {
                iterationsPassed.incrementAndGet();

                if (sp.changeSoulfire(-soulfirePerIteration)) {
                    overspend.set(true);
                    sp.setActivationContext(ActivationContext.NORMAL);
                    return;
                }

                channelBar.setProgress((float) iterationsPassed.get() / iterationsRequired);

                if (iterationsPassed.get() >= iterationsRequired
                    && !sp.isChannelInterrupted()
                    && sp.getActivationContext().equals(ActivationContext.CHANNELING)) {
                    Debug.umbral("My lad, you're healing yourself!");

                    sp.changeShards(Config.Combat.CHANNEL_HEAL_AMOUNT);
                    Prefab.Particles.SOULFIRE_POOF.display(sp.getChestLocation());
                    Prefab.Sounds.SHADOW_BLINK.playForAllInRadius(sp.self());
                    healComplete.set(true);
                    return;
                }

                Prefab.PotionEffects.HEAL_CHANNEL_SLOW.apply(wielder);
                Prefab.Particles.SMOKE.display(wielder.getLocation());
            },
            null,
            0, period,
            UmbralBladeAction.class, "channel",
            new PredicateRunnablePair(
                () -> sp.isChannelInterrupted()
                    || !sp.getActivationContext().equals(ActivationContext.CHANNELING)
                    || !sp.isSneakingAndHoldingRight(),
                () -> {
                    Debug.umbral(String.format(
                        "channel break -> interrupt=%s, ctx=%s, sneakRight=%s",
                        sp.isChannelInterrupted(),
                        sp.getActivationContext(),
                        sp.isSneakingAndHoldingRight()
                    ));
                    cleanupChannel.run();
                }
            ),
            new PredicateRunnablePair(
                overspend::get,
                cleanupChannel
            ),
            new PredicateRunnablePair(
                healComplete::get,
                cleanupChannel
            )
        ));
    }

    public static void hoverBlade(Combatant wielder) {
        UmbralBlade blade = wielder.getUmbralBlade();
        if (blade == null) return;

        blade.request(BladeRequest.WAITING);
    }

    public static void spiralFinisher(Combatant wielder) {
        UmbralBlade blade = wielder.getUmbralBlade();
        if (blade == null) return;

        blade.request(BladeRequest.FINISHER);
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

        performBlink(wielder, target);
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
                UmbralBladeAction.class, "performBlink",
                null
            );
        }, 60, TimeUnit.MILLISECONDS);
    }
}
