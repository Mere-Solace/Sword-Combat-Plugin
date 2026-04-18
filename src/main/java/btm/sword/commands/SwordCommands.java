package btm.sword.commands;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;

import btm.sword.Sword;
import btm.sword.config.Config;
import btm.sword.config.ConfigManager;
import btm.sword.system.attack.def.ParticleDisplayLibrary;
import btm.sword.system.control.TimeArbiter;
import btm.sword.system.entity.SwordEntityArbiter;
import btm.sword.system.entity.display.WeaponDisplayRegistry;
import btm.sword.system.entity.impl.SwordPlayer;
import btm.sword.system.entity.mob.MobTypeRegistry;
import btm.sword.system.item.material.MaterialType;
import btm.sword.system.playerdata.PlayerData;
import btm.sword.system.playerdata.PlayerDataManager;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Brigadier-based command registration for Sword: Combat Evolved.
 * <p>
 * Commands:
 * - /sword - Shows plugin info and usage
 * - /sword reload - Hot reloads configuration from disk
 * </p>
 */
public final class SwordCommands {
    private SwordCommands() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Registers all plugin commands using Paper's Brigadier command system.
     *
     * @param registrar The command registrar from the lifecycle event
     */
    public static void register(Commands registrar) {
        registrar.register(
            Commands.literal("sword")
                .executes(ctx -> {
                    CommandSender sender = ctx.getSource().getSender();
                    sender.sendMessage(Component.text("Sword: Combat Evolved", NamedTextColor.GOLD));
                    sender.sendMessage(Component.text(
                        "Usage: /sword reload | /sword dev <skipload|skipsave|freshprofile|resetjoin>",
                        NamedTextColor.GRAY
                    ));
                    return Command.SINGLE_SUCCESS;
                })
                .then(
                    Commands.literal("reload")
                        .requires(source -> source.getSender().hasPermission("sword.reload"))
                        .executes(ctx -> handleReload(ctx.getSource()))
                )
                .then(
                    Commands.argument("global_time_scale", DoubleArgumentType.doubleArg(0, 2))
                        .executes(ctx -> handleSetGlobalTimeScale(ctx.getSource(), DoubleArgumentType.getDouble(ctx, "global_time_scale")))
                )
                .then(
                    Commands.literal("dev")
                        .requires(source -> source.getSender().hasPermission("sword.dev"))
                        .then(
                            Commands.literal("skipload")
                                .executes(ctx -> handleDevSkipLoad(ctx.getSource()))
                        )
                        .then(
                            Commands.literal("skipsave")
                                .executes(ctx -> handleDevSkipSave(ctx.getSource()))
                        )
                        .then(
                            Commands.literal("freshprofile")
                                .executes(ctx -> handleDevFreshProfile(ctx.getSource(), null))
                                .then(
                                    Commands.argument("player", StringArgumentType.word())
                                        .executes(ctx -> handleDevFreshProfile(
                                            ctx.getSource(),
                                            StringArgumentType.getString(ctx, "player")
                                        ))
                                )
                        )
                        .then(
                            Commands.literal("resetjoin")
                                .executes(ctx -> handleDevResetJoin(ctx.getSource(), null))
                                .then(
                                    Commands.argument("player", StringArgumentType.word())
                                        .executes(ctx -> handleDevResetJoin(
                                            ctx.getSource(),
                                            StringArgumentType.getString(ctx, "player")
                                        ))
                                )
                        )
                )
                .then(
                    Commands.literal("give")
                        .then(
                            Commands.literal("material")
                                .then(
                                    Commands.argument("type", StringArgumentType.word())
                                        .executes(ctx -> handleGiveMaterial(
                                            ctx.getSource(),
                                            StringArgumentType.getString(ctx, "type"),
                                            1
                                        ))
                                        .then(
                                            Commands.argument("amount", IntegerArgumentType.integer(1, 64))
                                                .executes(ctx -> handleGiveMaterial(
                                                    ctx.getSource(),
                                                    StringArgumentType.getString(ctx, "type"),
                                                    IntegerArgumentType.getInteger(ctx, "amount")
                                                ))
                                        )
                                )
                        )
                        .then(
                            Commands.literal("credits")
                                .then(
                                    Commands.argument("amount", IntegerArgumentType.integer(1))
                                        .executes(ctx -> handleGiveCredits(
                                            ctx.getSource(),
                                            IntegerArgumentType.getInteger(ctx, "amount")
                                        ))
                                )
                        )
                )
                .build(),
            "Main command for Sword Combat Evolved",
            List.of("sce", "swordce")
        );
    }

