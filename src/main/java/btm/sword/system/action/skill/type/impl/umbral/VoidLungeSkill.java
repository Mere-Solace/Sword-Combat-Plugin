package btm.sword.system.action.skill.type.impl.umbral;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import btm.sword.system.action.UmbralBladeAction;
import btm.sword.system.action.skill.SkillId;
import btm.sword.system.action.skill.SkillIds;
import btm.sword.system.action.skill.SkillType;
import btm.sword.system.action.skill.type.ActiveSkill;
import btm.sword.system.control.SwordScheduler;
import btm.sword.system.control.TimeArbiter;
import btm.sword.system.entity.base.SwordEntity;
import btm.sword.system.entity.impl.Combatant;
import btm.sword.system.entity.umbral.UmbralBlade;
import btm.sword.system.item.ItemStackBuilder;
import btm.sword.utility.Prefab;
import btm.sword.utility.display.DrawUtil;
import btm.sword.utility.math.Basis;
import net.kyori.adventure.text.Component;

public class VoidLungeSkill extends ActiveSkill {

    private static final Component NAME = Component.text("Void Lunge");

    @Override
    public Component name() {
        return NAME;
    }

    @Override
    public SkillId id() {
        return SkillIds.VOID_LUNGE;
    }

    @Override
    public SkillType type() {
        return SkillType.UMBRAL;
    }

    @Override
    public ItemStack icon() {
        return ItemStackBuilder.of(Material.ENDER_PEARL)
            .name(Component.text("Void Lunge"))
            .hideAll()
            .build();
    }

    @Override
    public List<Component> description() {
        return List.of(
            Component.text("Dash behind an impaled foe and reclaim your blade.")
        );
    }

    @Override
    public void execute(Combatant combatant) {
        UmbralBlade blade = combatant.getUmbralBlade();
        if (blade == null) return;

        SwordEntity target = combatant.getTargetedEntity(40);

        if (target == null || target.isDead()) {
            return;
        }

        performBlink(combatant, target);
    }

    private void performBlink(Combatant combatant, SwordEntity target) {

        Prefab.Sounds.SHADOW_BLINK.playForAllInRadius(combatant.self());

        Vector to = target.getChestLocation().toVector().subtract(combatant.getChestLocation().toVector());

        // Second call is correct, want more dense particles
        Prefab.Particles.UMBRAL_BLADE_POOF.display(combatant.getChestLocation());
        Prefab.Particles.UMBRAL_BLADE_POOF.display(combatant.getChestLocation());

        DrawUtil.secant(List.of(Prefab.Particles.SOULFIRE_POOF, Prefab.Particles.UMBRAL_FLAME),
            combatant.getChestLocation(),
            combatant.getChestLocation().toVector().midpoint(
                combatant.getChestLocation().add(to).toVector()).toLocation(combatant.world()),
            0.5f);

        Vector n = to.normalize();
        Vector facing = n.clone().multiply(-1);

        Basis basis = new Basis(combatant.eyeLoc(), false);

        combatant.teleport(target.getChestLocation()
            .add(basis.forward().multiply(target.getAverageSize() * 0.5))
            .add(n.clone().multiply(target.getBodyLength() * 0.25))
            .setDirection(facing));
        SwordScheduler.runBukkitTaskLater(() -> {
            Prefab.Sounds.SHADOW_BLINK.playForAllInRadius(target.self());
            Prefab.Particles.SOULFIRE_POOF.display(combatant.getLocation());
            Prefab.Particles.UMBRAL_FLAME.display(combatant.getLocation());

            combatant.resetAirDashesPerformed(); // give em tools to be cool coming down

            TimeArbiter.runFixedIterationTaskTimer(
                null,
                () -> {
                    target.setVelocity(new Vector());
                    combatant.setVelocity(new Vector());

                    combatant.getUmbralBlade().onGrab(combatant);
                },
                0,50,10,
                UmbralBladeAction.class, "performBlink",
                null
            );
        }, 60, TimeUnit.MILLISECONDS);
    }

    @Override
    public int calculateCooldown(Combatant combatant) {
        return 0;
    }

    @Override
    public boolean canPerform(Combatant combatant) {
        return combatant.canPerformAction();
    }
}
