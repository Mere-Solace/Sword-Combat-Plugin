package btm.sword.system.action.skill.type;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import btm.sword.system.action.skill.Skill;
import btm.sword.system.item.ItemStackBuilder;
import net.kyori.adventure.text.Component;

public abstract class PassiveSkill implements Skill {
    @Override
    public ItemStack icon() {
        return ItemStackBuilder.of(Material.AMETHYST_SHARD)
            .name(Component.text("Yet-to-be-implemented Active Skill"))
            .hideAll()
            .build();
    }
}
