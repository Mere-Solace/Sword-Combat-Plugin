package btm.sword.system.inventory.menu.dev;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import btm.sword.config.Config;
import btm.sword.config.ConfigManager;
import btm.sword.system.entity.impl.SwordPlayer;
import btm.sword.system.inventory.menu.Menu;
import btm.sword.system.item.ItemStackBuilder;
import btm.sword.system.playerdata.PlayerData;
import btm.sword.system.playerdata.PlayerDataManager;
import btm.sword.utility.Debug;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.Window;

/**
 * Submenu of {@link DevMenu} listing all boolean debug and world toggles.
 * <p>
 * Most toggles take effect immediately without a server restart and are not persisted across
 * restarts. Exceptions: "Skip Load" and "Skip Save" are written to config.yaml on toggle and
 * survive server restarts.
 * </p>
 */
public class TogglesMenu extends Menu {

    /**
     * Creates a new TogglesMenu for the given player.
     *
     * @param player the player opening this menu
     */
    public TogglesMenu(SwordPlayer player) {
        super(player);
    }

    @Override
    public void open() {
        Player player = swordPlayer.player();

        SimpleItem verboseDebug = toggle(
            "Debug (general)",
            () -> Config.Debug.LOGGING_VERBOSE_DEBUG,
            () -> Config.Debug.LOGGING_VERBOSE_DEBUG = !Config.Debug.LOGGING_VERBOSE_DEBUG
        );

        SimpleItem verboseCombat = toggle(
            "Combat",
            () -> Config.Debug.LOGGING_VERBOSE_COMBAT,
            () -> Config.Debug.LOGGING_VERBOSE_COMBAT = !Config.Debug.LOGGING_VERBOSE_COMBAT
        );

        SimpleItem verboseMovement = toggle(
            "Movement",
            () -> Config.Debug.LOGGING_VERBOSE_MOVEMENT,
            () -> Config.Debug.LOGGING_VERBOSE_MOVEMENT = !Config.Debug.LOGGING_VERBOSE_MOVEMENT
        );

        SimpleItem verboseInventory = toggle(
            "Inventory",
            () -> Config.Debug.LOGGING_VERBOSE_INVENTORY,
            () -> Config.Debug.LOGGING_VERBOSE_INVENTORY = !Config.Debug.LOGGING_VERBOSE_INVENTORY
        );

        SimpleItem verboseSystem = toggle(
            "System (FSM / tasks)",
            () -> Config.Debug.LOGGING_VERBOSE_SYSTEM,
            () -> Config.Debug.LOGGING_VERBOSE_SYSTEM = !Config.Debug.LOGGING_VERBOSE_SYSTEM
        );

        SimpleItem verboseUmbral = toggle(
            "Umbral",
            () -> Config.Debug.LOGGING_VERBOSE_UMBRAL,
            () -> Config.Debug.LOGGING_VERBOSE_UMBRAL = !Config.Debug.LOGGING_VERBOSE_UMBRAL
        );

        SimpleItem verboseHostile = toggle(
            "Hostile",
            () -> Config.Debug.LOGGING_VERBOSE_HOSTILE,
            () -> Config.Debug.LOGGING_VERBOSE_HOSTILE = !Config.Debug.LOGGING_VERBOSE_HOSTILE
        );

        SimpleItem verboseListener = toggle(
            "Listener",
            () -> Config.Debug.LOGGING_VERBOSE_LISTENER,
            () -> Config.Debug.LOGGING_VERBOSE_LISTENER = !Config.Debug.LOGGING_VERBOSE_LISTENER
        );

        SimpleItem specialItemChecks = toggle(
            "Special item checks",
            () -> Debug.SPECIAL_ITEM_CHECKS_ENABLED,
            () -> Debug.SPECIAL_ITEM_CHECKS_ENABLED = !Debug.SPECIAL_ITEM_CHECKS_ENABLED
        );

        SimpleItem blockPlacing = toggle(
            "Block placing",
            () -> Config.World.BLOCK_INTERACTION_ALLOW_BLOCK_PLACING,
            () -> Config.World.BLOCK_INTERACTION_ALLOW_BLOCK_PLACING = !Config.World.BLOCK_INTERACTION_ALLOW_BLOCK_PLACING
        );

        SimpleItem verboseAnimation = toggle(
            "Animation (DEU hook)",
            () -> Config.Debug.LOGGING_VERBOSE_ANIMATION,
            () -> Config.Debug.LOGGING_VERBOSE_ANIMATION = !Config.Debug.LOGGING_VERBOSE_ANIMATION
        );

        SimpleItem verboseInput = toggle(
            "Input (combo / trie)",
            () -> Config.Debug.LOGGING_VERBOSE_INPUT,
            () -> Config.Debug.LOGGING_VERBOSE_INPUT = !Config.Debug.LOGGING_VERBOSE_INPUT
        );

        SimpleItem verboseSkill = toggle(
            "Skill (slot resolution)",
            () -> Config.Debug.LOGGING_VERBOSE_SKILL,
            () -> Config.Debug.LOGGING_VERBOSE_SKILL = !Config.Debug.LOGGING_VERBOSE_SKILL
        );

        SimpleItem verboseAbility = toggle(
            "Ability (lifecycle)",
            () -> Config.Debug.LOGGING_VERBOSE_ABILITY,
            () -> Config.Debug.LOGGING_VERBOSE_ABILITY = !Config.Debug.LOGGING_VERBOSE_ABILITY
        );

        SimpleItem verboseGrab = toggle(
            "Grab (action lifecycle)",
            () -> Config.Debug.LOGGING_VERBOSE_GRAB,
            () -> Config.Debug.LOGGING_VERBOSE_GRAB = !Config.Debug.LOGGING_VERBOSE_GRAB
        );

        SimpleItem verboseAttack = toggle(
            "Attack (sweeps / hitbox)",
            () -> Config.Debug.LOGGING_VERBOSE_ATTACK,
            () -> Config.Debug.LOGGING_VERBOSE_ATTACK = !Config.Debug.LOGGING_VERBOSE_ATTACK
        );

        SimpleItem verboseThrowing = toggle(
            "Throwing (item lifecycle)",
            () -> Config.Debug.LOGGING_VERBOSE_THROWING,
            () -> Config.Debug.LOGGING_VERBOSE_THROWING = !Config.Debug.LOGGING_VERBOSE_THROWING
        );

        SimpleItem verboseAttackVolume = toggle(
            "Attack Volume (sim / hits)",
            () -> Config.Debug.LOGGING_VERBOSE_ATTACK_VOLUME,
            () -> Config.Debug.LOGGING_VERBOSE_ATTACK_VOLUME = !Config.Debug.LOGGING_VERBOSE_ATTACK_VOLUME
        );

        @SuppressWarnings("unchecked")
        Config.ConfigEntry<Boolean> skipLoadEntry = (Config.ConfigEntry<Boolean>) Config.ENTRIES.stream()
            .filter(e -> e.path().equals("debug.skip_data_load"))
            .findFirst()
            .orElseThrow();

        SimpleItem skipLoad = toggle(
            "Skip Load (persisted)",
            () -> Config.Debug.SKIP_DATA_LOAD,
            () -> {
                ConfigManager.getInstance().setValue(skipLoadEntry, !Config.Debug.SKIP_DATA_LOAD);
                ConfigManager.getInstance().saveConfig();
            }
        );

        @SuppressWarnings("unchecked")
        Config.ConfigEntry<Boolean> skipSaveEntry = (Config.ConfigEntry<Boolean>) Config.ENTRIES.stream()
            .filter(e -> e.path().equals("debug.skip_data_save"))
            .findFirst()
            .orElseThrow();

        SimpleItem skipSave = toggle(
            "Skip Save (persisted)",
            () -> Config.Debug.SKIP_DATA_SAVE,
            () -> {
                ConfigManager.getInstance().setValue(skipSaveEntry, !Config.Debug.SKIP_DATA_SAVE);
                ConfigManager.getInstance().saveConfig();
            }
        );

        SimpleItem freshProfile = new SimpleItem(
            new ItemStackBuilder(Material.RECOVERY_COMPASS)
                .name(Component.text("Fresh Profile", NamedTextColor.AQUA, TextDecoration.BOLD))
                .lore(List.of(Component.text("Replace your session data with a blank profile", NamedTextColor.DARK_GRAY)))
                .build(),
            click -> {
                PlayerDataManager.resetToFresh(swordPlayer.getUniqueId());
                swordPlayer.player().sendMessage(Component.text("Session data reset to fresh profile.", NamedTextColor.GREEN));
                this.open();
            }
        );

        SimpleItem resetJoin = toggle(
            "Join Sequence Done",
            () -> {
                PlayerData pd = PlayerDataManager.getPlayerData(swordPlayer.getUniqueId());
                return pd != null && pd.isJoinSequenceCompleted();
            },
            () -> {
                PlayerData pd = PlayerDataManager.getPlayerData(swordPlayer.getUniqueId());
                if (pd != null) pd.setJoinSequenceCompleted(!pd.isJoinSequenceCompleted());
            }
        );

        Gui gui = Gui.normal()
            .setStructure(
                "# # # # # # # # #",
                "# A B C D E P Q V",
                "# F G H I J K R .",
                "# S T U L M N O .",
                "< # # # # # # # #")
            .addIngredient('#', BORDER)
            .addIngredient('A', verboseDebug)
            .addIngredient('B', verboseCombat)
            .addIngredient('C', verboseMovement)
            .addIngredient('D', verboseInventory)
            .addIngredient('E', verboseSystem)
            .addIngredient('P', verboseInput)
            .addIngredient('Q', verboseSkill)
            .addIngredient('F', verboseUmbral)
            .addIngredient('G', verboseHostile)
            .addIngredient('H', verboseListener)
            .addIngredient('I', verboseAnimation)
            .addIngredient('J', verboseAbility)
            .addIngredient('K', verboseGrab)
            .addIngredient('R', verboseAttack)
            .addIngredient('S', verboseThrowing)
            .addIngredient('T', specialItemChecks)
            .addIngredient('U', blockPlacing)
            .addIngredient('L', skipLoad)
            .addIngredient('M', skipSave)
            .addIngredient('N', freshProfile)
            .addIngredient('O', resetJoin)
            .addIngredient('V', verboseAttackVolume)
            .addIngredient('<', generatePreviousButtonOrDefault())
            .build();

        Window window = Window.single()
            .setViewer(player)
            .setTitle("Dev Toggles")
            .setGui(gui)
            .build();

        window.open();
    }
}
