package btm.sword.system.input;

import btm.sword.Sword;
import btm.sword.system.action.AttackAction;
import btm.sword.system.action.MovementAction;
import btm.sword.system.action.UtilityAction;
import btm.sword.system.entity.Combatant;
import btm.sword.system.entity.SwordPlayer;
import btm.sword.system.playerdata.StatType;
import btm.sword.util.SoundUtils;
import btm.sword.util.sound.SoundType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.List;

public class InputExecutionTree {
	private final BukkitScheduler s = Bukkit.getScheduler();
	private final Plugin plugin = Sword.getInstance();
	private final InputNode root = new InputNode(null);
	
	private InputNode currentNode = root;
	private StringBuilder sequenceToDisplay = new StringBuilder();
	private BukkitTask timeoutTimer = null;
	private final long timeoutTicks;
	
	public InputExecutionTree(long timeoutMillis) {
		this.timeoutTicks = (long) (timeoutMillis * (0.02)); // 1/50 (or 0.02) is the conversion from milliseconds to ticks
	}
	
	public void takeInput(InputType input, Material itemUsed, SwordPlayer executor) {
		executor.entity().sendMessage("  InputExecutionTree is taking input.");
		if (timeoutTimer != null) timeoutTimer.cancel();
		
		if (currentNode == null) {
			executor.entity().sendMessage("    current was null");
			return;
		}
		
		InputNode next = currentNode.getChild(input);
		
		if (next == null) {
			executor.entity().sendMessage("    No next node for that input");
			if (currentNode != root) {
				sequenceToDisplay.append("~");
				executor.displayInputSequence();
				reset();
				SoundUtils.playSound(executor.entity(), SoundType.BLOCK_GRINDSTONE_USE, 0.6f, 1f);
			}
			else {
				executor.entity().sendMessage("    No Input down that way at all. you're at the root");
			}
			return;
		}
		
		currentNode = next;
		sequenceToDisplay.append(inputToString(input));
		
		if (itemUsed != executor.getItemLastUsed() && currentNode.isSameItemRequired()) {
			executor.entity().sendMessage("    You swapped weapons in the middle of a sequence that requires the same weapon.\n\t Taking input from root instead");
			executor.displayMistake();
			reset();
			executor.setItemLastUsed(itemUsed);
			takeInput(input, itemUsed, executor);
			return;
		}
		
		boolean noChildren = noChildren();
		
		if (!noChildren) {
			sequenceToDisplay.append(" + ");
			timeoutTimer = new BukkitRunnable() {
				@Override
				public void run() {
					reset();
				}
			}.runTaskLater(plugin, timeoutTicks);
		}
		
		if (inActionState()) {
			executor.entity().sendMessage("    Executing the runnable now");
			currentNode.action.execute(executor, s, plugin);
		}
		
		executor.displayInputSequence();
		
		if (noChildren) {
			executor.entity().sendMessage("         resetting cuz no children.");
			reset();
		}
	}
	
	public void reset() {
		currentNode = root;
		sequenceToDisplay = new StringBuilder();
	}
	
	public boolean atRoot() {
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
	
	public boolean noChildren() {
		return currentNode.children.isEmpty();
	}
	
	public boolean inActionState() {
		return currentNode.action != null;
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
				new InputAction("Forward Dash",
						MovementAction.dash(swordPlayer, true),
						executor -> executor.calcCooldown(200L, 1000L, StatType.CELERITY, 10),
						Combatant::cannotPerformAnyAction), false);
		
		add(List.of(InputType.SHIFT, InputType.DROP),
				new InputAction("Backward Dash",
						MovementAction.dash(swordPlayer, false),
						executor -> executor.calcCooldown(200L, 1000L, StatType.CELERITY, 10),
						Combatant::cannotPerformAnyAction), false);
		
		// grab
		add(List.of(InputType.SHIFT, InputType.RIGHT),
				new InputAction("Grab",
						UtilityAction.grab(swordPlayer),
						executor -> executor.calcCooldown(200L, 1000L, StatType.FORTITUDE, 10),
						Combatant::cannotPerformAnyAction), false);
		
			// Item dependent actions:
		// basic attacks
		add(List.of(InputType.LEFT),
				new InputAction("Basic 0",
						AttackAction.basic(swordPlayer, 0),
						executor -> executor.calcCooldown(50L, 1100L, StatType.FINESSE, 10),
						Combatant::cannotPerformAnyAction), true);
		
		add(List.of(InputType.LEFT, InputType.LEFT),
				new InputAction("Basic 1",
						AttackAction.basic(swordPlayer, 1),
						executor -> executor.calcCooldown(0L, 0L, StatType.FINESSE, 10),
						Combatant::cannotPerformAnyAction), true);
		
		add(List.of(InputType.LEFT, InputType.LEFT, InputType.LEFT),
				new InputAction("Basic 2",
						AttackAction.basic(swordPlayer, 2),
						executor -> executor.calcCooldown(0L, 0L, StatType.FINESSE, 10),
						Combatant::cannotPerformAnyAction), true);
		
		// heavy attacks
		add(List.of(InputType.LEFT, InputType.RIGHT),
				new InputAction("Heavy 0",
						AttackAction.heavy(swordPlayer, 0),
						executor -> executor.calcCooldown(400L, 1000L, StatType.MIGHT, 10),
						Combatant::cannotPerformAnyAction), true);
		
		add(List.of(InputType.LEFT, InputType.LEFT,InputType.RIGHT),
				new InputAction("Heavy 1",
						AttackAction.heavy(swordPlayer, 1),
						executor -> executor.calcCooldown(400L, 1000L, StatType.MIGHT, 10),
						Combatant::cannotPerformAnyAction), true);
		
		// side step attacks
		add(List.of(InputType.SWAP, InputType.RIGHT),
				new InputAction("Right Side Step Attack",
						AttackAction.sideStep(swordPlayer, true),
						executor -> executor.calcCooldown(300L, 600L, StatType.CELERITY, 10),
						Combatant::cannotPerformAnyAction), true);
		
		add(List.of(InputType.SWAP, InputType.LEFT),
				new InputAction("Left Side Step Attack",
						AttackAction.sideStep(swordPlayer, false),
						executor -> executor.calcCooldown(300L, 600L, StatType.CELERITY, 10),
						Combatant::cannotPerformAnyAction), true);
		
		// skills
		add(List.of(InputType.DROP, InputType.RIGHT, InputType.SHIFT), null, true);
		
		add(List.of(InputType.DROP, InputType.RIGHT, InputType.DROP), null, true);
		
		add(List.of(InputType.DROP, InputType.RIGHT, InputType.LEFT), null, true);
	}
	
	private static class InputNode {
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
