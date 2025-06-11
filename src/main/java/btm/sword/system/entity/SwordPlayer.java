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
	
	public void takeInput(InputType input, Material itemUsed) {
		if (itemUsed != itemInUse) {
			inputExecutionTree.reset();
			itemInUse = itemUsed;
		}
		
		if (!inputExecutionTree.takeInput(input)) {
			inputExecutionTree.reset();
			return;
		}
		
		associatedEntity.showTitle(Title.title(
				Component.text(""),
				Component.text(inputExecutionTree.toString(), NamedTextColor.DARK_RED, TextDecoration.ITALIC),
				Title.Times.times(
						Duration.ofMillis(0),
						Duration.ofMillis(500),
						Duration.ofMillis(100)
				)
		));
		
		if (inputExecutionTree.noChildren()) {
			inputExecutionTree.reset();
		}
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
				MovementAction.dash(this, 1+(0.1*combatProfile.getStat(StatType.CELERITY)), true));
		
		set(List.of(InputType.SHIFT, InputType.DROP),
				MovementAction.dash(this, 1+(0.1*combatProfile.getStat(StatType.CELERITY)), false));
		
		// grab
		set(List.of(InputType.SHIFT, InputType.RIGHT),
				UtilityAction.grab(this, 2+(0.1*combatProfile.getStat(StatType.WILLPOWER))));
		
			// Item dependent actions:
		// basic attack sequence
		set(List.of(InputType.LEFT), null);
		set(List.of(InputType.LEFT, InputType.LEFT), null);
		set(List.of(InputType.LEFT, InputType.LEFT, InputType.LEFT), null);
		
		// heavy attack sequence
		set(List.of(InputType.RIGHT, InputType.LEFT), null);
		set(List.of(InputType.RIGHT, InputType.LEFT, InputType.LEFT), null);
		
		// skills
		set(List.of(InputType.DROP, InputType.RIGHT, InputType.SHIFT), null);
		set(List.of(InputType.DROP, InputType.RIGHT, InputType.DROP), null);
		set(List.of(InputType.DROP, InputType.RIGHT, InputType.LEFT), null);
	}
	
	public void set(List<InputType> sequence, Runnable action) {
		inputExecutionTree.add(sequence, action);
	}
}
