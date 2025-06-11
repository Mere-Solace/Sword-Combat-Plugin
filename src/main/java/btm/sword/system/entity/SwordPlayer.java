package btm.sword.system.entity;

import btm.sword.system.StatType;
import btm.sword.system.action.MovementAction;
import btm.sword.system.action.UtilityAction;
import btm.sword.system.input.InputExecutionTree;
import btm.sword.system.input.InputType;
import btm.sword.system.CombatProfile;
import btm.sword.system.playerdata.PlayerData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;

import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;

import java.time.Duration;
import java.util.List;

public class SwordPlayer extends SwordEntity {
	private final CombatProfile combatProfile;
	
	private final InputExecutionTree inputExecutionTree;
	private Material itemInUse = Material.AIR;
	
	// Utility attributes
	private boolean performedDropAction = false;
	
	
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
		
		if (!inputExecutionTree.takeInput(input, itemUsed, itemInUse)) {
			inputExecutionTree.reset();
			if (itemUsed != itemInUse) itemInUse = itemUsed;
			return;
		}
		
		if (itemUsed != itemInUse) itemInUse = itemUsed;
		
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
	
	public boolean hasPerformedDropAction() {
		return performedDropAction;
	}
	
	public void setPerformedDropAction(boolean performedDropAction) {
		this.performedDropAction = performedDropAction;
	}
	
	public void initializeInputTree() {
			// Item independent actions:
		// dodge forward, dodge backward
		set(List.of(InputType.DROP, InputType.DROP),
				MovementAction.dash(this, true),
				false);
		
		set(List.of(InputType.SHIFT, InputType.DROP),
				MovementAction.dash(this, false),
				false);
		
		// grab
		set(List.of(InputType.SHIFT, InputType.RIGHT),
				UtilityAction.grab(this),
				false);
		
			// Item dependent actions:
		// basic attack sequence
		set(List.of(InputType.LEFT), null,
				true);
		
		set(List.of(InputType.LEFT, InputType.LEFT), null,
				true);
		
		set(List.of(InputType.LEFT, InputType.LEFT, InputType.LEFT), null,
				true);
		
		// heavy attack sequence
		set(List.of(InputType.RIGHT, InputType.LEFT), null,
				true);
		
		set(List.of(InputType.RIGHT, InputType.LEFT, InputType.LEFT), null,
				true);
		
		// skills
		set(List.of(InputType.DROP, InputType.RIGHT, InputType.SHIFT), null,
				true);
		
		set(List.of(InputType.DROP, InputType.RIGHT, InputType.DROP), null,
				true);
		
		set(List.of(InputType.DROP, InputType.RIGHT, InputType.LEFT), null,
				true);
	}
	
	public void set(List<InputType> sequence, Runnable action, boolean sameItemRequired) {
		inputExecutionTree.add(sequence, action, sameItemRequired);
	}
	
	public void addStat(StatType stat, int amount) {
		combatProfile.addStat(stat, amount);
	}
}
