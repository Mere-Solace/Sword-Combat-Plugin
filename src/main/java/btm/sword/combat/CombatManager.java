package btm.sword.combat;

import btm.sword.Sword;
import btm.sword.combat.attack.AttackType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Iterator;

public class CombatManager {
	public static void executeAttack(Player player, AttackType attack) {
		double damage = attack.calculateDamage();
		double range = attack.calculateRange(5.0);
		
		Location origin = player.getEyeLocation().add(new Vector(0,-0.1,0));
		Vector dir = origin.getDirection();
		
		HashSet<LivingEntity> targets = attack.getTargets(player, origin, dir, range);
		
		Bukkit.getScheduler().runTaskAsynchronously(Sword.getInstance(), () -> attack.drawEffects(origin, dir, range, targets));

		for (LivingEntity target : targets) {
			target.damage(damage, player);
		}
		
		if (!targets.isEmpty()) {
			StringBuilder attackReport = new StringBuilder("[Targets Hit:\n");
			Iterator<LivingEntity> i = targets.iterator();
			while (i.hasNext()) {
				LivingEntity target = i.next();
				attackReport.append("  ").append(target);
				if (i.hasNext()) {
					attackReport.append(",");
				}
				attackReport.append("\n");
			}
			attackReport.append("  ").append("For ").append(damage).append(" Damage]");
			player.sendMessage(attackReport.toString());
		}
	}
}
