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
		// Sort
		// ping in descending order
		// tps in ascending order

		nodes.sort((node, node2) -> {
			if (node.getType() == NodeType.PING && node2.getType() == NodeType.PING) {
				return node.getCondition().getSecondCondition() < node2.getCondition().getSecondCondition() ? 1 : -1;
			}

			if (node.getType() == NodeType.TPS && node2.getType() == NodeType.TPS) {
				return node.getCondition().getSecondCondition() > node2.getCondition().getSecondCondition() ? 1 : -1;
			}

			return -1;
		});
	}
}
