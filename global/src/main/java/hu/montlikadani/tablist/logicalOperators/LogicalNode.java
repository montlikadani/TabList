package hu.montlikadani.tablist.logicalOperators;

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
}
