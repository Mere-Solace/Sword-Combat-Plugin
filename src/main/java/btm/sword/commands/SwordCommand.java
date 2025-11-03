package btm.sword.commands;

import btm.sword.config.ConfigManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Main command handler for /sword.
 * <p>
 * Subcommands:
 * - /sword reload - Hot reloads configuration from disk
 * </p>
 */
public class SwordCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Component.text("Sword: Combat Evolved", NamedTextColor.GOLD));
            sender.sendMessage(Component.text("Usage: /sword reload", NamedTextColor.GRAY));
            return true;
        }

        String subcommand = args[0].toLowerCase();

        switch (subcommand) {
            case "reload" -> {
                return handleReload(sender);
            }
            default -> {
                sender.sendMessage(Component.text("Unknown subcommand: " + subcommand, NamedTextColor.RED));
                sender.sendMessage(Component.text("Usage: /sword reload", NamedTextColor.GRAY));
                return true;
            }
        }
    }

    /**
     * Handles the /sword reload subcommand.
     * <p>
     * Reloads configuration from disk, allowing for hot config updates during testing.
     * </p>
     *
     * @param sender The command sender
     * @return true if command was handled successfully
     */
    private boolean handleReload(CommandSender sender) {
        // Permission check
        if (!sender.hasPermission("sword.reload")) {
            sender.sendMessage(Component.text("You don't have permission to reload the config.", NamedTextColor.RED));
            return true;
        }

        sender.sendMessage(Component.text("Reloading Sword: Combat Evolved configuration...", NamedTextColor.YELLOW));

        try {
            boolean success = ConfigManager.getInstance().reload();

            if (success) {
                sender.sendMessage(Component.text("✓ Configuration reloaded successfully!", NamedTextColor.GREEN));
                sender.sendMessage(Component.text("  All values have been updated from config.yaml", NamedTextColor.GRAY));
            } else {
                sender.sendMessage(Component.text("✗ Configuration reload failed!", NamedTextColor.RED));
                sender.sendMessage(Component.text("  Check console for error details. Using previous values.", NamedTextColor.GRAY));
            }
        } catch (Exception e) {
            sender.sendMessage(Component.text("✗ Fatal error during reload: " + e.getMessage(), NamedTextColor.DARK_RED));
            sender.sendMessage(Component.text("  Check console for full stack trace.", NamedTextColor.GRAY));
        }

        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // Subcommand completions
            if ("reload".startsWith(args[0].toLowerCase())) {
                completions.add("reload");
            }
        }

        return completions;
    }
}
