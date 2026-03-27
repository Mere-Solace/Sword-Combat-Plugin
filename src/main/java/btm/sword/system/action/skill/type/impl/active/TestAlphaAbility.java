package btm.sword.system.action.skill.type.impl.active;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import btm.sword.system.action.skill.SkillId;
import btm.sword.system.action.skill.SkillIds;
import btm.sword.system.action.skill.SkillType;
import btm.sword.system.action.skill.type.ActivatableAbility;
import btm.sword.system.entity.impl.Combatant;
import btm.sword.system.entity.impl.SwordPlayer;
import btm.sword.system.item.AbilityItemBuilder;
import btm.sword.system.item.ItemStackBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * No-op test ability Alpha. Used to verify the ability slot, equip/unequip, and
 * availability systems end-to-end.
 *
 * <p>TODO: remove once real found abilities replace test stubs.</p>
 */
public class TestAlphaAbility extends ActivatableAbility {

    @Override
    public Component name() {
        return Component.text("Test Alpha");
    }

    @Override
    public SkillId id() {
        return SkillIds.TEST_ALPHA;
    }

    @Override
    public SkillType type() {
        return SkillType.ACTIVE;
    }

    @Override
    public ItemStack icon() {
        return ItemStackBuilder.of(Material.GREEN_STAINED_GLASS_PANE)
            .name(Component.text("Test Alpha", NamedTextColor.GREEN))
            .hideAll()
            .build();
    }

    @Override
    public List<Component> description() {
        return List.of(Component.text("A no-op test ability.", NamedTextColor.GRAY));
    }

    @Override
    public ItemStack buildWorldItem() {
        ItemStack item = ItemStackBuilder.of(Material.GREEN_STAINED_GLASS_PANE)
            .name(Component.text("Test Alpha", NamedTextColor.GREEN))
            .lore(description())
            .hideAll()
            .build();
        return AbilityItemBuilder.tag(item, id());
    }

    @Override
    public void execute(Combatant combatant) {
        if (combatant instanceof SwordPlayer sp) {
            sp.player().sendMessage(Component.text("[Test] Alpha fired!", NamedTextColor.GREEN));
        }
    }

    @Override
    public int calculateCooldown(Combatant combatant) {
        return 0;
    }

    @Override
    public boolean canPerform(Combatant combatant) {
        return true;
    }
}
