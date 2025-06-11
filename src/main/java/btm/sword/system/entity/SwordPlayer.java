package btm.sword.system.entity;

import btm.sword.Sword;
import btm.sword.system.input.InputExecutionTree;
import btm.sword.combat.attack.Attack;
import btm.sword.system.input.InputType;
import btm.sword.system.CombatProfile;
import btm.sword.system.playerdata.PlayerData;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

public class SwordPlayer extends SwordEntity implements Combatant {
	private final CombatProfile combatProfile;
	
	private final InputExecutionTree inputExecutionTree;
	
	// Utility attributes
	private int ticksRightClick = 0;
	private boolean isHoldingRight = false;
	private boolean cancelRightClick = false;
	
	private boolean performedDropAction = false;
	
	public SwordPlayer(LivingEntity associatedEntity, PlayerData data) {
		super(associatedEntity);
		combatProfile = data.getCombatProfile();
		inputExecutionTree = new InputExecutionTree(combatProfile);
	}
	
	@Override
	public void performAbility(Material itemType, List<InputType> trigger) {
		Attack attack = combatProfile.getAttack(itemType, trigger, this);
		if (attack == null) {
//			associatedEntity.sendMessage("You can't perform an attack with a " + itemType);
			return;
		}
		
		// TODO keep track of the attack for cancellation
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
		new BukkitRunnable() {
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
	
	public boolean hasPerformedDropAction() {
		return performedDropAction;
	}
	
	public void setPerformedDropAction(boolean performedDropAction) {
		this.performedDropAction = performedDropAction;
	}
}
