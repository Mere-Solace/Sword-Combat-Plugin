package btm.sword.system.input;

import btm.sword.Sword;
import btm.sword.system.action.AttackAction;
import btm.sword.system.action.MovementAction;
import btm.sword.system.action.UtilityAction;
import btm.sword.system.action.type.AttackType;
import btm.sword.system.entity.Combatant;
import btm.sword.system.entity.SwordPlayer;
import btm.sword.system.entity.aspect.AspectType;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;

public class InputExecutionTree {
	private static final Plugin plugin = Sword.getInstance();
	
	private static final InputNode root = new InputNode(null);
	
	private InputNode currentNode;
	private StringBuilder sequenceToDisplay;
	private BukkitTask timeoutTimer;
	private final long timeoutTicks;
	
	public InputExecutionTree(long timeoutMillis) {
		currentNode = root;
		sequenceToDisplay = new StringBuilder();
		timeoutTimer = null;
		this.timeoutTicks = (long) (timeoutMillis * (0.02)); // 1/50 (or 0.02) is the conversion from milliseconds to ticks
	}
	
	public InputNode step(InputType input) {
		stopTimeoutTimer();
		// before taking input, if it is known that the current node is a leaf, reset and take input from the root
		if (!hasChildren()) reset();
		
		// shouldn't happen often
		if (currentNode == null) {
			reset();
			return null;
		}
		
		// initialize a new node that points to the traversal of the input
		InputNode next = currentNode.getChild(input);
		
		if (next == null) {
			if (isAtRoot()) return null;
			else {
				reset();
				if (currentNode.isCancellable())
					return step(input);
				
				else
					return null;
			}
		}
		
		sequenceToDisplay.append(inputToString(input));
		
		// set the
		currentNode = next;
		
		if (hasChildren()) {
			sequenceToDisplay.append(" + ");
			startTimeoutTimer();
		}
		
		return next;
	}
	
	private void startTimeoutTimer() {
		timeoutTimer = new BukkitRunnable() {
			@Override
			public void run() {
				reset();
			}
		}.runTaskLater(plugin, timeoutTicks);
	}
	
	public void stopTimeoutTimer() {
		if (timeoutTimer != null && !timeoutTimer.isCancelled()) timeoutTimer.cancel();
	}
	
	public void restartTimeoutTimer() {
		stopTimeoutTimer();
		startTimeoutTimer();
	}
	
	
	
	public void reset() {
		currentNode = root;
		sequenceToDisplay = new StringBuilder();
	}
	
	public boolean isAtRoot() {
		return currentNode == root;
	}
	
	public void add(List<InputType> inputSequence, InputAction action,
	                boolean sameItemRequired,
	                boolean cancellable,
	                boolean display) {
		InputNode dummy = root;
		for (InputType input : inputSequence) {
			if (dummy.noChild(input)) {
				dummy.addChild(input, null);
				dummy.setSameItemRequired(sameItemRequired);
				dummy.setCancellable(cancellable);
				dummy.setDisplay(display);
			}
			dummy = dummy.getChild(input);
		}
		dummy.setAction(action);
		dummy.setDisplay(display);
	}
	
	public void add(List<InputType> inputSequence, InputAction action,
	                boolean sameItemRequired,
	                boolean cancellable,
	                boolean display,
	                long minHoldTime) {
		InputNode dummy = root;
		for (InputType input : inputSequence) {
			if (dummy.noChild(input)) {
				if (input == InputType.RIGHT_HOLD || input == InputType.SHIFT_HOLD) {
					dummy.addChild(input, null, minHoldTime);
				}
				else {
					dummy.addChild(input, null);
				}
				dummy.setSameItemRequired(sameItemRequired);
				dummy.setCancellable(cancellable);
				dummy.setDisplay(display);
			}
			dummy = dummy.getChild(input);
		}
		dummy.setAction(action);
		dummy.setDisplay(display);
	}
	
	public boolean hasChildren() {
		return !currentNode.children.isEmpty();
	}
	
	@Override
	public String toString() {
		return sequenceToDisplay.toString();
	}
	
	private String inputToString(InputType type) {
		String out;
		switch (type) {
			case LEFT -> out = "L";
			case RIGHT -> out = "R";
			case DROP -> out = "D";
			case SHIFT -> out = "S";
			case SWAP -> out = "~";
			case RIGHT_HOLD -> out = "_R_";
			case SHIFT_HOLD -> out = "_S_";
			default -> out = "";
		}
		return out;
	}
	
	public boolean requiresSameItem() {
		return currentNode.isSameItemRequired();
	}
	
	public long getMinHoldLengthOfNext(InputType holdType) {
		InputNode next = currentNode.getChild(holdType);
		if (next == null) return -1;
		return next.getMinHoldTime();
	}
	
