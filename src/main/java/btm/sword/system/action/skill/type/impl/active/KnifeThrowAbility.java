package btm.sword.system.action.skill.type.impl.active;

import java.util.List;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import btm.sword.system.action.skill.AbilityUseType;
import btm.sword.system.action.skill.SkillId;
import btm.sword.system.action.skill.SkillIds;
import btm.sword.system.action.skill.SkillType;
import btm.sword.system.action.skill.type.ActivatableAbility;
import btm.sword.system.action.throwing.ThrowAction;
import btm.sword.system.entity.impl.Combatant;
import btm.sword.system.item.AbilityItemBuilder;
import btm.sword.system.item.ItemStackBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * A throwing knife ability that consumes one knife per throw and applies a short cooldown.
 *
 * <p>Uses both {@link AbilityUseType#STACK} and {@link AbilityUseType#COOLDOWN} — each throw
 * decrements the slot's use count, and a brief cooldown overlay prevents rapid re-casting.
 * When all uses are exhausted the slot becomes DEPLETED.</p>
 *
 * <p>The projectile is a diamond sword thrown at half scale for a compact visual.
 * The throw fires immediately on activation without any aim-windup animation.</p>
 */
public class KnifeThrowAbility extends ActivatableAbility {

    private static final Component NAME = Component.text("Throwing Knife");

    /** Projectile scale relative to a normal thrown sword. */
    private static final float PROJECTILE_SCALE = 0.5f;

    /** Initial velocity magnitude passed to the physics simulation. */
    private static final double PROJECTILE_VELOCITY = 2.0;

    @Override
    public Component name() {
        return NAME;
    }

    @Override
    public SkillId id() {
        return SkillIds.KNIFE_THROW;
    }

    @Override
    public SkillType type() {
        return SkillType.ACTIVE;
    }

    @Override
    public ItemStack icon() {
        return ItemStackBuilder.of(Material.DIAMOND_SWORD)
            .name(Component.text("Throwing Knife", NamedTextColor.WHITE))
            .hideAll()
            .build();
    }

    @Override
    public List<Component> description() {
        return List.of(
            Component.text("Hurl a knife at your target.", NamedTextColor.GRAY),
            Component.text("Can also be used as a weak melee weapon.", NamedTextColor.DARK_GRAY)
        );
    }

    @Override
    public ItemStack buildWorldItem() {
        ItemStack item = ItemStackBuilder.of(Material.DIAMOND_SWORD)
            .name(Component.text("Throwing Knife", NamedTextColor.WHITE))
            .lore(description())
            .hideAll()
            .build();
        return AbilityItemBuilder.tag(item, id());
    }

    @Override
    public boolean consumesOnUse() {
        return true;
    }

    /** Stack count and cooldown both apply — non-exclusive. */
    @Override
    public Set<AbilityUseType> useTypes() {
        return Set.of(AbilityUseType.STACK, AbilityUseType.COOLDOWN);
    }

    @Override
    public int maxUses() {
        return 3;
    }

    @Override
    public int cooldownTicks() {
        return 10;
    }

    @Override
    public boolean isWeapon() {
        return true;
    }

    @Override
    public void execute(Combatant combatant) {
        ItemStack projectile = ItemStackBuilder.of(Material.DIAMOND_SWORD)
            .name(Component.text("Throwing Knife"))
            .hideAll()
            .build();
        AbilityItemBuilder.tag(projectile, id());
        ThrowAction.throwDirect(combatant, projectile, PROJECTILE_SCALE, PROJECTILE_VELOCITY);
    }

    @Override
    public int calculateCooldown(Combatant combatant) {
        return 200;
    }

    @Override
    public boolean canPerform(Combatant combatant) {
        return combatant.canPerformAction();
    }
}
