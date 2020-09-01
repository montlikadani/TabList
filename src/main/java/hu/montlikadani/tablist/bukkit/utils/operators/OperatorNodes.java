package hu.montlikadani.tablist.bukkit.utils.operators;

import org.apache.commons.lang.StringUtils;

public class OperatorNodes implements ExpressionNode {

	private Condition condition;

	private String parseExpression;

	private final char[] expressions = { '>', '<', '=' }; // not equal not required

	public OperatorNodes(String str) {
		setParseExpression(str);
	}

	public OperatorNodes() {
	}

	@Override
	public void setParseExpression(String parseExpression) {
		if (!StringUtils.isEmpty(parseExpression)) {
			this.parseExpression = parseExpression;
			makeConditionFromInput(parseExpression);
		}
	}

	@Override
	public String getParseExpression() {
		return parseExpression;
	}

	@Override
	public char[] getExpressions() {
		return expressions;
	}

	@Override
	public Condition getCondition() {
		return condition;
	}

	private Condition makeConditionFromInput(final String str) {
		for (int i = 0; i < str.length(); i++) {
			char op = str.charAt(i);
			if (!isOperator(op) || !isNumber(str.charAt(i - 1))) {
				continue;
			}

			String[] c = str.split(String.valueOf(op));
			if (c.length > 1) {
				condition = new Condition(op, c);
				break;
			}
		}

		return condition;
	}

	private boolean isNumber(char c) {
		return (c >= '0' && c <= '9') || c == '.';
	}

	private boolean isOperator(char c) {
		for (int i = 0; i < expressions.length; i++) {
			if (c == expressions[i]) {
				return true;
			}
		}

		return false;
	}

	@Override
	public boolean parse(int leftCond) {
		boolean parsed = false;

		double secondCondition = condition.getSecondCondition();
		if (secondCondition < 0D || leftCond < 0D)
			return parsed;

		switch (condition.getOperator()) {
		case '>':
			parsed = leftCond > secondCondition;
			break;
		case '<':
			parsed = leftCond < secondCondition;
			break;
		case '=':
			parsed = leftCond == secondCondition;
			break;
		default:
			break;
		}

		return parsed;
	}
}
