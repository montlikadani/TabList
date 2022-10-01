package hu.montlikadani.tablist.logicalOperators;

public class OperatorNodes implements LogicalNode {

	protected final NodeType type;
	protected transient Condition condition;

	public OperatorNodes(NodeType type) {
		this.type = type;
	}

	@Override
	public NodeType getType() {
		return type;
	}

	@Override
	public LogicalNode parseInput(String input) {
		String strOperator = "";

		for (String logicalOperator : new String[] { ">", ">=", "<", "<=", "==", "!=" }) {
			String s = input.replaceAll("[^" + logicalOperator + "]", "");

			if (s.equals(logicalOperator)) {
				strOperator = s;
			}
		}

		Condition.RelationalOperators operator = Condition.RelationalOperators.getByOperator(strOperator);

		if (operator != null) {
			condition = new Condition(operator,
					String.valueOf(input.trim().replace(strOperator, ";").toCharArray()).split(";", 2));
		}

		return this;
	}

	@Override
	public Condition getCondition() {
		return condition;
	}

	@Override
	public boolean parse(double receivedValue) {
		if (type == NodeType.PING) {
			int secondCondition = (int) condition.getSecondCondition();
			if (secondCondition < 0)
				return false;

			int firstCondition = (int) receivedValue;
			if (firstCondition < 0)
				return false;

			switch (condition.operator) {
			case GREATER_THAN:
				return firstCondition > secondCondition;
			case GREATER_THAN_OR_EQUAL:
				return firstCondition >= secondCondition;
			case LESS_THAN:
				return firstCondition < secondCondition;
			case LESS_THAN_OR_EQUAL:
				return firstCondition <= secondCondition;
			case EQUAL:
				return firstCondition == secondCondition;
			case NOT_EQUAL:
				return firstCondition != secondCondition;
			default:
				return false;
			}
		}

		return false;
	}
}
