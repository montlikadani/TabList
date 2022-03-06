package hu.montlikadani.tablist.utils.operators;

public class OperatorNodes implements ExpressionNode {

	private Condition condition;
	private String parseExpression;

	private final String[] expressions = { ">", ">=", "<", "<=", "==", "!=" };

	public OperatorNodes(String str) {
		setParseExpression(str);
	}

	@Override
	public void setParseExpression(String parseExpression) {
		if (parseExpression == null || parseExpression.isEmpty()) {
			return;
		}

		this.parseExpression = parseExpression;

		String operator = "";

		for (int i = 0; i < expressions.length; i++) {
			String s = parseExpression.replaceAll("[^" + expressions[i] + "]", "");

			if (s.equals(expressions[i])) {
				operator = s;
			}
		}

		if (operator.isEmpty()) {
			return;
		}

		String[] array = String.valueOf(parseExpression.replace(" ", "").replace(operator, ";").toCharArray()).split(";", 2);

		if (array.length > 1 && array[1].replaceAll("[^\\d]", "").matches("[0-9]+")) {
			condition = new Condition(operator, array);
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

	@Override
	public boolean parse(int firstCondition) {
		if (firstCondition < 0) {
			return false;
		}

		int secondCondition = condition.getSecondCondition();
		if (secondCondition < 0)
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
}
