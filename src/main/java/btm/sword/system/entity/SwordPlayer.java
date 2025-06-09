package btm.sword.system.entity;

import btm.sword.Sword;
import btm.sword.combat.attack.Attack;
import btm.sword.combat.attack.AttackTriggerType;
import btm.sword.system.CombatProfile;
import btm.sword.system.playerdata.PlayerData;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class SwordPlayer extends SwordEntity implements Combatant {
	private final CombatProfile combatProfile;
	
	// Utility attributes
	private int ticksRightClick = 0;
	private boolean isHoldingRight = false;
	private boolean cancelRightClick = false;
	
	
	
	public SwordPlayer(LivingEntity associatedEntity, PlayerData data) {
		super(associatedEntity);
		combatProfile = data.getCombatProfile();
	}
	
	@Override
	public void performAbility(Material itemType, AttackTriggerType trigger) {
		Attack attack = combatProfile.getAttack(itemType, trigger, this);
		if (attack == null) {
//			associatedEntity.sendMessage("You can't perform an attack with a " + itemType);
			return;
		}
		
		attack.run();
	}
	
	public int getTicksRightClick() {
		return ticksRightClick;
	}
	
	public boolean isHoldingRightClick() {
		return isHoldingRight;
	}
	
	public void setIsHoldingRight(boolean isHoldingRight) {
		this.isHoldingRight = isHoldingRight;
	}
	
	public void setCancelRightClick(boolean cancelRightClick) {
		this.cancelRightClick = cancelRightClick;
	}
	
	public void startRightClickTimer() {
		BukkitTask timer = new BukkitRunnable() {
			@Override
			public void run() {
				if (!isHoldingRight || cancelRightClick) {
					associatedEntity.sendMessage("You held right click for " + ((double)ticksRightClick)/20 + " seconds.");
					ticksRightClick = 0;
					isHoldingRight = false;
					cancelRightClick = false;
					this.cancel();
					return;
				}

				ticksRightClick++;
			}
		}.runTaskTimer(Sword.getInstance(), 0, 1);
	}
}
