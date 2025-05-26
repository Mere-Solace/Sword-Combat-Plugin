//package btm.sword.commands;
//
//import io.papermc.paper.command.brigadier.BasicCommand;
//import io.papermc.paper.command.brigadier.CommandSourceStack;
//import org.bukkit.Bukkit;
//import org.bukkit.command.CommandSender;
//import org.bukkit.plugin.Plugin;
//import org.jetbrains.annotations.NotNull;
//
//import java.io.IOException;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.nio.file.StandardCopyOption;
//
//public class ReloadPluginCommand implements BasicCommand {
//	private final String pluginName;
//	private final String sourcePath;
//
//	public ReloadPluginCommand() {
//		this.pluginName = "sword";
//		this.sourcePath = "C:\\Users\\btm74\\IdeaProjects\\sword\\out\\artifacts\\sword_jar\\sword.jar";
//	}
//
//	@Override
//	public void execute(@NotNull CommandSourceStack commandSourceStack, @NotNull String[] args) {
//		CommandSender sender = commandSourceStack.getSender();
//		Plugin plugin = Bukkit.getPluginManager().getPlugin(pluginName);
//
//		if (plugin == null) {
//			sender.sendMessage("Could not find plugin '" + pluginName + "' to reload!");
//			return;
//		}
//
//		try {
//			Path source = Paths.get(sourcePath);
//			Path target = Paths.get("plugins", pluginName + ".jar");
//
//			if (!Files.exists(source)) {
//				sender.sendMessage("Source plugin could not be found at: " + source);
//				return;
//			}
//
//			Bukkit.getPluginManager().disablePlugin(plugin);
//
//			Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
//
//			Bukkit.getPluginManager().enablePlugin(plugin);
//
//			sender.sendMessage("Plugin " + pluginName + " has been reloaded successfully");
//		} catch (IOException e) {
//			throw new RuntimeException(e);
//		}
//	}
//}
