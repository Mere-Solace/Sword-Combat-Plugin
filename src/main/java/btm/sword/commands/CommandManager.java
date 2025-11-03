package btm.sword.commands;

import btm.sword.Sword;
import org.bukkit.command.PluginCommand;

/**
 * Manages registration of all plugin commands.
 */
public class CommandManager {
    /**
     * Registers all plugin commands with their executors and tab completers.
     */
    public static void register() {
        Sword plugin = Sword.getInstance();

        // Register /sword command
        PluginCommand swordCmd = plugin.getCommand("sword");
        if (swordCmd != null) {
            SwordCommand swordCommand = new SwordCommand();
            swordCmd.setExecutor(swordCommand);
            swordCmd.setTabCompleter(swordCommand);
        }
    }
}
