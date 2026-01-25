package btm.sword.system.action.skill.type;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import btm.sword.system.action.skill.Skill;
import btm.sword.system.entity.impl.Combatant;
import btm.sword.system.item.ItemStackBuilder;
import net.kyori.adventure.text.Component;

public abstract class ActiveSkill implements Skill {
    @Override
    public ItemStack icon() {
        return ItemStackBuilder.of(Material.SPECTRAL_ARROW)
            .name(Component.text("Yet-to-be-implemented Active Skill"))
            .hideAll()
            .build();
    }

    public abstract void execute(Combatant combatant);
    public abstract int calculateCooldown(Combatant combatant);
    public abstract boolean canPerform(Combatant combatant);
}
