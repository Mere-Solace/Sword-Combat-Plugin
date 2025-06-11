package btm.sword.system.action;

import btm.sword.system.StatType;
import btm.sword.system.entity.SwordEntity;
import btm.sword.system.entity.SwordPlayer;
import btm.sword.util.HitboxUtils;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;

public class UtilityAction extends SwordAction {
	
	public static Runnable grab(SwordEntity executor) {
		return new BukkitRunnable() {
			@Override
			public void run() {
				LivingEntity ex = executor.getAssociatedEntity();
				double range = executor instanceof SwordPlayer ? 2 + (0.1*((SwordPlayer) executor).getCombatProfile().getStat(StatType.MIGHT)) : 2;
				LivingEntity target = HitboxUtils.rayTrace(ex, range);
				
				if (target == null)
					ex.sendMessage("Miss");
				else
					ex.sendMessage(ex + " grabbed " + target);
			}
		};
	}
}
