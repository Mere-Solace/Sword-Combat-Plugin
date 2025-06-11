package btm.sword.system.input;

import btm.sword.Sword;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.List;

public class InputExecutionTree {
	private final InputNode root = new InputNode(null);
	private InputNode currentNode = root;
	private StringBuilder sequenceToDisplay = new StringBuilder();
	private BukkitTask timeoutTimer = null;
	
	private final BukkitScheduler s = Bukkit.getScheduler();
	private final Plugin plugin = Sword.getInstance();
	
	public InputExecutionTree() { }
	
	public boolean takeInput(InputType input, Material itemUsed, Material itemInUse) {
		if (timeoutTimer != null) timeoutTimer.cancel();
		
		if (currentNode.noChild(input)) {
			reset();
			return false;
		}
		
		if (currentNode.getChild(input).sameItemRequired && itemUsed != itemInUse) {
			reset();
		}
		
		currentNode = currentNode.getChild(input);
		
		if (inActionState()) performAction();
		
		sequenceToDisplay.append(inputToString(input));
		if (!noChildren()) {
			sequenceToDisplay.append(" + ");
			
			timeoutTimer = new BukkitRunnable() {
				@Override
				public void run() {
					reset();
				}
			}.runTaskLater(plugin, 10);
		}
		
		return true;
	}
	
	public void reset() {
		currentNode = root;
		sequenceToDisplay = new StringBuilder();
	}
	
	public void add(List<InputType> inputSequence, Runnable action, boolean sameItemRequired) {
		InputNode dummy = root;
		for (InputType input : inputSequence) {
			if (dummy.noChild(input)) {
				dummy.addChild(input, null);
				dummy.setSameItemRequired(sameItemRequired);
			}
			dummy = dummy.getChild(input);
		}
		dummy.setAction(action);
	}
	
	public boolean noChildren() {
		return currentNode.children.isEmpty();
	}
	
	public boolean inActionState() {
		return currentNode.action != null;
	}
	
	public void performAction() {
		s.runTask(plugin, currentNode.getAction());
	}
	
	public static class InputNode {
		private final HashMap<InputType, InputNode> children = new HashMap<>();
		private Runnable action;
		private boolean sameItemRequired = false;
		
		public InputNode(Runnable action) {
			this.action = action;
		}
		
		public void addChild(InputType input, Runnable action) {
			children.put(input, new InputNode(action));
		}
		
		public boolean noChild(InputType input) {
			return !children.containsKey(input);
		}
		
		public InputNode getChild(InputType input) {
			return children.get(input);
		}
		
		public Runnable getAction() {
			return action;
		}
		
		public void setAction(Runnable action) {
			this.action = action;
		}
		
		public void setSameItemRequired(boolean sameItemRequired) {
			this.sameItemRequired = sameItemRequired;
		}
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
}
