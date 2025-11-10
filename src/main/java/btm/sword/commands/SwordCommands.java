package btm.sword.commands;

import java.util.List;

import org.bukkit.command.CommandSender;

import com.mojang.brigadier.Command;

import btm.sword.config.ConfigManager;
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
                    // /sword with no args - show help
                    CommandSender sender = ctx.getSource().getSender();
                    sender.sendMessage(Component.text("Sword: Combat Evolved", NamedTextColor.GOLD));
                    sender.sendMessage(Component.text("Usage: /sword reload", NamedTextColor.GRAY));
                    return Command.SINGLE_SUCCESS;
                })
                .then(
                    Commands.literal("reload")
                        .requires(source -> source.getSender().hasPermission("sword.reload"))
                        .executes(ctx -> {
                            return handleReload(ctx.getSource());
                        })
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
        }

        return Command.SINGLE_SUCCESS;
    }
}
