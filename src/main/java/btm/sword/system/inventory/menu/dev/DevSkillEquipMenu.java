package btm.sword.system.inventory.menu.dev;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;

import btm.sword.system.action.skill.Skill;
import btm.sword.system.action.skill.SkillRegistry;
import btm.sword.system.action.skill.container.PlayerSkillContainer;
import btm.sword.system.action.skill.container.SkillSlot;
import btm.sword.system.entity.impl.SwordPlayer;
import btm.sword.system.inventory.menu.Menu;
import btm.sword.system.item.ItemStackBuilder;
import btm.sword.system.item.special.AbilitySlotManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import xyz.xenondevs.invui.gui.PagedGui;
import xyz.xenondevs.invui.gui.structure.Markers;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.Window;

/**
 * Dev-only two-level skill equip menu.
 * <p>
 * Level 1 — lists every registered skill. Level 2 — lists the {@link SkillSlot}s
 * compatible with the chosen skill's type. Selecting a slot calls
 * {@link PlayerSkillContainer#discover} + {@link PlayerSkillContainer#equip}, bypassing
 * normal availability checks, and refreshes the hotbar for active slots.
 * </p>
 */
public class DevSkillEquipMenu extends Menu {

    /**
     * @param swordPlayer the player opening this menu
     */
    public DevSkillEquipMenu(SwordPlayer swordPlayer) {
        super(swordPlayer);
    }

    @Override
    public void open() {
        List<Item> skillItems = new ArrayList<>();
        for (Skill skill : SkillRegistry.SKILL_MAPPING.values()) {
            skillItems.add(new SimpleItem(skill.icon(), click -> openSlotPicker(skill)));
        }

        SimpleItem back = new SimpleItem(
            new ItemStackBuilder(Material.COPPER_TRAPDOOR)
                .name(Component.text("Back", NamedTextColor.GRAY))
                .build(),
            click -> new DevMenu(swordPlayer).open()
        );

        PagedGui<Item> gui = PagedGui.items()
            .setStructure(
                "P # # # # # # # #",
                "x x x x x x x x x",
                "x x x x x x x x x",
                "# # # < # > # # #")
            .addIngredient('#', BORDER)
            .addIngredient('x', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
            .addIngredient('P', back)
            .setContent(skillItems)
            .build();

        Window.single()
            .setViewer(swordPlayer.player())
            .setTitle("Dev — Equip Skill")
            .setGui(gui)
            .build()
            .open();
    }

    /**
     * Opens the slot-picker level for the given skill, showing only slots whose type matches.
     *
     * @param skill the skill to equip
     */
    private void openSlotPicker(Skill skill) {
        List<Item> slotItems = new ArrayList<>();
        PlayerSkillContainer container = swordPlayer.getCombatProfile().getPlayerSkillContainer();

        for (SkillSlot slot : SkillSlot.values()) {
            if (slot.type() != skill.type()) continue;
            slotItems.add(new SimpleItem(
                new ItemStackBuilder(Material.LIME_STAINED_GLASS_PANE)
                    .name(Component.text(slot.title(), NamedTextColor.GREEN, TextDecoration.BOLD))
                    .build(),
                click -> {
                    container.discover(skill.id());
                    container.equip(slot, skill.id());
                    if (slot == SkillSlot.ACTIVE_1) {
                        swordPlayer.getAbilitySlotManager().refresh(AbilitySlotManager.SLOT_1);
                    } else if (slot == SkillSlot.ACTIVE_2) {
                        swordPlayer.getAbilitySlotManager().refresh(AbilitySlotManager.SLOT_2);
                    }
                    swordPlayer.message(Component.text("Equipped ", NamedTextColor.GREEN)
                        .append(Component.text(skill.id().asString(), NamedTextColor.WHITE))
                        .append(Component.text(" → " + slot.title(), NamedTextColor.GRAY)));
                    open();
                }
            ));
        }

        SimpleItem back = new SimpleItem(
            new ItemStackBuilder(Material.COPPER_TRAPDOOR)
                .name(Component.text("Back", NamedTextColor.GRAY))
                .build(),
            click -> open()
        );

        PagedGui<Item> gui = PagedGui.items()
            .setStructure(
                "P # # # # # # # #",
                "x x x x x x x x x",
                "# # # < # > # # #")
            .addIngredient('#', BORDER)
            .addIngredient('x', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
            .addIngredient('P', back)
            .setContent(slotItems)
            .build();

        Window.single()
            .setViewer(swordPlayer.player())
            .setTitle("Pick Slot — " + skill.id().asString())
            .setGui(gui)
            .build()
            .open();
    }
}
