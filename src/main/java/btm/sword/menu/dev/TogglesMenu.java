package btm.sword.menu.dev;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import btm.sword.config.Config;
import btm.sword.config.ConfigManager;
import btm.sword.config.section.DebugConfig;
import btm.sword.config.section.WorldConfig;
import btm.sword.entity.player.SwordPlayer;
import btm.sword.item.core.ItemStackBuilder;
import btm.sword.menu.Menu;
import btm.sword.playerdata.PlayerData;
import btm.sword.playerdata.PlayerDataManager;
import btm.sword.util.misc.Debug;
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
            () -> DebugConfig.LOGGING_VERBOSE_DEBUG,
            () -> DebugConfig.LOGGING_VERBOSE_DEBUG = !DebugConfig.LOGGING_VERBOSE_DEBUG
        );

        SimpleItem verboseCombat = toggle(
            "Combat",
            () -> DebugConfig.LOGGING_VERBOSE_COMBAT,
            () -> DebugConfig.LOGGING_VERBOSE_COMBAT = !DebugConfig.LOGGING_VERBOSE_COMBAT
        );

        SimpleItem verboseMovement = toggle(
            "Movement",
            () -> DebugConfig.LOGGING_VERBOSE_MOVEMENT,
            () -> DebugConfig.LOGGING_VERBOSE_MOVEMENT = !DebugConfig.LOGGING_VERBOSE_MOVEMENT
        );

        SimpleItem verboseInventory = toggle(
            "Inventory",
            () -> DebugConfig.LOGGING_VERBOSE_INVENTORY,
            () -> DebugConfig.LOGGING_VERBOSE_INVENTORY = !DebugConfig.LOGGING_VERBOSE_INVENTORY
        );

        SimpleItem verboseSystem = toggle(
            "System (FSM / tasks)",
            () -> DebugConfig.LOGGING_VERBOSE_SYSTEM,
            () -> DebugConfig.LOGGING_VERBOSE_SYSTEM = !DebugConfig.LOGGING_VERBOSE_SYSTEM
        );

        SimpleItem verboseUmbral = toggle(
            "Umbral",
            () -> DebugConfig.LOGGING_VERBOSE_UMBRAL,
            () -> DebugConfig.LOGGING_VERBOSE_UMBRAL = !DebugConfig.LOGGING_VERBOSE_UMBRAL
        );

        SimpleItem verboseHostile = toggle(
            "Hostile",
            () -> DebugConfig.LOGGING_VERBOSE_HOSTILE,
            () -> DebugConfig.LOGGING_VERBOSE_HOSTILE = !DebugConfig.LOGGING_VERBOSE_HOSTILE
        );

        SimpleItem verboseListener = toggle(
            "Listener",
            () -> DebugConfig.LOGGING_VERBOSE_LISTENER,
            () -> DebugConfig.LOGGING_VERBOSE_LISTENER = !DebugConfig.LOGGING_VERBOSE_LISTENER
        );

        SimpleItem specialItemChecks = toggle(
            "Special item checks",
            () -> Debug.SPECIAL_ITEM_CHECKS_ENABLED,
            () -> Debug.SPECIAL_ITEM_CHECKS_ENABLED = !Debug.SPECIAL_ITEM_CHECKS_ENABLED
        );

        SimpleItem blockPlacing = toggle(
            "Block placing",
            () -> WorldConfig.BLOCK_INTERACTION_ALLOW_BLOCK_PLACING,
            () -> WorldConfig.BLOCK_INTERACTION_ALLOW_BLOCK_PLACING = !WorldConfig.BLOCK_INTERACTION_ALLOW_BLOCK_PLACING
        );

        SimpleItem verboseAnimation = toggle(
            "Animation (DEU hook)",
            () -> DebugConfig.LOGGING_VERBOSE_ANIMATION,
            () -> DebugConfig.LOGGING_VERBOSE_ANIMATION = !DebugConfig.LOGGING_VERBOSE_ANIMATION
        );

        SimpleItem verboseInput = toggle(
            "Input (combo / trie)",
            () -> DebugConfig.LOGGING_VERBOSE_INPUT,
            () -> DebugConfig.LOGGING_VERBOSE_INPUT = !DebugConfig.LOGGING_VERBOSE_INPUT
        );

        SimpleItem verboseSkill = toggle(
            "Skill (slot resolution)",
            () -> DebugConfig.LOGGING_VERBOSE_SKILL,
            () -> DebugConfig.LOGGING_VERBOSE_SKILL = !DebugConfig.LOGGING_VERBOSE_SKILL
        );

        SimpleItem verboseAbility = toggle(
            "Ability (lifecycle)",
            () -> DebugConfig.LOGGING_VERBOSE_ABILITY,
            () -> DebugConfig.LOGGING_VERBOSE_ABILITY = !DebugConfig.LOGGING_VERBOSE_ABILITY
        );

        SimpleItem verboseGrab = toggle(
            "Grab (action lifecycle)",
            () -> DebugConfig.LOGGING_VERBOSE_GRAB,
            () -> DebugConfig.LOGGING_VERBOSE_GRAB = !DebugConfig.LOGGING_VERBOSE_GRAB
        );

        SimpleItem verboseAttack = toggle(
            "Attack (sweeps / hitbox)",
            () -> DebugConfig.LOGGING_VERBOSE_ATTACK,
            () -> DebugConfig.LOGGING_VERBOSE_ATTACK = !DebugConfig.LOGGING_VERBOSE_ATTACK
        );

        SimpleItem verboseThrowing = toggle(
            "Throwing (item lifecycle)",
            () -> DebugConfig.LOGGING_VERBOSE_THROWING,
            () -> DebugConfig.LOGGING_VERBOSE_THROWING = !DebugConfig.LOGGING_VERBOSE_THROWING
        );

        SimpleItem verboseAttackVolume = toggle(
            "Attack Volume (sim / hits)",
            () -> DebugConfig.LOGGING_VERBOSE_ATTACK_VOLUME,
            () -> DebugConfig.LOGGING_VERBOSE_ATTACK_VOLUME = !DebugConfig.LOGGING_VERBOSE_ATTACK_VOLUME
        );

        SimpleItem verboseParticleDisplay = toggle(
            "Particle Display (toWrapper)",
            () -> DebugConfig.LOGGING_VERBOSE_PARTICLE_DISPLAY,
            () -> DebugConfig.LOGGING_VERBOSE_PARTICLE_DISPLAY = !DebugConfig.LOGGING_VERBOSE_PARTICLE_DISPLAY
        );

        SimpleItem showHitboxes = toggle(
            "Hitbox Outlines (live attacks)",
            () -> DebugConfig.VISUALIZATION_SHOW_HITBOXES,
            () -> DebugConfig.VISUALIZATION_SHOW_HITBOXES = !DebugConfig.VISUALIZATION_SHOW_HITBOXES
        );

        @SuppressWarnings("unchecked")
        Config.ConfigEntry<Boolean> skipLoadEntry = (Config.ConfigEntry<Boolean>) Config.entries().stream()
            .filter(e -> e.path().equals("debug.skip_data_load"))
            .findFirst()
            .orElseThrow();

        SimpleItem skipLoad = toggle(
            "Skip Load (persisted)",
            () -> DebugConfig.SKIP_DATA_LOAD,
            () -> {
                ConfigManager.getInstance().setValue(skipLoadEntry, !DebugConfig.SKIP_DATA_LOAD);
                ConfigManager.getInstance().saveConfig();
            }
        );

        @SuppressWarnings("unchecked")
        Config.ConfigEntry<Boolean> skipSaveEntry = (Config.ConfigEntry<Boolean>) Config.entries().stream()
            .filter(e -> e.path().equals("debug.skip_data_save"))
            .findFirst()
            .orElseThrow();

        SimpleItem skipSave = toggle(
            "Skip Save (persisted)",
            () -> DebugConfig.SKIP_DATA_SAVE,
            () -> {
                ConfigManager.getInstance().setValue(skipSaveEntry, !DebugConfig.SKIP_DATA_SAVE);
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
                ". A B C D E P Q .",
                ". F G H I J K R .",
                ". S T U L M N O .",
                ". V W X . . . . .",
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
            .addIngredient('W', verboseParticleDisplay)
            .addIngredient('X', showHitboxes)
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
