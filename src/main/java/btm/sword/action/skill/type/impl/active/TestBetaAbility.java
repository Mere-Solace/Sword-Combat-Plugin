package btm.sword.action.skill.type.impl.active;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import btm.sword.action.skill.SkillId;
import btm.sword.action.skill.SkillIds;
import btm.sword.action.skill.SkillType;
import btm.sword.action.skill.type.ActivatableAbility;
import btm.sword.entity.base.Combatant;
import btm.sword.entity.player.SwordPlayer;
import btm.sword.item.core.AbilityItemBuilder;
import btm.sword.item.core.ItemStackBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * No-op test ability Beta. Used to verify the ability slot, equip/unequip, and
 * availability systems end-to-end.
 *
 * <p>TODO: remove once real found abilities replace test stubs.</p>
 */
public class TestBetaAbility extends ActivatableAbility {

    @Override
    public Component name() {
        return Component.text("Test Beta");
    }

    @Override
    public SkillId id() {
        return SkillIds.TEST_BETA;
    }

    @Override
    public SkillType type() {
        return SkillType.ACTIVE;
    }

    @Override
    public ItemStack icon() {
        return ItemStackBuilder.of(Material.YELLOW_STAINED_GLASS_PANE)
            .name(Component.text("Test Beta", NamedTextColor.YELLOW))
            .hideAll()
            .build();
    }

    @Override
    public List<Component> description() {
        return List.of(Component.text("A no-op test ability.", NamedTextColor.GRAY));
    }

    @Override
    public ItemStack buildWorldItem() {
        ItemStack item = ItemStackBuilder.of(Material.YELLOW_STAINED_GLASS_PANE)
            .name(Component.text("Test Beta", NamedTextColor.YELLOW))
            .lore(description())
            .hideAll()
            .build();
        return AbilityItemBuilder.tag(item, id());
    }

    @Override
    public void execute(Combatant combatant) {
        if (combatant instanceof SwordPlayer sp) {
            sp.player().sendMessage(Component.text("[Test] Beta fired!", NamedTextColor.YELLOW));
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
