package btm.sword.system.action;

import btm.sword.system.entity.SwordEntity;
import btm.sword.util.HitboxUtils;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;

public class UtilityAction extends SwordAction {
	
	public static Runnable grab(SwordEntity executor, double range) {
		return new BukkitRunnable() {
			@Override
			public void run() {
				LivingEntity ex = executor.getAssociatedEntity();
				LivingEntity target = HitboxUtils.rayTrace(ex, range);
				
				if (target == null)
					ex.sendMessage("Miss");
				else
					ex.sendMessage(ex + " grabbed " + target);
			}
		};
	}
}
