package btm.sword.system.inventory.menu;


import java.util.Map;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import btm.sword.system.action.skill.container.SkillSlot;
import btm.sword.system.entity.impl.SwordPlayer;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.Window;

public class CharacterMenu extends Menu {

    public CharacterMenu(SwordPlayer player) {
        super(player);
    }

    public static final Map<Integer, SkillSlot> menuSlots = Map.of(
        1, SkillSlot.UMBRAL_1,
        2, SkillSlot.UMBRAL_2,
        3, SkillSlot.UMBRAL_3,
        4, SkillSlot.ACTIVE_1,
        5, SkillSlot.ACTIVE_2,
        6, SkillSlot.PASSIVE_1,
        7, SkillSlot.PASSIVE_2,
        8, SkillSlot.PASSIVE_3,
        9, SkillSlot.PASSIVE_CORE
    );

    @Override
    public void open() {
        Player player = swordPlayer.player();

        Gui gui = Gui.normal()
            .setStructure(
                "# # # . S . # # #", // Stats and class info. Add a click that takes the player to change their class
                "# . . . . . . . #",
                "< . { 1 2 3 { . .", // 3 Umbral Skills
                ". . 4 - 9 - 5 . .", // 2 other active skills and 1 Core Passive
                "> . { 6 7 8 { . .", // 3 other passives
                "# . . . . . . . #",
                "# # # . W . # # #") // weapon combat proficiencies and info (equip normal weapon-specific passives and skills
            .addIngredient('#', new SimpleItem(new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE)))
            .addIngredient('S', swordPlayer.getPlayerHead())
            .addIngredient('<', generatePreviousButtonOrDefault())
            .addIngredient('>', generateForwardPreviousButtonOrDefault())
            .build();

        Window window = Window.single()
            .setViewer(player)
            .setTitle("MainMenu")
            .setGui(gui)
            .build();

        window.open();
    }
}