    /**
     * Handles the /sword reload subcommand.
     * <p>
     * Reloads configuration from disk, allowing for hot config updates during testing.
     * </p>
     *
     * @param source The command source
     * @return Command result status
     */
    private static int handleReload(CommandSourceStack source) {
        CommandSender sender = source.getSender();

        // Permission check (also handled by requires(), but double-checking for safety)
        if (!sender.hasPermission("sword.reload")) {
            sender.sendMessage(Component.text("You don't have permission to reload the config.", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        sender.sendMessage(Component.text("Reloading Sword: Combat Evolved configuration...", NamedTextColor.YELLOW));

        try {
            MobTypeRegistry.reload();
            WeaponDisplayRegistry.reload();
            ParticleDisplayLibrary.load(new File(Sword.getInstance().getDataFolder(), "particles.yml"));
            boolean success = ConfigManager.getInstance().reload();

            if (success) {
                sender.sendMessage(
                    Component.text("✓ Configuration reloaded successfully!", NamedTextColor.GREEN)
                );
                sender.sendMessage(
                    Component.text("  All values have been updated from config.yaml", NamedTextColor.GRAY)
                );
            } else {
                sender.sendMessage(
                    Component.text("✗ Configuration reload failed!", NamedTextColor.RED)
                );
                sender.sendMessage(
                    Component.text(
                        "  Check console for error details. Using previous values.",
                        NamedTextColor.GRAY
                    )
                );
            }
        } catch (Exception e) {
            sender.sendMessage(
                Component.text("✗ Fatal error during reload: " + e.getMessage(), NamedTextColor.DARK_RED)
            );
            sender.sendMessage(Component.text("  Check console for full stack trace.", NamedTextColor.GRAY));
            return 2;
        }

        return Command.SINGLE_SUCCESS;
    }

    /**
     * Handles {@code /sword give material <type> [amount]}.
     * Gives the executing player physical tagged material items in their inventory.
     * Overflow drops at their feet.
     *
     * @param source the command source
     * @param typeId the {@link MaterialType} id string
     * @param amount number of items to give (1–64)
     * @return command result status
     */
    private static int handleGiveMaterial(CommandSourceStack source, String typeId, int amount) {
        CommandSender sender = source.getSender();
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be run by a player.", NamedTextColor.RED));
            return 2;
        }

        MaterialType type = MaterialType.fromId(typeId);
        if (type == null) {
            sender.sendMessage(Component.text("Unknown material type: \"" + typeId + "\"", NamedTextColor.RED));
            sender.sendMessage(Component.text("Valid types: " + validMaterialIds(), NamedTextColor.GRAY));
            return 2;
        }

        ItemStack stack = type.buildItemStack(amount);
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(stack);
        for (ItemStack drop : leftover.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), drop);
        }

        sender.sendMessage(
            Component.text("Gave ", NamedTextColor.GREEN)
                .append(Component.text(amount + "x ", NamedTextColor.WHITE))
                .append(type.displayName())
                .append(Component.text(".", NamedTextColor.GREEN))
        );
        return Command.SINGLE_SUCCESS;
    }

    /**
     * Handles {@code /sword give credits <amount>}.
     * Adds steel credits directly to the executing player's currency pouch.
     *
     * @param source the command source
     * @param amount number of credits to add
     * @return command result status
     */
    private static int handleGiveCredits(CommandSourceStack source, int amount) {
        CommandSender sender = source.getSender();
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be run by a player.", NamedTextColor.RED));
            return 2;
        }

        if (!(SwordEntityArbiter.getOrAdd(player) instanceof SwordPlayer sp)) {
            sender.sendMessage(Component.text("Could not resolve SwordPlayer.", NamedTextColor.RED));
            return 2;
        }

