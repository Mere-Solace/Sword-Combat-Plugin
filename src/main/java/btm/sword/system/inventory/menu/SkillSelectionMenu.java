package btm.sword.system.inventory.menu;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.bukkit.Material;

import btm.sword.system.action.skill.Skill;
import btm.sword.system.action.skill.SkillId;
import btm.sword.system.action.skill.SkillRegistry;
import btm.sword.system.action.skill.SkillType;
import btm.sword.system.action.skill.container.PlayerSkillContainer;
import btm.sword.system.action.skill.container.SkillSlot;
import btm.sword.system.entity.impl.SwordPlayer;
import btm.sword.system.item.ItemStackBuilder;
import net.kyori.adventure.text.Component;
import xyz.xenondevs.invui.gui.PagedGui;
import xyz.xenondevs.invui.gui.structure.Markers;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.Window;

public class SkillSelectionMenu extends Menu {
    private final PlayerSkillContainer skillContainer;
    private final SkillSlot slot;
    private final SkillType type;

    public SkillSelectionMenu(SwordPlayer swordPlayer, SkillSlot slot) {
        super(swordPlayer);
        this.skillContainer = swordPlayer.getCombatProfile().getPlayerSkillContainer();
        this.slot = slot;
        this.type = slot.type();
    }

    @Override
    public void open() {
        AtomicReference<SkillId> curSelected = new AtomicReference<>(skillContainer.getEquipped(slot));

        List<Item> skillSelectItems = skillContainer.freeSkillIds(type)
            .stream()
            .map(id -> new SimpleItem(
                SkillRegistry.get(id).icon(),
                click -> {
                    skillContainer.equip(slot, id);
                    this.open();
                })).collect(Collectors.toList()); // this line eliminates the type difference error.

        Skill cur = SkillRegistry.get(curSelected.get());

        SimpleItem unequipCurrent = cur == null ?
            new SimpleItem(new ItemStackBuilder(Material.WHITE_STAINED_GLASS_PANE)
                .name(Component.text("Select a Skill to Equip"))
                .build(),
                click -> {})
            : new SimpleItem(cur.icon(),
            click -> {
                skillContainer.unequip(slot);
                this.open();
            });

        SimpleItem back = new SimpleItem(new ItemStackBuilder(Material.COPPER_TRAPDOOR)
            .name(Component.text("Back to Character Menu"))
            .build(),
            click -> new CharacterMenu(swordPlayer).open()
        );

        PagedGui<Item> gui = PagedGui.items()
            .setStructure(
                "P # # # # # # # #",
                "x x x x x x x x x",
                "x x x x x x x x x",
                "# # # < C > # # #")
            .addIngredient('#', BORDER)
            .addIngredient('x', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
            .addIngredient('C', unequipCurrent)
            .addIngredient('P', back)
            .setContent(skillSelectItems)
            .build();

        Window window = Window.single()
            .setViewer(swordPlayer.getPlayer())
            .setTitle("MainMenu")
            .setGui(gui)
            .build();

        window.open();
    }
}
