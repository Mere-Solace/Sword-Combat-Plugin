package btm.sword.system.entity;

import btm.sword.Sword;
import btm.sword.system.action.utility.thrown.ThrowAction;
import btm.sword.system.input.InputAction;
import btm.sword.system.entity.aspect.AspectType;
import btm.sword.system.input.InputExecutionTree;
import btm.sword.system.input.InputType;
import btm.sword.system.inventory.Menu;
import btm.sword.system.inventory.MenuActions;
import btm.sword.system.inventory.selection.CustomActionButton;
import btm.sword.system.item.ItemStackBuilder;
import btm.sword.system.item.KeyCache;
import btm.sword.system.playerdata.PlayerData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;

public class SwordPlayer extends Combatant {
	private final Player player;
	
	private final Menu mainMenu;
	private static final String menuHotKeyId = "menu_hotkey";
	
	private final InputExecutionTree inputExecutionTree;
	private final long inputTimeoutMillis = 1200L;
	
	private boolean performedDropAction;
	
	private BukkitTask rightTask;
	private boolean holdingRight;
	private long rightHoldTimeStart;
	private long timeRightHeld;
	private ItemStack mainItemStackAtTimeOfHold;
	private ItemStack offItemStackAtTimeOfHold;
	private int indexOfRightHold;
	private boolean mainHandRightHold;
	
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
		
		setItemAtIndex(new ItemStackBuilder(Material.SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE)
				.name(Component.text("+}- ", TextColor.color(72, 72, 72))
						.append(Component.text("Menu", TextColor.color(30, 150, 180)))
						.append(Component.text(" -{+", TextColor.color(72, 72, 72))))
				.tag(KeyCache.buttonTag, menuHotKeyId)
				.hideAll()
				.build(), 8);
		
		mainMenu = new Menu(Component.text("+}- ", TextColor.color(72, 72, 72))
				.append(Component.text("Menu", TextColor.color(30, 150, 180)))
				.append(Component.text(" -{+", TextColor.color(72, 72, 72))),
				45);
		
		mainMenu.setButton(22, new CustomActionButton(Material.SHIELD, Component.text("Spawn Display"), null, MenuActions::spawnDisplay));
		
		inputExecutionTree = new InputExecutionTree(inputTimeoutMillis);
		inputExecutionTree.initializeInputTree();
		
		performedDropAction = false;
		
		holdingRight = false;
		rightHoldTimeStart = 0L;
		timeRightHeld = 0L;
		mainHandRightHold = false;
		
		sneaking = false;
		sneakHoldTimeStart = 0L;
		timeSneakHeld = 0L;
		
		thrownItemIndex = -1;
		
