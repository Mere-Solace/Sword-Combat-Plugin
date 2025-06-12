package btm.sword.system.action;

import btm.sword.system.entity.SwordEntity;
import org.bukkit.Material;
import org.bukkit.scheduler.BukkitRunnable;

public class AttackAction {
	
	public static Runnable basic(SwordEntity executor, int stage, Material item) {
		if (item.name().endsWith("_SWORD")) {
			return basicSword(executor, stage, item);
		}
		return null;
	}
	
	public static Runnable basicSword(SwordEntity executor, int stage, Material swordType) {
		double damage;
		switch (swordType) {
			case NETHERITE_SWORD -> damage = 15;
			case DIAMOND_SWORD -> damage = 11;
			case IRON_SWORD -> damage = 8;
			case GOLDEN_SWORD -> damage = 6;
			default -> damage = 4;
		}
		
		return new BukkitRunnable() {
			@Override
			public void run() {
			
			}
		};
	}
}
