package hu.montlikadani.tablist.bukkit.utils.operators;

import org.apache.commons.lang.StringUtils;

import hu.montlikadani.tablist.bukkit.config.ConfigValues;

public class OperatorNodes implements ExpressionNode {

	private static int pingNode = 1, tpsNode = 2;

	private int type;
	private Condition condition;

	private String parseExpression;

	private final String[] expressions = { ">", ">=", "<", "<=", "==" }; // not equal not required

	public OperatorNodes(String str, int type) {
		this(type);

		setParseExpression(str);
	}

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
		if (!StringUtils.isEmpty(parseExpression)) {
			this.parseExpression = parseExpression;
			condition = makeConditionFromInput(parseExpression);
		}
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

		String[] c = String.valueOf(str.trim().replace(operator, ";").toCharArray()).split(";");
		return isNumber(c[0]) ? new Condition(operator, c) : null;
	}

	private boolean isNumber(String num) {
		try {
			Double.parseDouble(num);
			return true;
		} catch (NumberFormatException e) {
		}

		return false;
	}

	@Override
	public boolean parse(double leftCond) {
		if (condition == null) {
			return false;
		}

		if (type == NodeType.TPS) {
			double secondCondition = condition.getSecondCondition();
			if (secondCondition < 0D || leftCond < 0D)
				return false;

			// TODO: Is there a better way to split leftCond for to be equally for secondCondition?
			int left = Double.toString(leftCond).length(), right = Double.toString(secondCondition).length();
			if (left != right) {
				leftCond = Double
						.parseDouble(Double.toString(leftCond).substring(0, right - ConfigValues.getTpsSize()));
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
			default:
				return false;
			}
		} else if (type == NodeType.PING) {
			int firstCondition = (int) leftCond;
			int secondCondition = (int) condition.getSecondCondition();
			if (secondCondition < 0 || firstCondition < 0)
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
			default:
				return false;
			}
		}

		return false;
	}
}
