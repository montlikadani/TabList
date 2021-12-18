package hu.montlikadani.tablist.utils.operators;

import hu.montlikadani.tablist.config.constantsLoader.ConfigValues;
import hu.montlikadani.tablist.logicalOperators.LogicalNode;

public class OverriddenOperatorNodes extends hu.montlikadani.tablist.logicalOperators.OperatorNodes {

	public OverriddenOperatorNodes(int type) {
		super(type);
	}

	@Override
	public boolean parse(double leftCond) {
		if (type == LogicalNode.NodeType.getLastTps()) {
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
			case ">":
				return leftCond > secondCondition;
			case ">=":
				return leftCond >= secondCondition;
			case "<":
				return leftCond < secondCondition;
			case "<=":
				return leftCond <= secondCondition;
			case "==":
				return leftCond == secondCondition;
			case "!=":
				return leftCond != secondCondition;
			default:
				return false;
			}
		}

		return super.parse(leftCond);
	}
}
