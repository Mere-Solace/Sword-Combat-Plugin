package btm.sword.combat.ability.abilitytype;

import btm.sword.Sword;
import btm.sword.combat.ability.Ability;
import btm.sword.system.entity.SwordEntity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class DashAbility extends Ability {
	
	public DashAbility(SwordEntity executor) {
		super(executor);
	}
	
	@Override
	public void onRun() {
		LivingEntity ex = executor.getAssociatedEntity();
		double dashPower = 1;
		double m = ex.isSneaking() ? -dashPower : dashPower;
		
		for (int i = 0; i < 2; i++) {
			new BukkitRunnable() {
				@Override
				public void run() {
					ex.setVelocity(ex.getEyeLocation().getDirection().multiply(m).add(new Vector(0, .4, 0)));
				}
			}.runTaskLater(Sword.getInstance(), i);
		}
	}
}