		swappingInInv = false;
		droppingInInv = false;
	}
	
	@Override
	public void onSpawn() {
		super.onSpawn();
	}
	
	@Override
	public void onDeath() {
	
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
			if (rightTask == null)
				startHoldingRight();
			else
				return;
		}
		else if (input == InputType.SHIFT) {
			if (sneakTask == null)
				startSneaking();
			else
				return;
		}
		
		if (input == InputType.RIGHT_TAP || input == InputType.SHIFT_TAP) {
			if (!inputExecutionTree.nextExists(input)) return;
		}
		
		if (input == InputType.RIGHT_HOLD) {
			long minTime = inputExecutionTree.getMinHoldLengthOfNext(input);
			if (minTime == -1 || timeRightHeld < minTime) {
				if (isAttemptingThrow()) ThrowAction.throwCancel(this);
				return;
			}
		}
		else if (input == InputType.SHIFT_HOLD) {
			long minTime = inputExecutionTree.getMinHoldLengthOfNext(input);
			if (minTime == -1 || timeSneakHeld < minTime) {
				return;
			}
		}
		
		InputExecutionTree.InputNode node = inputExecutionTree.step(input);
		
		if (node == null)
			return;
		else if (node.shouldDisplay())
			displayInputSequence();

		InputAction action = node.getAction();
		
		if (action != null) {
			if (!action.execute(this)) {
				resetTree();
			}
		}
	}
	
	// method used to catch input before it's sent to act(), and before the action is cancelled.
	// true -> the action gets cancelled
	// false -> the input passes and gets handled by act()
	public boolean evaluateItemInput(ItemStack itemStack, InputType inputType) {
		Material type = itemStack.getType();
		ItemMeta meta = itemStack.getItemMeta();
		String id = meta != null ? meta.getPersistentDataContainer().get(KeyCache.buttonTagKey, PersistentDataType.STRING) : null;
		switch (inputType) {
			case RIGHT -> {
				if (type.isEdible() || type == Material.SHIELD || type == Material.BOW || type == Material.CROSSBOW) {
					return true;
				}
				else if (id != null && id.equals(menuHotKeyId)) {
					mainMenu.display(player);
					return true;
				}
			}
			case DROP -> {
				if (id != null && id.equals(menuHotKeyId)) {
					mainMenu.display(player);
					return true;
				}
			}
			default -> {
				return false;
			}
		}
		return false;
	}
	
	public boolean handleInventoryInput(InventoryClickEvent e) {
		Inventory inv = e.getInventory();
		ClickType clickType = e.getClick();
		InventoryAction action = e.getAction();
		ItemStack onCursor = e.getCursor();
		ItemStack clicked = e.getCurrentItem();
		int slotNumber = e.getSlot();
		
		message("\n\n~|------Beginning of new inventory interact event------|~"
				+ "\n       Inventory: " + inv.getType()
				+ "\n       Click type: " + clickType
				+ "\n       Action type: " + action
				+ "\n       Item on cursor: " + onCursor
				+ "\n       Current Item in slot: " + clicked
				+ "\n       slot number: " + slotNumber);
		
		if (mainMenu.equateInv(inv)) {
			mainMenu.handleClick(e);
			return true;
		}
		return false;
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
	
	public void startHoldingRight() {
		message("   Starting to hold right click!");
		if (holdingRight) return;
		
		if (rightTask != null && !rightTask.isCancelled()) rightTask.cancel();
		
		holdingRight = true;
		rightHoldTimeStart = System.currentTimeMillis();
		
		mainItemStackAtTimeOfHold = getItemStackInHand(true);
		offItemStackAtTimeOfHold = getItemStackInHand(false);
		
		mainHandRightHold = !mainItemStackAtTimeOfHold.isEmpty();
		
		indexOfRightHold = getCurrentInvIndex();
		
		setItemStackInHand(new ItemStack(Material.GUNPOWDER), true);
		
		rightTask = new BukkitRunnable() {
			@Override
			public void run() {
				inputExecutionTree.restartTimeoutTimer();
				if (!player.isHandRaised() && !player.isBlocking()) { // player must ALWAYS be holding a shield in offhand, then... I can work with this though
					endHoldingRight();
				}
				if (!holdingRight) {
					if (timeRightHeld < 162)
						act(InputType.RIGHT_TAP);
					else
						act(InputType.RIGHT_HOLD);
					resetHoldingRight();
					cancel();
				}
			}
		}.runTaskTimer(Sword.getInstance(), 2L, 1L);
	}
	
	public void resetHoldingRight() {
		rightTask = null;
		holdingRight = false;
		rightHoldTimeStart = 0L;
		timeRightHeld = 0L;
	}
	
	public void endHoldingRight() {
		holdingRight = false;
		timeRightHeld = System.currentTimeMillis() - rightHoldTimeStart;
		setItemStackInHand(offItemStackAtTimeOfHold, false);
		setItemAtIndex(mainItemStackAtTimeOfHold, indexOfRightHold);
	}
	
	public ItemStack getMainItemStackAtTimeOfHold() {
		return mainItemStackAtTimeOfHold;
	}
	
	public ItemStack getOffItemStackAtTimeOfHold() {
		return offItemStackAtTimeOfHold;
	}
	
	public boolean isMainHandRightHold() {
		return mainHandRightHold;
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
					if (timeSneakHeld < 162)
						act(InputType.SHIFT_TAP);
					else
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
