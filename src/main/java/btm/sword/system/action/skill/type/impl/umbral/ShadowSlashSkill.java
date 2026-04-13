package btm.sword.system.action.skill.type.impl.umbral;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import btm.sword.system.action.skill.SkillId;
import btm.sword.system.action.skill.SkillIds;
import btm.sword.system.action.skill.SkillType;
import btm.sword.system.action.skill.type.ActiveSkill;
import btm.sword.system.entity.impl.Combatant;
import btm.sword.system.item.ItemStackBuilder;
import net.kyori.adventure.text.Component;

/** Active skill that executes a shadow slash combo for UmbralBlade combatants. */
public class ShadowSlashSkill extends ActiveSkill {
    @Override
    public void execute(Combatant combatant) {
        combatant.message("Casting Shadow Slash!!!@!(*DH");
    }

    @Override
    public int calculateCooldown(Combatant combatant) {
        return 0;
    }

    @Override
    public boolean canPerform(Combatant combatant) {
        return true;
    }

    @Override
    public SkillId id() {
        return SkillIds.SHADOW_SLASH;
    }

    @Override
    public SkillType type() {
        return SkillType.UMBRAL;
    }

    @Override
    public ItemStack icon() {
        return ItemStackBuilder.of(Material.NETHERITE_SWORD)
            .name(Component.text("Shadow Slash"))
            .hideAll()
            .build();
    }

    @Override
    public Component name() {
        return null;
    }

    @Override
    public List<Component> description() {
        return List.of(

        );
    }
}
