package btm.sword.system.input;

import btm.sword.Sword;
import btm.sword.system.action.AttackAction;
import btm.sword.system.action.MovementAction;
import btm.sword.system.action.UtilityAction;
import btm.sword.system.entity.Combatant;
import btm.sword.system.entity.SwordPlayer;
import btm.sword.system.playerdata.StatType;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.List;

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
		if (timeoutTimer != null) timeoutTimer.cancel();
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
			reset();
			return null;
		}
		
		// set the
		currentNode = next;
		
		sequenceToDisplay.append(inputToString(input));
		
		if (hasChildren()) {
			sequenceToDisplay.append(" + ");
			timeoutTimer = new BukkitRunnable() {
				@Override
				public void run() {
					reset();
				}
			}.runTaskLater(plugin, timeoutTicks);
		}
		
		return next;
	}
	
	public void reset() {
		currentNode = root;
		sequenceToDisplay = new StringBuilder();
	}
	
	public boolean isRoot() {
		return currentNode == root;
	}
	
	public void add(List<InputType> inputSequence, InputAction action, boolean sameItemRequired) {
		InputNode dummy = root;
		for (InputType input : inputSequence) {
			if (dummy.noChild(input)) {
				dummy.addChild(input, null);
			}
			dummy = dummy.getChild(input);
		}
		dummy.setAction(action);
		dummy.setSameItemRequired(sameItemRequired);
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
			case SWAP -> out = "W";
			default -> out = "";
		}
		return out;
	}
	
	public void initializeInputTree(SwordPlayer swordPlayer) {
			// Item independent actions:
		// dodge forward, dodge backward
		add(List.of(InputType.DROP, InputType.DROP),
				new InputAction(
						MovementAction.dash(swordPlayer, true),
						executor -> executor.calcCooldown(200L, 1000L, StatType.CELERITY, 10),
						Combatant::cannotPerformAnyAction), false);
		
		add(List.of(InputType.SHIFT, InputType.DROP),
				new InputAction(
						MovementAction.dash(swordPlayer, false),
						executor -> executor.calcCooldown(200L, 1000L, StatType.CELERITY, 10),
						Combatant::cannotPerformAnyAction), false);
		
		// grab
		add(List.of(InputType.SHIFT, InputType.RIGHT),
				new InputAction(
						UtilityAction.grab(swordPlayer),
						executor -> executor.calcCooldown(200L, 1000L, StatType.FORTITUDE, 10),
						Combatant::cannotPerformAnyAction), false);
		
			// Item dependent actions:
		// basic attacks
		add(List.of(InputType.LEFT),
				new InputAction(
						AttackAction.basic(swordPlayer, 0),
						executor -> executor.calcCooldown(50L, 1100L, StatType.FINESSE, 10),
						Combatant::cannotPerformAnyAction), true);
		
		add(List.of(InputType.LEFT, InputType.LEFT),
				new InputAction(
						AttackAction.basic(swordPlayer, 1),
						executor -> executor.calcCooldown(0L, 0L, StatType.FINESSE, 10),
						Combatant::cannotPerformAnyAction), true);
		
		add(List.of(InputType.LEFT, InputType.LEFT, InputType.LEFT),
				new InputAction(
						AttackAction.basic(swordPlayer, 2),
						executor -> executor.calcCooldown(0L, 0L, StatType.FINESSE, 10),
						Combatant::cannotPerformAnyAction), true);
		
		// heavy attacks
		add(List.of(InputType.LEFT, InputType.RIGHT),
				new InputAction(
						AttackAction.heavy(swordPlayer, 0),
						executor -> executor.calcCooldown(400L, 1000L, StatType.MIGHT, 10),
						Combatant::cannotPerformAnyAction), true);
		
		add(List.of(InputType.LEFT, InputType.LEFT,InputType.RIGHT),
				new InputAction(
						AttackAction.heavy(swordPlayer, 1),
						executor -> executor.calcCooldown(400L, 1000L, StatType.MIGHT, 10),
						Combatant::cannotPerformAnyAction), true);
		
		// side step attacks
		add(List.of(InputType.SWAP, InputType.RIGHT),
				new InputAction(
						AttackAction.sideStep(swordPlayer, true),
						executor -> executor.calcCooldown(300L, 600L, StatType.CELERITY, 10),
						Combatant::cannotPerformAnyAction), true);
		
		add(List.of(InputType.SWAP, InputType.LEFT),
				new InputAction(
						AttackAction.sideStep(swordPlayer, false),
						executor -> executor.calcCooldown(300L, 600L, StatType.CELERITY, 10),
						Combatant::cannotPerformAnyAction), true);
		
		// skills
		add(List.of(InputType.DROP, InputType.RIGHT, InputType.SHIFT), null, true);
		
		add(List.of(InputType.DROP, InputType.RIGHT, InputType.DROP), null, true);
		
		add(List.of(InputType.DROP, InputType.RIGHT, InputType.LEFT), null, true);
	}
	
	public static class InputNode {
		private InputAction action;
		private final HashMap<InputType, InputNode> children = new HashMap<>();
		private boolean sameItemRequired;
		
		public InputNode(InputAction action) {
			this.action = action;
		}
		
		public void addChild(InputType input, InputAction action) {
			children.put(input, new InputNode(action));
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
		
		public void setSameItemRequired(boolean sameItemRequired) {
			this.sameItemRequired = sameItemRequired;
		}
		
		public boolean isSameItemRequired() {
			return sameItemRequired;
		}
	}
}
