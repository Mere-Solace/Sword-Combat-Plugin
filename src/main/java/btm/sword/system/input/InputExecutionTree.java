package btm.sword.system.input;


import btm.sword.system.CombatProfile;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class InputExecutionTree {
	private final InputNode root = new InputNode(null, null, false);
	private InputNode currentNode = root;
	
	public InputExecutionTree() {
		init();
	}
	
	public InputExecutionTree(CombatProfile combatProfile) {
		this();
		List<List<InputType>> playerBoundAttacks = combatProfile.getAllBoundSequences();
		
		for (List<InputType> inputSequence : playerBoundAttacks) {
			add(inputSequence);
		}
	}
	
	public boolean takeInput(InputType input) {
		if (currentNode.noChild(input)) return false;
		
		currentNode = currentNode.getChild(input);
		return true;
	}
	
	public void reset() {
		currentNode = root;
	}
	
	public LinkedList<InputType> getSequence() {
		return currentNode.getSequence();
	}
	
	public void add(List<InputType> inputSequence) {
		InputNode dummy = root;
		for (InputType input : inputSequence) {
			if (dummy.noChild(input))
				dummy.addChild(input, false);
			dummy = dummy.getChild(input);
		}
		dummy.setExecutionState(true);
	}
	
	public boolean inExecutionState() {
		return currentNode.isExecutionState;
	}
	
	public static class InputNode {
		private final InputType input;
		private InputNode parent;
		private final HashMap<InputType, InputNode> children = new HashMap<>();
		private boolean isExecutionState;
		
		public InputNode(InputType input, InputNode parent, boolean isExecutionState) {
			this.input = input;
			this.parent = parent;
			this.isExecutionState = isExecutionState;
		}
		
		public void addChild(InputType input, boolean isExecutionState) {
			children.put(input, new InputNode(input, this, isExecutionState));
		}
		
		public void addChild(InputNode inputNode) {
			children.put(input, inputNode);
			getChild(input).parent = this;
		}
		
		public boolean noChild(InputType input) {
			return !children.containsKey(input);
		}
		
		public InputNode getChild(InputType input) {
			return children.get(input);
		}
		
		public void setExecutionState(boolean isExecutionState) {
			this.isExecutionState = isExecutionState;
		}
		
		public LinkedList<InputType> getSequence() {
			LinkedList<InputType> sequence;
			
			if (parent != null)
				sequence = parent.getSequence();
			else
				sequence = new LinkedList<>();
			
			if (input != null)
				sequence.add(input);
			
			return sequence;
		}
	}
	
	public void init() {
		add(List.of(InputType.DROP, InputType.DROP));
		add(List.of(InputType.LEFT));
		add(List.of(InputType.LEFT, InputType.LEFT));
		add(List.of(InputType.LEFT, InputType.LEFT, InputType.LEFT));
	}
}
