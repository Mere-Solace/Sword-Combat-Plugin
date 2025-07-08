package btm.sword.system.entity;

import btm.sword.Sword;
import btm.sword.system.action.utility.thrown.ThrowAction;
import btm.sword.system.input.InputAction;
import btm.sword.system.entity.aspect.AspectType;
import btm.sword.system.input.InputExecutionTree;
import btm.sword.system.input.InputType;
import btm.sword.system.playerdata.PlayerData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;

public class SwordPlayer extends Combatant {
	private final Player player;
	
	private final InputExecutionTree inputExecutionTree;
	private final long inputTimeoutMillis = 1200L;
	
	private boolean performedDropAction;
	private boolean canDrop;
	
	private long holdTimeoutLength;
	
	private BukkitTask rightHoldCheckTask;
	private ItemStack heldItemStack;
	private boolean mainHandForRight;
	private int rightHoldIndex;
	private boolean holdingRight;
	private long rightHoldTimeStart;
	private long lastHoldTimeRecorded;
	private long timeRightHeld;
	
	private BukkitTask sneakTask;
	private boolean sneaking;
	private long sneakHoldTimeStart;
	private long timeSneakHeld;
	
	private int thrownItemIndex;
	
	private boolean swappingInInv;
	private boolean droppingInInv;
	
	public SwordPlayer(LivingEntity associatedEntity, PlayerData data) {
		super(associatedEntity, data.getCombatProfile());
		player = (Player) self;
		
		inputExecutionTree = new InputExecutionTree(inputTimeoutMillis);
		inputExecutionTree.initializeInputTree();
		
		performedDropAction = false;
		canDrop = false;
		
		holdTimeoutLength = 75L;
		
		rightHoldCheckTask = null;
		holdingRight = false;
		rightHoldTimeStart = 0L;
		lastHoldTimeRecorded = 0L;
		timeRightHeld = 0L;
		
		sneaking = false;
		sneakHoldTimeStart = 0L;
		timeSneakHeld = 0L;
		
		thrownItemIndex = -1;
		
		swappingInInv = false;
		droppingInInv = false;
	}
	
