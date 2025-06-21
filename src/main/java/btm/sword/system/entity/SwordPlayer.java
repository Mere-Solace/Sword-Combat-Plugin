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
	
	BukkitTask rightHoldCheckTask;
	boolean holdingRight;
	long rightHoldTimeStart;
	long lastHoldTimeRecorded;
	long timeRightHeld;
	
	BukkitTask sneakTask;
	boolean sneaking;
	long sneakHoldTimeStart;
	long timeSneakHeld;
	
	public SwordPlayer(LivingEntity associatedEntity, PlayerData data) {
		super(associatedEntity, data.getCombatProfile());
		inputExecutionTree = new InputExecutionTree(inputTimeoutMillis);
		inputExecutionTree.initializeInputTree();
		
		rightHoldCheckTask = null;
		holdingRight = false;
		rightHoldTimeStart = 0L;
		lastHoldTimeRecorded = 0L;
		timeRightHeld = 0L;
		
		sneaking = false;
		sneakHoldTimeStart = 0L;
		timeSneakHeld = 0L;
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
			updateLastRightHoldTime();
			if (rightHoldCheckTask == null) {
				startRightHoldCheck();
			}
			else {
				inputExecutionTree.restartTimeoutTimer();
				return;
			}
		}
		else if (input == InputType.SHIFT) {
			if (sneakTask == null)
				startSneaking();
		}
		
		if (input == InputType.RIGHT_HOLD || input == InputType.SHIFT_HOLD) {
			long minTime = inputExecutionTree.getMinHoldLengthOfNext(input);
			message("Min Hold Time: " + minTime + ", Right held for " + timeRightHeld + ", Shift held for: " + timeSneakHeld);
			if (minTime == -1
					|| (input == InputType.RIGHT_HOLD && timeRightHeld < minTime)
					|| (input == InputType.SHIFT_HOLD && timeSneakHeld < minTime)) {
				message("  not letting you send input to the tree.");
				return;
			}
		}
		
		InputExecutionTree.InputNode node = inputExecutionTree.step(input);
		
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
		if (holdingRight) return;
		message("Pressed Right");
		if (rightHoldCheckTask != null) rightHoldCheckTask.cancel();
		
		holdingRight = true;
		rightHoldTimeStart = System.currentTimeMillis();
		
		rightHoldCheckTask = new BukkitRunnable() {
			@Override
			public void run() {
				long curTime = System.currentTimeMillis();
				if (curTime - lastHoldTimeRecorded > 200L) {
					onStopRightHold();
					message("You stopped holding Right (held for " + ((double)(timeRightHeld)/1000) + " s)");
					resetHoldingRight();
					cancel();
				}
			}
		}.runTaskTimer(Sword.getInstance(), 0, 4L);
	}
	
	public void updateLastRightHoldTime() {
		lastHoldTimeRecorded = System.currentTimeMillis();
	}
	
	public void resetHoldingRight() {
		rightHoldCheckTask = null;
		holdingRight = false;
		rightHoldTimeStart = 0L;
		lastHoldTimeRecorded = 0L;
		timeRightHeld = 0L;
	}
	
	public void onStopRightHold() {
		timeRightHeld = System.currentTimeMillis() - rightHoldTimeStart;
		act(InputType.RIGHT_HOLD);
	}
	
	public void startSneaking() {
		if (sneaking) return;
		message("Pressed Sneak");
		if (sneakTask != null) sneakTask.cancel();
		
		sneaking = true;
		sneakHoldTimeStart = System.currentTimeMillis();
		
		sneakTask = new BukkitRunnable() {
			@Override
			public void run() {
				inputExecutionTree.restartTimeoutTimer();
				if (!sneaking) {
					act(InputType.SHIFT_HOLD);
					resetSneaking();
					cancel();
				}
			}
		}.runTaskTimer(Sword.getInstance(), 0L, 1L);
	}
	
	public void resetSneaking() {
		sneakTask = null;
		sneaking = false;
		sneakHoldTimeStart = 0L;
		timeSneakHeld = 0L;
	}
	
	public void endSneaking() {
		sneaking = false;
		timeSneakHeld = System.currentTimeMillis() - sneakHoldTimeStart;
	}
}
