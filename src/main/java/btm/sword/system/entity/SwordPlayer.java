package btm.sword.system.entity;

import btm.sword.Sword;
import btm.sword.combat.ability.Ability;
import btm.sword.system.input.InputExecutionTree;
import btm.sword.system.input.InputType;
import btm.sword.system.CombatProfile;
import btm.sword.system.playerdata.PlayerData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.List;

public class SwordPlayer extends SwordEntity implements Combatant {
	private final CombatProfile combatProfile;
	
	private final InputExecutionTree inputExecutionTree;
	private Material itemInUse = Material.AIR;
	
//	private final HashMap<Ability, BukkitTask> runningAbilities = new HashMap<>();
	
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
		Ability ability = combatProfile.getAbility(itemType, trigger, this);
		if (ability == null) return;
		
		BukkitScheduler s = Bukkit.getScheduler();
		BukkitTask abilityTask = s.runTaskLater(Sword.getInstance(), ability,  ability.getDelayTicks());
//		runningAbilities.put(ability, abilityTask);
	}
	
	public void takeInput(InputType input, Material itemUsed) {
		if (itemUsed != itemInUse) {
			associatedEntity.sendMessage("Item used: " + itemUsed);
			associatedEntity.sendMessage("Item that was in use: " + itemInUse);
			inputExecutionTree.reset();
			itemInUse = itemUsed;
		}
		
		if (!inputExecutionTree.takeInput(input)) {
			inputExecutionTree.reset();
			return;
		}
		
		if (inputExecutionTree.inExecutionState()) {
			performAbility(itemUsed, inputExecutionTree.getSequence());
		}
		
		associatedEntity.showTitle(Title.title(
				Component.text(""),
				Component.text(inputExecutionTree.toString(), NamedTextColor.GRAY, TextDecoration.ITALIC),
				Title.Times.times(
						Duration.ofMillis(1),
						Duration.ofMillis(350),
						Duration.ofMillis(100)
				)
		));
		
		if (inputExecutionTree.noChildren()) {
			inputExecutionTree.reset();
		}
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
