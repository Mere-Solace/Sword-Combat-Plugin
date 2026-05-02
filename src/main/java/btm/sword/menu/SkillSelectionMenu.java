package btm.sword.menu;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import btm.sword.action.skill.Skill;
import btm.sword.action.skill.SkillId;
import btm.sword.action.skill.SkillRegistry;
import btm.sword.action.skill.SkillType;
import btm.sword.action.skill.container.PlayerSkillContainer;
import btm.sword.action.skill.container.SkillAvailability;
import btm.sword.action.skill.container.SkillSlot;
import btm.sword.entity.player.SwordPlayer;
import btm.sword.item.core.ItemStackBuilder;
import btm.sword.item.special.AbilitySlotManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import xyz.xenondevs.invui.gui.PagedGui;
import xyz.xenondevs.invui.gui.structure.Markers;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.Window;

/** Menu allowing the player to browse and equip skills for a specific skill slot. */
public class SkillSelectionMenu extends Menu {
    private final PlayerSkillContainer skillContainer;
    private final SkillSlot slot;
    private final SkillType type;

    /** Constructs the skill selection menu for the given player and target skill slot. */
    public SkillSelectionMenu(SwordPlayer swordPlayer, SkillSlot slot) {
        super(swordPlayer);
        this.skillContainer = swordPlayer.getCombatProfile().getPlayerSkillContainer();
        this.slot = slot;
        this.type = slot.type();
    }

    private void refreshAbilitySlot(SkillSlot skillSlot) {
        if (skillSlot == SkillSlot.ACTIVE_1) {
            swordPlayer.getAbilitySlotManager().refresh(AbilitySlotManager.SLOT_1);
        } else if (skillSlot == SkillSlot.ACTIVE_2) {
            swordPlayer.getAbilitySlotManager().refresh(AbilitySlotManager.SLOT_2);
        }
    }

    @Override
    public void open() {
        AtomicReference<SkillId> curSelected = new AtomicReference<>(skillContainer.getEquipped(slot));

        List<Item> skillSelectItems = new ArrayList<>(skillContainer.freeSkillIds(type)
            .stream()
            .map(id -> (Item) new SimpleItem(
                SkillRegistry.get(id).icon(),
                click -> {
                    skillContainer.equip(slot, id);
                    refreshAbilitySlot(slot);
                    this.open();
                })).collect(Collectors.toList())); // this line eliminates the type difference error.

        // Discovered but locked skills (depleted / relinquished) — shown but not selectable
        for (SkillId id : skillContainer.lockedSkillIds(type)) {
            SkillAvailability availability = skillContainer.getAvailability(id);
            Component lockLabel = availability == SkillAvailability.RELINQUISHED
                ? Component.text(" [Relinquished]", NamedTextColor.DARK_RED)
                : Component.text(" [Depleted]", NamedTextColor.DARK_GRAY);
            ItemStack lockedIcon = new ItemStackBuilder(SkillRegistry.get(id).icon().getType())
                .name(SkillRegistry.get(id).icon().getItemMeta().displayName().append(lockLabel))
                .hideAll()
                .build();
            skillSelectItems.add(new SimpleItem(lockedIcon, click -> {}));
        }

        Skill cur = SkillRegistry.get(curSelected.get());

        SimpleItem unequipCurrent = cur == null ?
            new SimpleItem(new ItemStackBuilder(Material.WHITE_STAINED_GLASS_PANE)
                .name(Component.text("Select a Skill to Equip"))
                .build(),
                click -> {})
            : new SimpleItem(cur.icon(),
            click -> {
                skillContainer.unequip(slot);
                refreshAbilitySlot(slot);
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
            .setTitle(slot.title())
            .setGui(gui)
            .build();

        window.open();
    }
}
