package btm.sword.system.entity;

import btm.sword.system.input.InputAction;
import btm.sword.system.playerdata.StatType;
import btm.sword.system.action.MovementAction;
import btm.sword.system.action.UtilityAction;
import btm.sword.system.input.InputExecutionTree;
import btm.sword.system.input.InputType;
import btm.sword.system.playerdata.CombatProfile;
import btm.sword.system.playerdata.PlayerData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;

import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.List;

public class SwordPlayer extends SwordEntity {
	private final CombatProfile combatProfile;
	private final InputExecutionTree inputExecutionTree;
	
	private Material itemLastUsed = Material.AIR;
	
	private boolean performedDropAction = false;
	
	private boolean isGrabbing = false;
	private BukkitTask grabTask;
	private SwordEntity grabbedEntity;
	
	public SwordPlayer(LivingEntity associatedEntity, PlayerData data) {
		super(associatedEntity);
		combatProfile = data.getCombatProfile();
		inputExecutionTree = new InputExecutionTree();
		initializeInputTree();
	}
	
	public CombatProfile getCombatProfile() {
		return combatProfile;
	}
	
	public void takeInput(InputType input, Material itemUsed) {
		if (isGrabbing) {
			return;
		}
		
		// the takeInput call in this if-statement is where the runnable associated with the node is run.
		if (!inputExecutionTree.takeInput(input, itemUsed, this)) {
			inputExecutionTree.reset();
			if (itemUsed != itemLastUsed) itemLastUsed = itemUsed;
			return;
		}
		
		if (itemUsed != itemLastUsed) itemLastUsed = itemUsed;
		
		associatedEntity.showTitle(Title.title(
				Component.text(""),
				Component.text(inputExecutionTree.toString(), NamedTextColor.DARK_RED, TextDecoration.ITALIC),
				Title.Times.times(
						Duration.ofMillis(0),
						Duration.ofMillis(500),
						Duration.ofMillis(100)
				)
		));
		
		if (inputExecutionTree.noChildren()) inputExecutionTree.reset();
	}
	
	public Material getItemLastUsed() {
		return itemLastUsed;
	}
	
	public boolean hasPerformedDropAction() {
		return performedDropAction;
	}
	
	public void setPerformedDropAction(boolean performedDropAction) {
		this.performedDropAction = performedDropAction;
	}
	
	public boolean isGrabbing() {
		return isGrabbing;
	}
	
	public void setGrabbing(boolean isGrabbing) {
		this.isGrabbing = isGrabbing;
	}
	
	public BukkitTask getGrabTask() {
		return grabTask;
	}
	
	public void setGrabTask(BukkitTask grabTask) {
		this.grabTask = grabTask;
	}
	
	public SwordEntity getGrabbedEntity() {
		return grabbedEntity;
	}
	
	public void setGrabbedEntity(SwordEntity grabbedEntity) {
		this.grabbedEntity = grabbedEntity;
	}
	
	public void initializeInputTree() {
			// Item independent actions:
		// dodge forward, dodge backward
		set(List.of(InputType.DROP, InputType.DROP),
				new InputAction(
						MovementAction.dash(this, true),
						executor -> 1000L - (executor.getCombatProfile().getStat(StatType.CELERITY) * 10L),
						executor -> !executor.isGrabbing()),
				false);
		
		set(List.of(InputType.SHIFT, InputType.DROP),
				new InputAction(
						MovementAction.dash(this, false),
						executor -> 1000L - (executor.getCombatProfile().getStat(StatType.CELERITY) * 10L),
						executor -> !executor.isGrabbing()),
				false);
		
		// grab
		set(List.of(InputType.SHIFT, InputType.RIGHT),
				new InputAction(
				UtilityAction.grab(this),
						executor -> Math.max(100L, 400L - (executor.getCombatProfile().getStat(StatType.FORTITUDE) * 10L)),
						executor -> !executor.isGrabbing()),
				false);
		
			// Item dependent actions:
		// basic attack sequence
		set(List.of(InputType.LEFT), null,
				true);
		
		set(List.of(InputType.LEFT, InputType.LEFT), null,
				true);
		
		set(List.of(InputType.LEFT, InputType.LEFT, InputType.LEFT), null,
				true);
		
		// skills
		set(List.of(InputType.DROP, InputType.RIGHT, InputType.SHIFT), null,
				true);
		
		set(List.of(InputType.DROP, InputType.RIGHT, InputType.DROP), null,
				true);
		
		set(List.of(InputType.DROP, InputType.RIGHT, InputType.LEFT), null,
				true);
	}
	
	public void set(List<InputType> sequence, InputAction action, boolean sameItemRequired) {
		inputExecutionTree.add(sequence, action, sameItemRequired);
	}
	
	public void addStat(StatType stat, int amount) {
		combatProfile.addStat(stat, amount);
	}
}
