package btm.sword.commands;

import btm.sword.Sword;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class ReloadPlugin implements CommandExecutor {
	private final String pluginName;
	private final String sourcePath;
	
	public ReloadPlugin(String pluginName, String sourcePath) {
		this.pluginName = pluginName;
		this.sourcePath = sourcePath;
	}
	
	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
		Plugin plugin = Bukkit.getPluginManager().getPlugin(pluginName);
		
		if (plugin == null) {
			sender.sendMessage("Could not find plugin '" + pluginName + "' to reload!");
			return true;
		}
		
		try {
			Path source = Paths.get(sourcePath);
			Path target = Paths.get("plugins", pluginName + ".jar");
			
			if (!Files.exists(source)) {
				sender.sendMessage("Source plugin could not be found at: " + source);
				return true;
			}
			
			Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
			
			Bukkit.getPluginManager().disablePlugin(plugin);
			Bukkit.getPluginManager().enablePlugin(plugin);
			
			sender.sendMessage("Plugin " + pluginName + " has been reloaded successfully");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
		return true;
	}
}
