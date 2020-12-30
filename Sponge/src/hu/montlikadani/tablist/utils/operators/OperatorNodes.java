package hu.montlikadani.tablist.utils.operators;

public class OperatorNodes implements ExpressionNode {

	private Condition condition;
	private String parseExpression;

	private final String[] expressions = { ">", ">=", "<", "<=", "==" }; // not equal not required

	public OperatorNodes(String str) {
		setParseExpression(str);
	}

	@Override
	public void setParseExpression(String parseExpression) {
		if (parseExpression != null && !parseExpression.isEmpty()) {
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
		return c[0].contains("%player-ping%")
				? (c[1].replaceAll("[^\\d]", "").matches("[0-9]+")) ? new Condition(operator, c) : null
				: null;
	}

	@Override
	public boolean parse(int firstCondition) {
		if (condition == null) {
			return false;
		}

		int secondCondition = condition.getSecondCondition();
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
}
