package btm.sword.system.entity;

import btm.sword.system.playerdata.StatType;
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

public class SwordPlayer extends Combatant {
	private final InputExecutionTree inputExecutionTree;
	private final long inputTimeoutMillis = 1000L;
	
	private Material itemInUse = Material.AIR;
	private Material itemLastUsed = Material.AIR;
	
	private boolean performedDropAction = false;
	
	public SwordPlayer(LivingEntity associatedEntity, PlayerData data) {
		super(associatedEntity, data.getCombatProfile());
		inputExecutionTree = new InputExecutionTree(inputTimeoutMillis);
		inputExecutionTree.initializeInputTree(this);
	}
	
	public void takeInput(InputType input, Material itemUsed) {
		itemInUse = itemUsed;
		// the takeInput call in this if-statement is where the runnable associated with the node is run.
		inputExecutionTree.takeInput(input, itemUsed, this);
		
		itemLastUsed = itemUsed;
		
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
	
	public boolean atRoot() {
		return inputExecutionTree.atRoot();
	}
	
	public long calcCooldown(long min, long base, StatType stat, double multiplier) {
		return (long) Math.max(min, base - (this.getCombatProfile().getStat(stat) * multiplier));
	}
	
	public void displayInputSequence() {
		associatedEntity.showTitle(Title.title(
				Component.text(""),
				Component.text(inputExecutionTree.toString(), NamedTextColor.DARK_RED, TextDecoration.ITALIC),
				Title.Times.times(
						Duration.ofMillis(0),
						Duration.ofMillis(inputTimeoutMillis),
						Duration.ofMillis(100))));
	}
	
	public void displayDisablingEffect() {
		associatedEntity.showTitle(Title.title(
				Component.text(""),
				Component.text("hah ur disabled", NamedTextColor.DARK_GRAY, TextDecoration.ITALIC),
				Title.Times.times(
						Duration.ofMillis(0),
						Duration.ofMillis(inputTimeoutMillis),
						Duration.ofMillis(100))));
	}
	
	public void displayCooldown(long timeLeft) {
		associatedEntity.showTitle(Title.title(
				Component.text(""),
				Component.text("on cooldown: " + (double) timeLeft*(1/1000), NamedTextColor.GRAY, TextDecoration.ITALIC),
				Title.Times.times(
						Duration.ofMillis(0),
						Duration.ofMillis(inputTimeoutMillis),
						Duration.ofMillis(100))));
	}
	
	public void addStat(StatType stat, int amount) {
		combatProfile.addStat(stat, amount);
	}
}
