package btm.sword.system.entity.npc.menu;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.bukkit.Material;

import btm.sword.system.entity.impl.SwordPlayer;
import btm.sword.system.entity.npc.NpcEntity;
import btm.sword.system.entity.npc.dialogue.NpcDialogueChoice;
import btm.sword.system.entity.npc.dialogue.NpcDialogueController;
import btm.sword.system.entity.npc.dialogue.NpcDialogueNode;
import btm.sword.system.item.ItemStackBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.Window;

/**
 * InvUI surface that renders one {@link NpcDialogueNode} for a player.
 * <p>
 * Layout — single-row chest GUI:
 * <ul>
 *   <li>Slot 0: speaker portrait (PLAYER_HEAD-style item) with the NPC's display name
 *       and the node's spoken lines as lore.</li>
 *   <li>Slots 1..N: one button per choice (max 8 choices). Clicking dispatches the
 *       choice index back to the {@link NpcDialogueController}.</li>
 *   <li>Last slot: "End dialogue" button if the node has no choices, otherwise empty.</li>
 * </ul>
 * The menu is single-shot — the router constructs a fresh instance for every render.
 */
public final class NpcDialogueMenu extends NpcMenu {

    /** Maximum number of choices that fit in the menu. */
    public static final int MAX_CHOICES = 8;

    private final NpcDialogueNode node;
    private final NpcDialogueController controller;

    /**
     * Constructs a dialogue menu for a single render of the given node.
     *
     * @param player     the viewing player
     * @param npc        the NPC speaking
     * @param node       the node to render
     * @param controller the controller that owns dialogue state for this NPC
     */
    public NpcDialogueMenu(SwordPlayer player, NpcEntity npc,
                           NpcDialogueNode node, NpcDialogueController controller) {
        super(player, npc);
        this.node = Objects.requireNonNull(node, "node");
        this.controller = Objects.requireNonNull(controller, "controller");
    }

    @Override
    public void open() {
        SimpleItem speaker = buildSpeaker();
        List<SimpleItem> choiceItems = buildChoices();
        SimpleItem endButton = node.choices().isEmpty() ? buildEndButton() : null;

        Gui.Builder.Normal builder = Gui.normal().setStructure("S 1 2 3 4 5 6 7 8");
        builder.addIngredient('S', speaker);
        char[] slots = {'1', '2', '3', '4', '5', '6', '7', '8'};
        for (int i = 0; i < slots.length; i++) {
            if (i < choiceItems.size()) {
                builder.addIngredient(slots[i], choiceItems.get(i));
            } else if (i == choiceItems.size() && endButton != null) {
                builder.addIngredient(slots[i], endButton);
            } else {
                builder.addIngredient(slots[i], BORDER);
            }
        }

        Window.single()
            .setViewer(swordPlayer.player())
            .setTitle(plainSpeakerTitle())
            .setGui(builder.build())
            .build()
            .open();
    }

    private SimpleItem buildSpeaker() {
        ItemStackBuilder builder = new ItemStackBuilder(Material.PAPER)
            .name(Component.text("» ", NamedTextColor.GOLD)
                .append(Component.text(npc.displayName(), NamedTextColor.YELLOW, TextDecoration.BOLD)))
            .lore(node.lines());
        return new SimpleItem(builder.build());
    }

    private List<SimpleItem> buildChoices() {
        List<SimpleItem> items = new ArrayList<>(node.choices().size());
        for (int i = 0; i < node.choices().size() && i < MAX_CHOICES; i++) {
            int index = i;
            NpcDialogueChoice choice = node.choices().get(i);
            SimpleItem item = new SimpleItem(
                new ItemStackBuilder(Material.WRITABLE_BOOK)
                    .name(Component.text("• ", NamedTextColor.GRAY)
                        .append(choice.label().colorIfAbsent(NamedTextColor.WHITE)))
                    .build(),
                click -> controller.select(swordPlayer, npc, index)
            );
            items.add(item);
        }
        return items;
    }

    private SimpleItem buildEndButton() {
        return new SimpleItem(
            new ItemStackBuilder(Material.BARRIER)
                .name(Component.text("End conversation", NamedTextColor.RED, TextDecoration.BOLD))
                .build(),
            click -> {
                controller.end(swordPlayer);
                click.getPlayer().closeInventory();
            }
        );
    }

    private String plainSpeakerTitle() {
        return npc.displayName();
    }
}