	public void initializeInputTree() {
			// Item independent actions:
		// dodge forward, dodge backward
		add(List.of(InputType.SWAP, InputType.SWAP),
				new InputAction(
						executor -> MovementAction.dash(executor, true),
						executor -> executor.calcCooldown(AspectType.CELERITY, 200L, 1000L, 10),
						Combatant::canAirDash,
						false, true),
				false, true, true);

		add(List.of(InputType.SHIFT, InputType.SWAP),
				new InputAction(
						executor -> MovementAction.dash(executor, false),
						executor -> executor.calcCooldown(AspectType.CELERITY, 200L, 1000L, 10),
						Combatant::canAirDash,
						false, true),
				false, true, true);

		// grab
		add(List.of(InputType.SHIFT, InputType.LEFT),
				new InputAction(
						UtilityAction::grab,
						executor -> executor.calcCooldown(AspectType.FORTITUDE, 200L, 1000L, 10),
						Combatant::canPerformAction,
						false, true),
				false, false, true);

			// Item dependent actions:
		// basic attacks
		add(List.of(InputType.LEFT),
				new InputAction(
						executor -> AttackAction.basicAttack(executor, AttackType.BASIC_1),
						executor -> Math.max(0, (executor.getTimeOfLastAttack() + executor.getDurationOfLastAttack()) - System.currentTimeMillis()),
						Combatant::canPerformAction,
						true, true),
				true, true, false);
		
		add(List.of(InputType.LEFT, InputType.LEFT),
				new InputAction(
						executor -> AttackAction.basicAttack(executor, AttackType.BASIC_2),
						executor -> 0L,
						Combatant::canPerformAction,
						false, true),
				true, true, false);

		add(List.of(InputType.LEFT, InputType.LEFT, InputType.LEFT),
				new InputAction(
						executor -> AttackAction.basicAttack(executor, AttackType.BASIC_3),
						executor -> 0L,
						Combatant::canPerformAction,
						false, true),
				true, true, false);
		
		// throw hold action
		add(List.of(InputType.DROP, InputType.RIGHT),
				new InputAction(
						UtilityAction::throwReady,
						executor -> 0L,
						Combatant::canPerformAction,
						false, false),
				true, true, true);
		
		// throw
		add(List.of(InputType.DROP, InputType.RIGHT, InputType.RIGHT_HOLD),
				new InputAction(
						UtilityAction::throwItem,
						executor -> 0L,
						Combatant::canPerformAction,
						false, false),
				true, true, true, 600L);
		
		// skills
		add(List.of(InputType.SWAP, InputType.RIGHT, InputType.SHIFT),
				null,
				true, false, true);
		
		add(List.of(InputType.SWAP, InputType.RIGHT, InputType.DROP),
				null,
				true, false, true);
		
		add(List.of(InputType.SWAP, InputType.RIGHT, InputType.LEFT),
				null,
				true, false, true);
		
		add(List.of(InputType.RIGHT, InputType.RIGHT_HOLD),
				new InputAction(
						executor -> MovementAction.dash(executor, true),
						executor -> executor.calcCooldown(AspectType.CELERITY, 200L,1400L, 10),
						Combatant::canAirDash,
						true, true),
				true, true, true, 1000L);
		
		add(List.of(InputType.SHIFT, InputType.SHIFT_HOLD),
				new InputAction(
						UtilityAction::death,
						executor -> executor.calcCooldown(AspectType.CELERITY, 200L,1400L, 10),
						Combatant::canPerformAction,
						true, true),
				true, true, true, 500L);
		
		// Drop an item "ability"
		add(List.of(InputType.DROP, InputType.DROP, InputType.DROP),
				new InputAction(
						executor -> UtilityAction.allowDrop((SwordPlayer) executor),
						executor -> 0L,
						Combatant::canPerformAction,
						false, false),
				true, true, true);
	}
	
	public static class InputNode {
		private InputAction action;
		private final HashMap<InputType, InputNode> children = new HashMap<>();
		private boolean sameItemRequired;
		private boolean cancellable;
		private boolean display;
		private final long minHoldTime;
		
		public InputNode(InputAction action, long minHoldTime) {
			this.action = action;
			this.minHoldTime = minHoldTime;
		}
		
		public InputNode(InputAction action) {
			this(action, -1);
		}
		
		public void addChild(InputType input, InputAction action) {
			children.putIfAbsent(input, new InputNode(action));
		}
		
		public void addChild(InputType input, InputAction action, long minHoldLength) {
			children.putIfAbsent(input, new InputNode(action, minHoldLength));
		}
		
		public boolean noChild(InputType input) {
			return !children.containsKey(input);
		}
		
		public InputNode getChild(InputType input) {
			return children.get(input);
		}
		
		public InputAction getAction() {
			return action;
		}
		
		public void setAction(InputAction action) {
			this.action = action;
		}
		
		public boolean isSameItemRequired() {
			return sameItemRequired;
		}
		
		public void setSameItemRequired(boolean sameItemRequired) {
			this.sameItemRequired = sameItemRequired;
		}
		
		public boolean isCancellable() {
			return cancellable;
		}
		
		public void setCancellable(boolean cancellable) {
			this.cancellable = cancellable;
		}
		
		public boolean shouldDisplay() {
			return display;
		}
		
		public void setDisplay(boolean display) {
			this.display = display;
		}
		
		public long getMinHoldTime() {
			return minHoldTime;
		}
	}
}
