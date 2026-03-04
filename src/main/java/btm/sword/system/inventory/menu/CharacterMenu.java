package btm.sword.system.inventory.menu;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import btm.sword.config.Config;
import btm.sword.system.action.skill.Skill;
import btm.sword.system.action.skill.SkillId;
import btm.sword.system.action.skill.SkillIds;
import btm.sword.system.action.skill.SkillRegistry;
import btm.sword.system.action.skill.container.SkillSlot;
import btm.sword.system.entity.impl.SwordPlayer;
import btm.sword.system.item.ItemStackBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.Click;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.Window;

public class CharacterMenu extends Menu {

    public CharacterMenu(SwordPlayer player) {
        super(player);
    }

    public static final Map<Character, SkillSlot> menuSlots = Map.of(
        '1', SkillSlot.UMBRAL_1,
        '2', SkillSlot.UMBRAL_2,
        '3', SkillSlot.UMBRAL_3,
        '4', SkillSlot.ACTIVE_1,
        '5', SkillSlot.ACTIVE_2,
        '6', SkillSlot.PASSIVE_1,
        '7', SkillSlot.PASSIVE_2,
        '8', SkillSlot.PASSIVE_3,
        '9', SkillSlot.PASSIVE_CORE
    );

    public static final ItemStack lockedSlot = ItemStackBuilder
        .of(Material.BARRIER)
        .name(Component.text("Locked Slot", TextColor.color(255, 0, 0), TextDecoration.BOLD))
        .build();

    private SimpleItem generateMenuSlotButton(SkillSlot slot) {
        String inputSeq = switch (slot) {
            case UMBRAL_1 -> "SWAP - LEFT - LEFT";
            case UMBRAL_2 -> "SWAP - LEFT - RIGHT";
            case UMBRAL_3 -> "SWAP - LEFT - SWAP";

            case ACTIVE_1, ACTIVE_2 -> "Active Ability";

            case PASSIVE_1, PASSIVE_2, PASSIVE_3 -> "Passive Ability";
            case PASSIVE_CORE -> "Core Passive";
        };

        SkillId skillId = swordPlayer.getCombatProfile().getPlayerSkillContainer().getEquipped(slot);

        if (skillId == null) {
            return generateEmptySlotButton(slot, inputSeq);
        }

        if (skillId.equals(SkillIds.LOCKED)) {
            return generateLockedSlotButton(slot);
        }

        Skill skillPointer = SkillRegistry.get(skillId);

        if (skillPointer == null) {
            return generateEmptySlotButton(slot, inputSeq);
        }

        List<Component> lore = new ArrayList<>(skillPointer.description());
        lore.addFirst(skillPointer.icon().displayName());

        ItemStack toDisplay =
            ItemStackBuilder.of(skillPointer.icon().getType())
                .name(Component.text(inputSeq, Config.SwordColor.TEXT_COOL, TextDecoration.ITALIC))
                .lore(lore)
                .hideAll()
                .build();

        return new SimpleItem(toDisplay,
            selectAction(slot)
        );
    }

    private SimpleItem generateEmptySlotButton(SkillSlot slot, String inputSeq) {
        return new SimpleItem(ItemStackBuilder.of(Material.WHITE_STAINED_GLASS_PANE)
            .name(Component.text(inputSeq, Config.SwordColor.TEXT_COOL, TextDecoration.ITALIC))
            .lore(List.of(Component.text("Empty Slot")))
            .build(),
            selectAction(slot)
        );
    }

    private SimpleItem generateLockedSlotButton(SkillSlot slot) {
        return new SimpleItem(lockedSlot, unlockSlot(slot));
    }

    private Consumer<Click> selectAction(SkillSlot slot) {
        return click -> new SkillSelectionMenu(swordPlayer, slot).open();
    }

    private Consumer<Click> unlockSlot(SkillSlot slot) {
        // TODO: confirmation screen first?
        // TODO: should require the consumption of something (or achievement of something)
        return click -> {
            swordPlayer.getCombatProfile().getPlayerSkillContainer().unlock(slot);
            this.open();
        };
    }

    @Override
    public void open() {
        Player player = swordPlayer.player();

        Gui.Builder.Normal normal = Gui.normal()
            .setStructure(
                "# # # . S . # # #", // Stats and class info. Add a click that takes the player to change their class
                "# . { 1 2 3 { . #", // 3 Umbral Skills
                "# . 4 - 9 - 5 . .", // 2 other active skills and 1 Core Passive
                "# . { 6 7 8 { . .", // 3 other passives
                "# . . . . . . . #",
                "# # # < W > # # #") // weapon combat proficiencies and info (equip normal weapon-specific passives and skills
            .addIngredient('#', BORDER)
            .addIngredient('S', swordPlayer.getPlayerHead())
            .addIngredient('<', generatePreviousButtonOrDefault())
            .addIngredient('>', generateForwardPreviousButtonOrDefault());

        for (Map.Entry<Character, SkillSlot> entry : menuSlots.entrySet()) {
            normal.addIngredient(entry.getKey(), generateMenuSlotButton(entry.getValue()));
        }

        Gui gui = normal.build();

        Window window = Window.single()
            .setViewer(player)
            .setTitle("MainMenu")
            .setGui(gui)
            .build();

        window.open();
    }
}
