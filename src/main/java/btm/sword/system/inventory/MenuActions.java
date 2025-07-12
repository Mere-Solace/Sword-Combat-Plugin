package btm.sword.system.inventory;

import btm.sword.Sword;
import btm.sword.system.entity.Developer;
import btm.sword.util.Cache;
import btm.sword.util.ParticleWrapper;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.entity.*;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

public class MenuActions {
	public static void spawnDisplay(Developer developer, ClickType clickType) {
		Player player = developer.player();
		boolean main;
		switch (clickType) {
			case LEFT, SHIFT_LEFT -> main = false;
			case RIGHT, SHIFT_RIGHT -> main = true;
			default -> {
				return;
			}
		}
		ItemStack itemStack = developer.getItemStackInHand(main);
		if (itemStack == null || itemStack.getType().isAir()) return;
		
		DevMenu menu = developer.getDevMenu();
		Display display;
		if (itemStack.getType().isBlock()) {
			display = (BlockDisplay) player.getWorld().spawnEntity(player.getEyeLocation().add(player.getEyeLocation().getDirection().multiply(2)), EntityType.BLOCK_DISPLAY);
		} else {
			display = (ItemDisplay) player.getWorld().spawnEntity(player.getEyeLocation().add(player.getEyeLocation().getDirection().multiply(2)), EntityType.ITEM_DISPLAY);
		}
		
		if (display instanceof BlockDisplay bd) {
			bd.setBlock(itemStack.getType().createBlockData());
			bd.setGlowing(true);
			bd.setGlowColorOverride(Color.fromRGB(255, 131, 37));
		} else {
			((ItemDisplay) display).setItemStack(itemStack);
		}
		
		menu.addNewDisplay(itemStack, display);
		
		new BukkitRunnable() {
			@Override
			public void run() {
				if (display.isDead()) cancel();
				
				if (menu.shouldDisplayOriginParticles()) {
					Cache.thrownItemMarkerParticle.display(display.getLocation());
					new ParticleWrapper(Particle.END_ROD, 1, 0, 0, 0, 0).display(display.getLocation());
				}
			}
		}.runTaskTimer(Sword.getInstance(), 0L, 3L);
	}
}
