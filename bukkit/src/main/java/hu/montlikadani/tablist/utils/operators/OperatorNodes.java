package hu.montlikadani.tablist.utils.operators;

import hu.montlikadani.tablist.config.constantsLoader.ConfigValues;

public class OperatorNodes implements ExpressionNode {

	private static int pingNode = 1, tpsNode = 2;

	private int type;
	private Condition condition;

	private String parseExpression;

	private final String[] expressions = { ">", ">=", "<", "<=", "==", "!=" };

	public OperatorNodes(int type) {
		this.type = type;

		pingNode++;
		tpsNode++;
	}

	public static final class NodeType {
		public static final int PING = pingNode;
		public static final int TPS = tpsNode;

		private NodeType() {
		}
	}

	@Override
	public int getType() {
		return type;
	}

	@Override
	public void setParseExpression(String parseExpression) {
		this.parseExpression = parseExpression;
		condition = makeConditionFromInput(parseExpression);
	}

	@Override
	public String getParseExpression() {
		return parseExpression;
	}

	@Override
	public String[] getExpressions() {
		return expressions;
	}

	@Override
	public Condition getCondition() {
		return condition;
	}

	private Condition makeConditionFromInput(final String str) {
		String operator = "";

		for (int i = 0; i < expressions.length; i++) {
			String expression = expressions[i];
			String s = str.replaceAll("[^" + expression + "]", "");

			if (s.contentEquals(expression)) {
				operator = s;
			}
		}

		if (operator.isEmpty()) {
			return null;
		}

		return new Condition(operator, String.valueOf(str.trim().replace(operator, ";").toCharArray()).split(";", 2));
	}

	@Override
	public boolean parse(double leftCond) {
		if (type == NodeType.TPS) {
			if (leftCond < 0D)
				return false;

			double secondCondition = condition.getSecondCondition();
			if (secondCondition < 0D)
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

		if (type == NodeType.PING) {
			int secondCondition = (int) condition.getSecondCondition();
			if (secondCondition < 0)
				return false;

			int firstCondition = (int) leftCond;
			if (firstCondition < 0)
				return false;

			switch (condition.getOperator()) {
			case ">":
				return firstCondition > secondCondition;
			case ">=":
				return firstCondition >= secondCondition;
			case "<":
				return firstCondition < secondCondition;
			case "<=":
				return firstCondition <= secondCondition;
			case "==":
				return firstCondition == secondCondition;
			case "!=":
				return firstCondition != secondCondition;
			default:
				return false;
			}
		}

		return false;
	}
}
