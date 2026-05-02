package btm.sword.entity.ai.ability;

import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import btm.sword.combat.attack.MobSweepAttack;
import btm.sword.combat.style.AttackType;
import btm.sword.config.Config;
import btm.sword.entity.mob.Hostile;

/**
 * A melee slash ability for hostile mobs.
 *
 * <p>Randomly selects one of {@link AttackType#SLASH1}, {@link AttackType#SLASH2}, or
 * {@link AttackType#SLASH3} and executes a {@link MobSweepAttack} using
 * {@link btm.sword.util.prefab.Prefab.Attacks#DEFAULT_MOB_HIT}.
 * The mob approaches the target during the wind-up (category: {@link AbilityCategory#MELEE}).
 */
public class MobSlashAbility implements MobAbility {

    private static final AttackType[] SLASH_TYPES = {AttackType.SLASH1, AttackType.SLASH2, AttackType.SLASH3};

    @Override
    public String name() {
        return "mob_slash";
    }

    @Override
    public AbilityCategory category() {
        return AbilityCategory.MELEE;
    }

    @Override
    public boolean canUse(Hostile h) {
        return h.getAbilityCooldown(name()) <= 0;
    }

    @Override
    public void execute(Hostile h) {
        if (h.getCurrentTarget() == null || !h.getCurrentTarget().self().isValid()) return;
        AttackType type = SLASH_TYPES[ThreadLocalRandom.current().nextInt(SLASH_TYPES.length)];
        MobSweepAttack attack = new MobSweepAttack(ItemStack.of(Material.STONE_SWORD), type, false);
        attack.execute(h);
    }

    @Override
    public int cooldownTicks() {
        return Config.Hostile.MOB_SLASH_COOLDOWN_TICKS;
    }
}
