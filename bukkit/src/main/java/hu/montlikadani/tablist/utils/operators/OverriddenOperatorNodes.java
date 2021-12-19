package hu.montlikadani.tablist.utils.operators;

import hu.montlikadani.tablist.config.constantsLoader.ConfigValues;
import hu.montlikadani.tablist.logicalOperators.LogicalNode;

public class OverriddenOperatorNodes extends hu.montlikadani.tablist.logicalOperators.OperatorNodes {

	public OverriddenOperatorNodes(LogicalNode.NodeType type) {
		super(type);
	}

	@Override
	public boolean parse(double leftCond) {
		if (type == LogicalNode.NodeType.TPS) {
			if (leftCond < 0.0)
				return false;

			double secondCondition = condition.getSecondCondition();
			if (secondCondition < 0.0)
				return false;

			// Making leftCond to be equally to secondCondition with tpsSize
			int tpsSize = ConfigValues.getTpsSize();
			if (Math.floor(leftCond * tpsSize) != Math.floor(secondCondition * tpsSize)) {
				String lc = Double.toString(leftCond);
				int size = (tpsSize == 1 ? 3 : lc.indexOf('.')) + (tpsSize < 1 ? 2 : tpsSize);
				int length = lc.length();

				leftCond = Double.parseDouble(lc.substring(0, size > length ? length : size));
			}

			switch (condition.getOperator()) {
			case GREATER_THAN:
				return leftCond > secondCondition;
			case GREATER_THAN_OR_EQUAL:
				return leftCond >= secondCondition;
			case LESS_THAN:
				return leftCond < secondCondition;
			case LESS_THAN_OR_EQUAL:
				return leftCond <= secondCondition;
			case EQUAL:
				return leftCond == secondCondition;
			case NOT_EQUAL:
				return leftCond != secondCondition;
			default:
				return false;
			}
		}

		return super.parse(leftCond);
	}
}
