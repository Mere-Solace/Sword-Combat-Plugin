package btm.sword.system.entity;

import btm.sword.system.action.AttackAction;
import btm.sword.system.input.InputAction;
import btm.sword.system.playerdata.StatType;
import btm.sword.system.action.MovementAction;
import btm.sword.system.action.UtilityAction;
import btm.sword.system.input.InputExecutionTree;
import btm.sword.system.input.InputType;
import btm.sword.system.playerdata.PlayerData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;

import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;

import java.time.Duration;
import java.util.List;

public class SwordPlayer extends Combatant {
	private final InputExecutionTree inputExecutionTree;
	
	private Material itemInUse = Material.AIR;
	private Material itemLastUsed = Material.AIR;
	
	private boolean performedDropAction = false;
	
	public SwordPlayer(LivingEntity associatedEntity, PlayerData data) {
		super(associatedEntity, data.getCombatProfile());
		inputExecutionTree = new InputExecutionTree();
		initializeInputTree();
	}
	
	public void takeInput(InputType input, Material itemUsed) {
		associatedEntity.sendMessage(itemUsed.toString());
		itemInUse = itemUsed;
		
		// the takeInput call in this if-statement is where the runnable associated with the node is run.
		if (!inputExecutionTree.takeInput(input, itemUsed, this)) {
			inputExecutionTree.reset();
			itemLastUsed = itemUsed;
			associatedEntity.showTitle(Title.title(
					Component.text(""),
					Component.text("/*~*#*~*/", NamedTextColor.DARK_GRAY, TextDecoration.BOLD),
					Title.Times.times(
							Duration.ofMillis(0),
							Duration.ofMillis(500),
							Duration.ofMillis(100))
			));
			return;
		}
		
		itemLastUsed = itemUsed;
		
		associatedEntity.showTitle(Title.title(
				Component.text(""),
				Component.text("| " + inputExecutionTree + " |", NamedTextColor.DARK_RED, TextDecoration.ITALIC),
				Title.Times.times(
						Duration.ofMillis(0),
						Duration.ofMillis(500),
						Duration.ofMillis(100))
		));
		
		if (inputExecutionTree.noChildren()) inputExecutionTree.reset();
	}
	
	public Material getItemInUse() {
		return itemInUse;
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
	
	public void initializeInputTree() {
			// Item independent actions:
		// dodge forward, dodge backward
		set(List.of(InputType.DROP, InputType.DROP),
				new InputAction(
						MovementAction.dash(this, true),
						executor -> executor.calcCooldown(200L, 1000L, StatType.CELERITY, 10),
						Combatant::cannotPerformAction),
				false);
		
		set(List.of(InputType.SHIFT, InputType.DROP),
				new InputAction(
						MovementAction.dash(this, false),
						executor -> executor.calcCooldown(200L, 1000L, StatType.CELERITY, 10),
						Combatant::cannotPerformAction),
				false);
		
		// grab
		set(List.of(InputType.SHIFT, InputType.RIGHT),
				new InputAction(
				UtilityAction.grab(this),
						executor -> executor.calcCooldown(200L, 1000L, StatType.FORTITUDE, 10),
						Combatant::cannotPerformAction),
				false);
		
			// Item dependent actions:
		// basic attack sequence
		set(List.of(InputType.LEFT),
				new InputAction(
						AttackAction.basic(this, 0),
						executor -> executor.calcCooldown(200L, 1000L, StatType.FORM, 10),
						Combatant::cannotPerformAction),
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
		
		set(List.of(InputType.DROP, InputType.RIGHT, InputType.LEFT),
				new InputAction(
						AttackAction.basic(this, 1),
						executor -> executor.calcCooldown(200L, 1000L, StatType.FORM, 10),
						Combatant::cannotPerformAction),
				true);
	}
	
	public void set(List<InputType> sequence, InputAction action, boolean sameItemRequired) {
		inputExecutionTree.add(sequence, action, sameItemRequired);
	}
	
	public long calcCooldown(long min, long base, StatType stat, double multiplier) {
		return (long) Math.max(min, base - (this.getCombatProfile().getStat(stat) * multiplier));
	}
	
	public void addStat(StatType stat, int amount) {
		combatProfile.addStat(stat, amount);
	}
}
