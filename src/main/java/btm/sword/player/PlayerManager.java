package btm.sword.player;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.*;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerManager {
	private static final Map<UUID, PlayerData> players = new HashMap<>();
	private static final Gson gson = new Gson();
	private static final File datafile = new File("plugins/sword/playerdata.json");
	
	public static void initialize() {
		loadData();
		
		for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
			registerPlayer(onlinePlayer);
		}
	}
	
	public static void shutdown() {
		saveData();
	}
	
	public static void registerPlayer(Player player) {
		players.putIfAbsent(player.getUniqueId(), new PlayerData(player.getUniqueId()));
	}
	
	public static void unregisterPlayer(Player player) {
		players.remove(player.getUniqueId());
	}
	
	public static PlayerData getPlayerData(UUID uuid) {
		return players.get(uuid);
	}
	
	public static Player getOnlinePlayer(UUID uuid) {
		return Bukkit.getPlayer(uuid);
	}
	
	public static void saveData() {
		try (FileWriter writer = new FileWriter(datafile)) {
			gson.toJson(players, writer);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void loadData() {
		if (!datafile.exists()) return;
		try (FileReader reader = new FileReader(datafile)) {
			Type type = new TypeToken<Map<UUID, PlayerData>>() {}.getType();
			Map<UUID, PlayerData> loaded = gson.fromJson(reader, type);
			if (loaded != null) {
				players.clear();
				players.putAll(loaded);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