        sp.getPlayerStorage().addCredits(amount);
        sender.sendMessage(
            Component.text("Added ", NamedTextColor.GREEN)
                .append(Component.text(amount + " ✦ Steel Credits", NamedTextColor.WHITE))
                .append(Component.text(" to your currency pouch.", NamedTextColor.GREEN))
        );
        return Command.SINGLE_SUCCESS;
    }

    /** Returns a comma-separated list of all valid {@link MaterialType} id strings. */
    private static String validMaterialIds() {
        StringBuilder sb = new StringBuilder();
        MaterialType[] types = MaterialType.values();
        for (int i = 0; i < types.length; i++) {
            sb.append(types[i].id());
            if (i < types.length - 1) sb.append(", ");
        }
        return sb.toString();
    }

    /**
     * Handles {@code /sword [global_time_scale]}.
     *
     * @param source the command source
     * @param value  the new time scale (0.0–2.0)
     * @return command result status
     */
    public static int handleSetGlobalTimeScale(CommandSourceStack source, double value) {
        CommandSender sender = source.getSender();

        if (TimeArbiter.setGlobalTimeScale(value)) {
            sender.sendMessage("");
            return Command.SINGLE_SUCCESS;
        }
        return 2;
    }

    /**
     * Handles {@code /sword dev skipload}.
     * Toggles {@link Config.Debug#SKIP_DATA_LOAD} and immediately persists the change to config.yaml.
     * Console-compatible — no player entity required.
     *
     * @param source the command source
     * @return command result status
     */
    private static int handleDevSkipLoad(CommandSourceStack source) {
        CommandSender sender = source.getSender();
        @SuppressWarnings("unchecked")
        Config.ConfigEntry<Boolean> entry = (Config.ConfigEntry<Boolean>) Config.ENTRIES.stream()
            .filter(e -> e.path().equals("debug.skip_data_load"))
            .findFirst()
            .orElseThrow();
        boolean newValue = !Config.Debug.SKIP_DATA_LOAD;
        ConfigManager.getInstance().setValue(entry, newValue);
        ConfigManager.getInstance().saveConfig();
        sender.sendMessage(Component.text(
            "SKIP_DATA_LOAD -> " + newValue + " (saved to config.yaml).",
            newValue ? NamedTextColor.YELLOW : NamedTextColor.GREEN
        ));
        return Command.SINGLE_SUCCESS;
    }

    /**
     * Handles {@code /sword dev skipsave}.
     * Toggles {@link Config.Debug#SKIP_DATA_SAVE} and immediately persists the change to config.yaml.
     * Console-compatible — no player entity required.
     *
     * @param source the command source
     * @return command result status
     */
    private static int handleDevSkipSave(CommandSourceStack source) {
        CommandSender sender = source.getSender();
        @SuppressWarnings("unchecked")
        Config.ConfigEntry<Boolean> entry = (Config.ConfigEntry<Boolean>) Config.ENTRIES.stream()
            .filter(e -> e.path().equals("debug.skip_data_save"))
            .findFirst()
            .orElseThrow();
        boolean newValue = !Config.Debug.SKIP_DATA_SAVE;
        ConfigManager.getInstance().setValue(entry, newValue);
        ConfigManager.getInstance().saveConfig();
        sender.sendMessage(Component.text(
            "SKIP_DATA_SAVE -> " + newValue + " (saved to config.yaml).",
            newValue ? NamedTextColor.YELLOW : NamedTextColor.GREEN
        ));
        return Command.SINGLE_SUCCESS;
    }

    /**
     * Handles {@code /sword dev freshprofile [player]}.
     * Replaces the target player's session data with a fresh {@link PlayerData} instance.
     * If run from console without a target argument, an error is returned.
     *
     * @param source     the command source
     * @param playerName the target player name, or {@code null} to use the sender
     * @return command result status
     */
    private static int handleDevFreshProfile(CommandSourceStack source, String playerName) {
        CommandSender sender = source.getSender();
        Player target = resolvePlayer(sender, playerName);
        if (target == null) return 2;

        PlayerDataManager.resetToFresh(target.getUniqueId());
        sender.sendMessage(Component.text(
            "Reset " + target.getName() + "'s session data to a fresh profile.",
            NamedTextColor.GREEN
        ));
        if (!target.equals(sender)) {
            target.sendMessage(Component.text(
                "Your session data has been reset to a fresh profile by an admin.",
                NamedTextColor.YELLOW
            ));
        }
        return Command.SINGLE_SUCCESS;
    }

    /**
     * Handles {@code /sword dev resetjoin [player]}.
     * Toggles the {@code joinSequenceCompleted} flag on the target player's {@link PlayerData}.
     * If run from console without a target argument, an error is returned.
     *
     * @param source     the command source
     * @param playerName the target player name, or {@code null} to use the sender
     * @return command result status
     */
    private static int handleDevResetJoin(CommandSourceStack source, String playerName) {
        CommandSender sender = source.getSender();
        Player target = resolvePlayer(sender, playerName);
        if (target == null) return 2;

        PlayerData data = PlayerDataManager.getPlayerData(target.getUniqueId());
        if (data == null) {
            sender.sendMessage(Component.text("No PlayerData found for " + target.getName() + ".", NamedTextColor.RED));
            return 2;
        }
        boolean newValue = !data.isJoinSequenceCompleted();
        data.setJoinSequenceCompleted(newValue);
        sender.sendMessage(Component.text(
            target.getName() + "'s joinSequenceCompleted -> " + newValue + ".",
            NamedTextColor.GREEN
        ));
        return Command.SINGLE_SUCCESS;
    }

    /**
     * Resolves a target {@link Player} from the given name or falls back to the sender.
     * Sends an error message and returns {@code null} if resolution fails.
     *
     * @param sender the command sender (used as fallback when {@code name} is null)
     * @param name   the player name to look up, or {@code null} to use the sender
     * @return the resolved online player, or {@code null} if not found
     */
    private static Player resolvePlayer(CommandSender sender, String name) {
        if (name != null) {
            Player found = Bukkit.getPlayer(name);
            if (found == null) {
                sender.sendMessage(Component.text("Player not found: " + name, NamedTextColor.RED));
            }
            return found;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text(
                "Console must specify a player: /sword dev <subcommand> <player>", NamedTextColor.RED
            ));
            return null;
        }
        return player;
    }
}
