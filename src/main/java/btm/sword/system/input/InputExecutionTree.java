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
		this.timeoutTicks = (long) (timeoutMillis * (0.02));
	}
	
	public void takeInput(InputType input, Material itemUsed, SwordPlayer executor) {
		if (currentNode == null || currentNode.noChild(input)) {
			if (currentNode != root) {
				reset();
				SoundUtils.playSound(executor.entity(), SoundType.BLOCK_GRINDSTONE_USE, 0.5f, 1f);
			}
			
			takeInput(input, itemUsed, executor);
			return;
		}
		
		if (timeoutTimer != null) timeoutTimer.cancel();
		
		InputNode next = currentNode.getChild(input);
		InputAction action = next.getAction();
		
		if (action.isSameItemRequired() && itemUsed != executor.getItemLastUsed()) reset();
		
		currentNode = next;
		
		if (inActionState()) action.execute(executor, s, plugin);
		
		sequenceToDisplay.append(inputToString(input));
		
		if (!noChildren()) {
			sequenceToDisplay.append(" + ");
			
			timeoutTimer = new BukkitRunnable() {
				@Override
				public void run() {
					reset();
				}
			}.runTaskLater(plugin, timeoutTicks);
		}
	}
	
	public void reset() {
		currentNode = root;
		sequenceToDisplay = new StringBuilder();
	}
	
	public boolean atRoot() {
		return currentNode.equals(root);
	}
	
	public void add(List<InputType> inputSequence, InputAction action) {
		InputNode dummy = root;
		for (InputType input : inputSequence) {
			if (dummy.noChild(input)) {
				dummy.addChild(input, null);
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
						Combatant::cannotPerformAction, false));
		
		add(List.of(InputType.SHIFT, InputType.DROP),
				new InputAction(
						MovementAction.dash(swordPlayer, false),
						executor -> executor.calcCooldown(200L, 1000L, StatType.CELERITY, 10),
						Combatant::cannotPerformAction, false));
		
		// grab
		add(List.of(InputType.SHIFT, InputType.RIGHT),
				new InputAction(
						UtilityAction.grab(swordPlayer),
						executor -> executor.calcCooldown(200L, 1000L, StatType.FORTITUDE, 10),
						Combatant::cannotPerformAction, false));
		
		// Item dependent actions:
		// basic attack sequence
		add(List.of(InputType.LEFT),
				new InputAction(
						AttackAction.basic(swordPlayer, 0),
						executor -> executor.calcCooldown(200L, 1000L, StatType.FORM, 10),
						Combatant::cannotPerformAction, true));
		
		add(List.of(InputType.LEFT, InputType.LEFT),
				new InputAction(
						AttackAction.basic(swordPlayer, 1),
						executor -> executor.calcCooldown(200L, 1000L, StatType.FORM, 10),
						Combatant::cannotPerformAction, true));
		
		add(List.of(InputType.LEFT, InputType.LEFT, InputType.LEFT),
				new InputAction(
						AttackAction.basic(swordPlayer, 2),
						executor -> executor.calcCooldown(200L, 1000L, StatType.FORM, 10),
						Combatant::cannotPerformAction, true));
		
		// side step attacks
		add(List.of(InputType.SWAP, InputType.RIGHT),
				new InputAction(AttackAction.sideStep(swordPlayer, true),
						executor -> executor.calcCooldown(300L, 600L, StatType.CELERITY, 10),
						Combatant::cannotPerformAction, true));
		
		add(List.of(InputType.SWAP, InputType.LEFT),
				new InputAction(AttackAction.sideStep(swordPlayer, false),
						executor -> executor.calcCooldown(300L, 600L, StatType.CELERITY, 10),
						Combatant::cannotPerformAction, true));
		
		// skills
		add(List.of(InputType.DROP, InputType.RIGHT, InputType.SHIFT), null);
		
		add(List.of(InputType.DROP, InputType.RIGHT, InputType.DROP), null);
		
		add(List.of(InputType.DROP, InputType.RIGHT, InputType.LEFT),
				new InputAction(
						AttackAction.heavy(swordPlayer, 1),
						executor -> executor.calcCooldown(400L, 1000L, StatType.FORM, 10),
						Combatant::cannotPerformAction, true));
	}
	
	private static class InputNode {
		private InputAction action;
		private final HashMap<InputType, InputNode> children = new HashMap<>();
		
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
	}
}
