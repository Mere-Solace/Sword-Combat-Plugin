package btm.sword.combat;

import btm.sword.Sword;
import btm.sword.combat.attack.AttackType;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Collection;

public class CombatManager {
	public static void executeAttack(Player player, AttackType attack) {
		double damage = attack.calculateDamage();
		double range = attack.calculateRange(5.0);
		
		Location origin = player.getLocation();
		Vector dir = player.getEyeLocation().getDirection();
		
		Collection<LivingEntity> targets = attack.getTargets(player, origin, dir, range);
		
		attack.drawEffect(origin, dir, range);
		
		for (LivingEntity target : targets) {
			Sword.getInstance().getLogger().info(target.toString());
			target.damage(damage, player);
		}
	}
}
