package hu.montlikadani.tablist.utils.operators;

import hu.montlikadani.tablist.logicalOperators.LogicalNode;

public final class OverriddenOperatorNodes extends hu.montlikadani.tablist.logicalOperators.OperatorNodes {

	public OverriddenOperatorNodes(LogicalNode.NodeType type) {
		super(type);
	}

	@Override
	public boolean parse(double receivedValue) {
		if (type == LogicalNode.NodeType.TPS) {
			if (receivedValue < 0.0)
				return false;

			double secondCondition = condition.getSecondCondition();
			if (secondCondition < 0.0)
				return false;

			switch (condition.operator) {
			case GREATER_THAN:
				return receivedValue > secondCondition;
			case GREATER_THAN_OR_EQUAL:
				return receivedValue >= secondCondition;
			case LESS_THAN:
				return receivedValue < secondCondition;
			case LESS_THAN_OR_EQUAL:
				return receivedValue <= secondCondition;
			case EQUAL:
				return receivedValue == secondCondition;
			case NOT_EQUAL:
				return receivedValue != secondCondition;
			default:
				return false;
			}
		}

		return super.parse(receivedValue);
	}
}
