package btm.sword.system.action.utility.thrown;

import btm.sword.system.action.SwordAction;
import btm.sword.system.entity.Combatant;
import btm.sword.system.entity.SwordPlayer;
import org.bukkit.Color;
import org.bukkit.entity.*;
import org.bukkit.inventory.*;
import org.bukkit.scheduler.BukkitRunnable;

public class ThrowAction extends SwordAction {
	public static void throwReady(Combatant executor) {
		executor.setAttemptingThrow(true);
		executor.setThrowCancelled(false);
		executor.setThrowSuccessful(false);
		
		LivingEntity ex = executor.entity();
		ItemDisplay display = (ItemDisplay) ex.getWorld().spawnEntity(ex.getEyeLocation(), EntityType.ITEM_DISPLAY);
		display.setGlowing(true);
		display.setGlowColorOverride(Color.fromRGB(255, 0, 15));
		
		ThrownItem thrownItem;
		if (executor instanceof SwordPlayer sp) {
			display.setItemStack(sp.getMainItemStackAtTimeOfHold());
        }
		else {
			ItemStack main = executor.getItemStackInHand(true);
			ItemStack off = executor.getItemStackInHand(false);
			
			display.setItemStack(main);
        }
        thrownItem = new ThrownItem(executor, display);
        executor.setThrownItem(thrownItem);
		thrownItem.onReady();
	}
	
	public static void throwCancel(Combatant executor) {
		executor.setAttemptingThrow(false);
		executor.setThrowCancelled(true);
		executor.setThrowSuccessful(false);
		
		if (executor instanceof SwordPlayer sp) {
			sp.setItemAtIndex(sp.getMainHandItemStackDuringThrow(), sp.getThrownItemIndex());
		}
		else {
			executor.setItemStackInHand(executor.getMainHandItemStackDuringThrow(), true);
		}
		executor.setItemStackInHand(executor.getOffHandItemStackDuringThrow(), false);
		
		executor.setThrownItem(null);
	}

	public static void throwItem(Combatant executor) {
		if (executor.isThrowCancelled()) return;
		
		executor.setAttemptingThrow(false);
		executor.setThrowSuccessful(true);
		
		cast(executor, 10L, new BukkitRunnable() {
			@Override
			public void run() {
				executor.getThrownItem().onRelease(2);
				executor.setItemStackInHand(executor.getMainHandItemStackDuringThrow(), true);
			}
		});
	}
}
