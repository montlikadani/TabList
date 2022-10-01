package hu.montlikadani.tablist.logicalOperators;

import java.util.List;

public interface LogicalNode {

	static LogicalNode newNode(NodeType type) {
		return new OperatorNodes(type);
	}

	LogicalNode parseInput(String input);

	NodeType getType();

	Condition getCondition();

	boolean parse(double value);

	public enum NodeType {

		PING, TPS

	}

	public static StringBuilder parseCondition(double value, NodeType type, List<LogicalNode> nodes) {
		String color = "";

		for (LogicalNode node : nodes) {
			if (node.getType() == type && node.parse(value)) {
				color = node.getCondition().getColor();
			}
		}

		StringBuilder builder = new StringBuilder(color);

		return type == NodeType.PING ? builder.append((int) value) : builder.append(value);
	}

	public static void reverseOrderOfArray(List<LogicalNode> nodes) {
		int size = nodes.size();
		int start = size - 1;

		// Sort
		// ping in descending order
		// tps in ascending order
		for (int i = 0; i < size; i++) {
			for (int j = start; j > i; j--) {
				LogicalNode node = nodes.get(i), node2 = nodes.get(j);

				boolean firstPing = node.getType() == NodeType.PING;

				if ((firstPing && node2.getType() == NodeType.PING
						&& node.getCondition().getSecondCondition() < node2.getCondition().getSecondCondition())
						|| (firstPing && node2.getType() == NodeType.TPS
								&& node.getCondition().getSecondCondition() > node2.getCondition().getSecondCondition())) {
					nodes.set(i, node2);
					nodes.set(j, node);
				}
			}
		}
	}
}