	public void act(InputType input) {
		if (isAttemptingThrow()) {
			if (input != InputType.RIGHT && input != InputType.RIGHT_HOLD) {
				ThrowAction.throwCancel(this);
				resetTree();
				return;
			}
		}
		
		if (input == InputType.SWAP && isGrabbing()) {
			setGrabbing(false);
			return;
		}
		
		if (input == InputType.LEFT && isGrabbing()) {
			onGrabHit();
			return;
		}
		
		if (getAbilityCastTask() != null) {
			return;
		}
		
		if (input == InputType.RIGHT) {
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
			if (minTime == -1
					|| (input == InputType.RIGHT_HOLD && timeRightHeld < minTime)
					|| (input == InputType.SHIFT_HOLD && timeSneakHeld < minTime)) {
				if (minTime == -1) message("No hold input action");
				else message("Didn't hold for long enough");
				
				if (isAttemptingThrow()) ThrowAction.throwCancel(this);
				
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
			if (!action.execute(this)) {
				resetTree();
			}
		}
	}
	
	public Player player() {
		return player;
	}
	
	public boolean hasPerformedDropAction() {
		return performedDropAction;
	}
	
	public void setPerformedDropAction(boolean performedDropAction) {
		this.performedDropAction = performedDropAction;
	}
	
	public long getHoldTimeoutLength() {
		return holdTimeoutLength;
	}
	
	public void setHoldTimeoutLength(long holdTimeoutLength) {
		this.holdTimeoutLength = holdTimeoutLength;
	}
	
	public boolean isMainHandForRight() {
		return mainHandForRight;
	}
	
	public void setMainHandForRight(boolean mainHandForRight) {
		this.mainHandForRight = mainHandForRight;
	}
	
	public void resetTree() {
		inputExecutionTree.reset();
	}
	
	public boolean isAtRoot() {
		return inputExecutionTree.isAtRoot();
	}
	
	public void displayInputSequence() {
		self.showTitle(Title.title(
				Component.text(""),
				Component.text(inputExecutionTree.toString(), NamedTextColor.DARK_RED, TextDecoration.ITALIC),
				Title.Times.times(
						Duration.ofMillis(0),
						Duration.ofMillis(inputTimeoutMillis),
						Duration.ofMillis(100))));
	}
	
	public void displayMistake() {
		self.showTitle(Title.title(
				Component.text(""),
				Component.text("~*#*~", NamedTextColor.DARK_GRAY, TextDecoration.ITALIC),
				Title.Times.times(
						Duration.ofMillis(0),
						Duration.ofMillis(inputTimeoutMillis),
						Duration.ofMillis(100))));
	}
	
	public void displayDisablingEffect() {
		self.showTitle(Title.title(
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
		self.showTitle(Title.title(
				Component.text(""),
				Component.text("on cooldown: " + timeToDisplay + " " + unit, NamedTextColor.GRAY, TextDecoration.ITALIC),
				Title.Times.times(
						Duration.ofMillis(0),
						Duration.ofMillis(inputTimeoutMillis),
						Duration.ofMillis(100))));
	}
	
	public void displayTitle(Component title, Component subtitle, long fadein, long duration, long fadeout) {
		self.showTitle(Title.title(
				title,
				subtitle,
				Title.Times.times(
						Duration.ofMillis(fadein),
						Duration.ofMillis(duration),
						Duration.ofMillis(fadeout))));
	}
	
	public void itemNameDisplay(String toDisplay, TextColor color, TextDecoration style) {
		ItemStack stack = getItemStackInHand(true).clone();
		if (stack.isEmpty() || stack.getType().isAir()) stack = new ItemStack(Material.GUNPOWDER);
		ItemMeta metaData = stack.getItemMeta();
		if (metaData == null) {
			message("MetaData for item to display is null!");
			return;
		}
		if (style == null)
			metaData.itemName(Component.text(toDisplay, color));
		else
			metaData.itemName(Component.text(toDisplay, color, style));
		
		stack.setItemMeta(metaData);
		player.sendEquipmentChange(self, EquipmentSlot.HAND, stack);
	}
	
	public void addStat(AspectType stat, int amount) {
		aspects.getAspect(stat).addBaseValue(amount);
		// invalidate all cached, calculated values with that stat
	}
	
	public boolean inputReliantOnItem() {
		return inputExecutionTree.requiresSameItem();
	}
	
	public void startRightHoldCheck() {
		if (holdingRight) return;
		
		if (rightHoldCheckTask != null && !rightHoldCheckTask.isCancelled()) rightHoldCheckTask.cancel();
		
		mainHandForRight = hasItemInMainHand();
		heldItemStack = getItemStackInHand(mainHandForRight);
		if (mainHandForRight)
			rightHoldIndex = getCurrentInvIndex();
		else
			rightHoldIndex = -1;
		
		setItemTypeInHand(Material.BREAD, mainHandForRight);
		
		holdingRight = true;
		rightHoldTimeStart = System.currentTimeMillis();
		
		rightHoldCheckTask = new BukkitRunnable() {
			@Override
			public void run() {
				if (!player.getActiveItem().isEmpty()) {
					message("   ~ lad! ye be usin' an active item! ~");
					updateLastRightHoldTime();
				}
				
				long curTime = System.currentTimeMillis();
				if (curTime - lastHoldTimeRecorded > 62L) { // don't allow min hold times to be less than ~ 112 ms!
					onStopRightHold();
					resetHoldingRight();
					cancel();
				}
			}
		}.runTaskTimer(Sword.getInstance(), 0, 1L);
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
		message("       * held right for: " + timeRightHeld + " ms *");
		if (mainHandForRight)
			setItemAtIndex(heldItemStack, rightHoldIndex);
		else
			setItemStackInHand(heldItemStack, false);
		act(InputType.RIGHT_HOLD);
	}
	
	public void startSneaking() {
		if (sneaking) return;
		
		if (sneakTask != null && !sneakTask.isCancelled()) sneakTask.cancel();
		
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
	
	@Override
	public void onSpawn() {
		super.onSpawn();
	}
	
	@Override
	public void onDeath() {
	
	}
	
	public int getThrownItemIndex() {
		return thrownItemIndex;
	}
	
	public void setThrownItemIndex() {
		thrownItemIndex = getCurrentInvIndex();
	}
	
	public int getCurrentInvIndex() {
		return player.getInventory().getHeldItemSlot();
	}
	
	public void setItemAtIndex(ItemStack item, int index) {
		player.getInventory().setItem(index, item);
	}
	
	public boolean isSwappingInInv() {
		return swappingInInv;
	}
	
	public void setSwappingInInv() {
		swappingInInv = true;
		new BukkitRunnable() {
			@Override
			public void run() {
				swappingInInv = false;
			}
		}.runTaskLater(Sword.getInstance(), 1L);
	}
	
	public boolean isDroppingInInv() {
		return droppingInInv;
	}
	
	public void setDroppingInInv() {
		droppingInInv = true;
		new BukkitRunnable() {
			@Override
			public void run() {
				droppingInInv = false;
			}
		}.runTaskLater(Sword.getInstance(), 1L);
	}
}
