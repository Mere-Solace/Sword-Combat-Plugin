package btm.sword.system.entity;

import btm.sword.Sword;
import btm.sword.system.input.InputAction;
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
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;

public class SwordPlayer extends Combatant {
	private final InputExecutionTree inputExecutionTree;
	private final long inputTimeoutMillis = 1200L;
	
	private boolean performedDropAction = false;
	private boolean canDrop;
	
	BukkitTask holdCheckTask;
	InputType currentHoldingType;
	long rightHoldTimeStart;
	long lastHoldTimeRecorded;
	long timeRightHeld;
	
	boolean isSneaking;
	long shitHoldTimeStart;
	long timeShiftHeld;
	
	public SwordPlayer(LivingEntity associatedEntity, PlayerData data) {
		super(associatedEntity, data.getCombatProfile());
		inputExecutionTree = new InputExecutionTree(inputTimeoutMillis);
		inputExecutionTree.initializeInputTree();
		currentHoldingType = InputType.NONE;
		rightHoldTimeStart = 0L;
		lastHoldTimeRecorded = 0L;
		timeRightHeld = 0L;
		isSneaking = false;
	}
	
	public void act(InputType input) {
		if (input == InputType.LEFT && isGrabbing()) {
			onGrabHit();
			return;
		}
		
		if (getAbilityCastTask() != null) {
			return;
		}
		
		if (input == InputType.RIGHT) {
			updateLastHoldTime();
			if (holdCheckTask == null) {
				startRightHoldCheck();
			}
			else {
				inputExecutionTree.restartTimeoutTimer();
				return;
			}
		}
		
		if (input == InputType.R_HOLD || input == InputType.SHIFT_HOLD) {
			long minTime = inputExecutionTree.getMinHoldLengthOfNext(input);
			message("Min Hold Time: " + minTime + ", Right held for " + timeRightHeld + ", Shift held for: " + timeShiftHeld);
			if (minTime == -1
					|| (input == InputType.R_HOLD && timeRightHeld < minTime)
					|| (input == InputType.SHIFT_HOLD && timeShiftHeld < minTime)) {
				return;
			}
		}
		
		InputExecutionTree.InputNode node = inputExecutionTree.step(input);
		
		if (input == InputType.SHIFT) {
			setSneaking();
			inputExecutionTree.stopTimeoutTimer();
		}
		
		if (node != null) {
			if (node.shouldDisplay()) {
				displayInputSequence();
			}
		}
		else return;
		
		InputAction action = node.getAction();
		
		if (action != null) {
			if (action.execute(this)) {
				action.setTimeLastExecuted();
			}
			else {
				resetTree();
			}
		}
	}
	
	public boolean hasPerformedDropAction() {
		return performedDropAction;
	}
	
	public void setPerformedDropAction(boolean performedDropAction) {
		this.performedDropAction = performedDropAction;
	}
	
	public void setCanDrop(boolean canDrop) {
		this.canDrop = canDrop;
	}
	
	public boolean canDrop() {
		return canDrop;
	}
	
	public void resetTree() {
		inputExecutionTree.reset();
	}
	
	public boolean isAtRoot() {
		return inputExecutionTree.isAtRoot();
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
	
	public void displayMistake() {
		associatedEntity.showTitle(Title.title(
				Component.text(""),
				Component.text("~*#*~", NamedTextColor.DARK_GRAY, TextDecoration.ITALIC),
				Title.Times.times(
						Duration.ofMillis(0),
						Duration.ofMillis(inputTimeoutMillis),
						Duration.ofMillis(100))));
	}
	
	public void displayDisablingEffect() {
		associatedEntity.showTitle(Title.title(
				Component.text(""),
				Component.text("ur disabled", NamedTextColor.DARK_GRAY, TextDecoration.ITALIC),
				Title.Times.times(
						Duration.ofMillis(0),
						Duration.ofMillis(inputTimeoutMillis),
						Duration.ofMillis(100))));
	}
	
	public void displayCooldown(long timeLeft) {
		double timeToDisplay = timeLeft > 1000L ? (double)timeLeft/1000 : timeLeft;
		String unit = timeLeft > 1000L ? "s" : "ms";
		associatedEntity.showTitle(Title.title(
				Component.text(""),
				Component.text("on cooldown: " + timeToDisplay + " " + unit, NamedTextColor.GRAY, TextDecoration.ITALIC),
				Title.Times.times(
						Duration.ofMillis(0),
						Duration.ofMillis(inputTimeoutMillis),
						Duration.ofMillis(100))));
	}
	
	public Material getItemInUse() {
		return ((Player) associatedEntity).getInventory().getItemInMainHand().getType();
	}
	
	public void addStat(StatType stat, int amount) {
		combatProfile.addStat(stat, amount);
	}
	
	public boolean inputReliantOnItem() {
		return inputExecutionTree.requiresSameItem();
	}
	
	public void startRightHoldCheck() {
		if (currentHoldingType == InputType.RIGHT) return;
		message("Pressed Right");
		if (holdCheckTask != null) holdCheckTask.cancel();
		
		currentHoldingType = InputType.RIGHT;
		rightHoldTimeStart = System.currentTimeMillis();
		
		holdCheckTask = new BukkitRunnable() {
			@Override
			public void run() {
				long curTime = System.currentTimeMillis();
				if (curTime - lastHoldTimeRecorded > 200L) {
					onStopRightHold();
					message("You stopped holding Right (held for " + ((double)(timeRightHeld)/1000) + " s)");
					resetHolding();
					cancel();
				}
			}
		}.runTaskTimer(Sword.getInstance(), 0, 4L);
	}
	
	public void updateLastHoldTime() {
		lastHoldTimeRecorded = System.currentTimeMillis();
	}
	
	public void resetHolding() {
		holdCheckTask = null;
		currentHoldingType = InputType.NONE;
		rightHoldTimeStart = 0L;
		lastHoldTimeRecorded = 0L;
	}
	
	public void onStopRightHold() {
		timeRightHeld = System.currentTimeMillis() - rightHoldTimeStart;
		act(InputType.R_HOLD);
	}
	
	public void setSneaking() {
		if (isSneaking) return;
		isSneaking = true;
		shitHoldTimeStart = System.currentTimeMillis();
	}
	
	public void endSneaking() {
		isSneaking = false;
		timeShiftHeld = System.currentTimeMillis() - shitHoldTimeStart;
		act(InputType.SHIFT_HOLD);
	}
}
