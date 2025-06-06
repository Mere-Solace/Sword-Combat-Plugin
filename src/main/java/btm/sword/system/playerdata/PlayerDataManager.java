package btm.sword.system.playerdata;

import btm.sword.Sword;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.*;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerDataManager {
	private static final Map<UUID, PlayerData> allPlayerData = new HashMap<>();
	private static final Gson gson = new Gson();
	private static final File datafile = new File("plugins/sword/playerdata.json");
	
	public static void initialize() {
		loadPlayerData();
		
		for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
			register(onlinePlayer.getUniqueId());
		}
	}
	
	public static void shutdown() {
		savePlayerData();
	}
	
	public static void register(UUID uuid) {
		allPlayerData.putIfAbsent(uuid, new PlayerData(uuid));
	}
	
	public static PlayerData getPlayerData(UUID uuid) {
		return allPlayerData.get(uuid);
	}
	
	public static void savePlayerData() {
		try (FileWriter writer = new FileWriter(datafile)) {
			gson.toJson(allPlayerData, writer);
		} catch (IOException e) {
			Sword.getInstance().getLogger().info(e.getMessage());
		}
	}
	
	public static void loadPlayerData() {
		if (!datafile.exists()) return;
		try (FileReader reader = new FileReader(datafile)) {
			Type type = new TypeToken<Map<UUID, PlayerData>>() {}.getType();
			Map<UUID, PlayerData> loaded = gson.fromJson(reader, type);
			if (loaded != null) {
				allPlayerData.clear();
				allPlayerData.putAll(loaded);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
