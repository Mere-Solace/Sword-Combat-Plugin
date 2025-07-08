package btm.sword.listeners;

import btm.sword.Sword;
import btm.sword.system.action.utility.UtilityAction;
import btm.sword.system.entity.Combatant;
import btm.sword.system.entity.SwordEntity;
import btm.sword.system.entity.SwordEntityArbiter;
import btm.sword.system.entity.SwordPlayer;
import btm.sword.system.item.prefab.Prefab;
import btm.sword.util.dev.DevMenu;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.intellij.lang.annotations.Subst;

public class PlayerListener implements Listener {
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		Player p = event.getPlayer();
		SwordEntityArbiter.register(p);
		p.sendMessage("Hello!");
	}
	
	@EventHandler
	public void onPlayerLeave(PlayerQuitEvent event) {
		SwordEntityArbiter.remove(event.getPlayer().getUniqueId());
		Sword.getInstance().getLogger().info(event.getPlayer().getName() + " has left the server ;(");
	}
	
	@EventHandler
	public void onPlayerDeath(PlayerDeathEvent event) {

	}
	
	@EventHandler
	public void onPlayerRespawn(PlayerRespawnEvent event) {
		SwordEntityArbiter.register(event.getPlayer());
		event.getPlayer().sendMessage("You've been reassigned to your SwordPlayer object!");
		SwordPlayer swordPlayer = (SwordPlayer) SwordEntityArbiter.getOrAdd(event.getPlayer().getUniqueId());
		event.getPlayer().sendMessage("You: " + swordPlayer);
		
		swordPlayer.onSpawn();
	}
	
	@EventHandler
	public void onItemPickup(EntityPickupItemEvent event) {
		SwordEntity e = SwordEntityArbiter.getOrAdd(event.getEntity().getUniqueId());
		if (!e.isAbleToPickup())
			event.setCancelled(true);
	}
	
	@EventHandler
	public void inventoryEvent(InventoryEvent event) {
		for (HumanEntity h : event.getViewers()) {
			if (h instanceof Player) {
				SwordEntityArbiter.get(h.getUniqueId()).message("getInventory(): " + event.getInventory() + "\n  getView(): " + event.getView());
			}
		}
	}
	
	@EventHandler
	public void inventoryInteractEvent(InventoryClickEvent event) {
		SwordPlayer sp = (SwordPlayer) SwordEntityArbiter.getOrAdd(event.getViewers().getFirst().getUniqueId());
		ClickType clickType = event.getClick();
		InventoryAction action = event.getAction();
		sp.message("Click event firing.\nInventory: " + event.getInventory().getType()
				+ "\nClick type: " + clickType
				+ "\nAction type: " + action);
		
		switch (clickType) {
			case SWAP_OFFHAND -> sp.setSwappingInInv();
			case DROP, CONTROL_DROP -> sp.setDroppingInInv();
			case SHIFT_RIGHT -> {
				sp.message("Shift right clicking!");
				sp.entity().getWorld().dropItem(sp.getChestLocation(), event.getCursor());
				sp.player().getInventory().setItem(event.getSlot(), new ItemStack(Material.AIR));
			}
			case DOUBLE_CLICK -> {
				sp.message("Double clicked smth");
				sp.entity().getWorld().dropItem(sp.getChestLocation(), event.getCursor());
				sp.player().getInventory().setItem(event.getSlot(), new ItemStack(Material.AIR));
			}
			case SHIFT_LEFT -> {
				sp.message("Shift lefting!");
				sp.entity().getWorld().dropItem(sp.getChestLocation(), event.getCursor());
				sp.player().getInventory().setItem(event.getSlot(), new ItemStack(Material.AIR));
			}
		}
		
		switch (action) {
			case DROP_ALL_SLOT, DROP_ALL_CURSOR, DROP_ONE_SLOT, DROP_ONE_CURSOR, UNKNOWN -> {
				sp.message("Dropping is detected");
				sp.setDroppingInInv();
			}
			case SWAP_WITH_CURSOR, HOTBAR_SWAP -> {
				sp.message("Swapping detected");
				sp.setSwappingInInv();
			}
			case PICKUP_ALL, PICKUP_HALF, PICKUP_ONE, PICKUP_SOME -> {
				sp.message("You picked something up");
			}
			case PLACE_ALL, PLACE_SOME, PLACE_ONE -> sp.message("You placed something");
		}
	}
	
	@EventHandler
	public void onMessage(AsyncChatEvent event) {
		Player player = event.getPlayer();
		
		Component msg = event.message();
		
		String cleaned = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(msg).trim();
		
		Sword.getInstance().getLogger().info("Chat input: " + cleaned);
		
		if (cleaned.startsWith("sound")) {
			if (cleaned.startsWith("soundTest ")) {
				UtilityAction.soundTest(
						(Combatant) SwordEntityArbiter.getOrAdd(player.getUniqueId()),
						Integer.parseInt(cleaned.split(" ")[1])
				);
				player.sendMessage(Component.text("§cSound test started at index " + cleaned.split(" ")[1]));
				return;
			}
			String[] parts = cleaned.split("\\s+");
			if (parts.length >= 2) {
				@Subst("king.phylum.classy") String soundKey = parts[1];
				float volume = 1f;
				float pitch = 1f;
				if (parts.length >= 3) {
					volume = Float.parseFloat(parts[2]);
				}
				if (parts.length >= 4) {
					pitch = Float.parseFloat(parts[3]);
				}
				Sound sound = Sound.sound(
						Key.key(soundKey),
						Sound.Source.PLAYER,
						volume,
						pitch
				);
				player.playSound(sound);
				player.sendMessage("§aPlayed sound: " + soundKey);
				event.setCancelled(true);
			}
		}
		else if (cleaned.startsWith("particle ")) {
			String[] parts = cleaned.split("\\s+");
			if (parts.length >= 2) {
				String particleKey = parts[1];
				int count = 10;
				if (parts.length >= 3) {
					count = Integer.parseInt(parts[2]);
				}
				try {
					Particle particle = Particle.valueOf(particleKey.toUpperCase());
					player.getWorld().spawnParticle(
							particle,
							player.getLocation().add(0, 1, 0),
							count
					);
					player.sendMessage("§bDisplayed particle: " + particleKey);
				} catch (IllegalArgumentException ex) {
					player.sendMessage("§cUnknown particle: " + particleKey);
				}
				event.setCancelled(true);
			}
		}
		else if (cleaned.startsWith("give")) {
			SwordEntityArbiter.getOrAdd(player.getUniqueId()).giveItem(Prefab.sword);
		}
		else if (cleaned.startsWith("dev tools")) {
			SwordPlayer sp = (SwordPlayer) SwordEntityArbiter.getOrAdd(player.getUniqueId());
			player.openInventory(DevMenu.create(sp));
		}
	}
}
