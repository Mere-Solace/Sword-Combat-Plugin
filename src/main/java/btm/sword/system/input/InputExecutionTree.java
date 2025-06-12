package btm.sword.system.input;

import btm.sword.Sword;
import btm.sword.system.entity.SwordPlayer;
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
	
	public boolean takeInput(InputType input, Material itemUsed, SwordPlayer executor) {
		if (timeoutTimer != null) timeoutTimer.cancel();
		
		if (currentNode.noChild(input)) {
			reset();
			return false;
		}
		
		if (currentNode.getChild(input).sameItemRequired && itemUsed != executor.getItemLastUsed()) {
			reset();
		}
		
		currentNode = currentNode.getChild(input);
		
		if (currentNode.getAction() != null &&
				(!currentNode.getAction().isUsable(executor)
				|| currentNode.getAction().calcCooldown(executor) >= System.currentTimeMillis() - currentNode.getTimeLastExecuted())) {
			executor.getAssociatedEntity().sendMessage("This action is on cooldown my lad!");
			reset();
			return false;
		}
		
		if (inActionState()) performAction();
		currentNode.setTimeLastExecuted(System.currentTimeMillis());
		
		sequenceToDisplay.append(inputToString(input));
		if (!noChildren()) {
			sequenceToDisplay.append(" + ");
			
			timeoutTimer = new BukkitRunnable() {
				@Override
				public void run() {
					reset();
				}
			}.runTaskLater(plugin, 12);
		}
		
		return true;
	}
	
	public void reset() {
		currentNode = root;
		sequenceToDisplay = new StringBuilder();
	}
	
	public void add(List<InputType> inputSequence, InputAction action, boolean sameItemRequired) {
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
		s.runTask(plugin, currentNode.getAction().getRunnable());
	}
	
	public static class InputNode {
		private final HashMap<InputType, InputNode> children = new HashMap<>();
		private InputAction action;
		private boolean sameItemRequired = false;
		
		private long timeLastExecuted;
		
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
		
		public long getTimeLastExecuted() {
			return timeLastExecuted;
		}
		
		public void setTimeLastExecuted(long time) {
			timeLastExecuted = time;
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
