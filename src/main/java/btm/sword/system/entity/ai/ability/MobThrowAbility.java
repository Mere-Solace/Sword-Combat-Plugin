package btm.sword.system.entity.ai.ability;

import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import btm.sword.config.Config;
import btm.sword.system.action.throwing.types.DroppedItem;
import btm.sword.system.entity.impl.Hostile;

/**
 * A ranged throwing ability for hostile mobs.
 *
 * <p>Launches the mob's off-hand item as a {@link DroppedItem} with a parabolic arc toward the
 * current target. The mob retreats from the target during the wind-up
 * (category: {@link AbilityCategory#RANGED}).
 */
public class MobThrowAbility implements MobAbility {

    @Override
    public String name() {
        return "mob_throw";
    }

    @Override
    public AbilityCategory category() {
        return AbilityCategory.RANGED;
    }

    @Override
    public boolean canUse(Hostile h) {
        Integer cooldown = h.getAbilityCooldowns().get(name());
        return cooldown == null || cooldown <= 0;
    }

    @Override
    public void execute(Hostile h) {
        if (h.getCurrentTarget() == null || !h.getCurrentTarget().self().isValid()) return;
        ItemStack projectile = h.getItemStackInHand(false);
        if (projectile == null || projectile.isEmpty()) return;

        Vector toTarget = h.getCurrentTarget().self().getLocation()
            .subtract(h.self().getLocation())
            .toVector();

        double distance = toTarget.length();
        if (distance < 0.001) return;

        toTarget.normalize();
        toTarget.setY(toTarget.getY() + Config.Hostile.MOB_THROW_ARC_HEIGHT * distance / 8.0);

        DroppedItem thrown = new DroppedItem(h.getChestLocation(), toTarget, projectile.clone());
        thrown.register();
    }

    @Override
    public int cooldownTicks() {
        return Config.Hostile.MOB_THROW_COOLDOWN_TICKS;
    }
}
