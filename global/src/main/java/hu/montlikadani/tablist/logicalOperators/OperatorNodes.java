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
		Condition.RelationalOperators chosen = null;
		Condition.RelationalOperators[] values = Condition.RelationalOperators.values();

		// TODO Do we really need validation for the existence of tps and ping placeholders?
		// I think it's not that important honestly, because each option is separated

		for (Condition.RelationalOperators logicalOperator : values) {
			if (input.replaceAll("[^" + logicalOperator + "]", "").equals(logicalOperator.operator)) {
				chosen = logicalOperator;

				// Do not break the loop operation here, it may have multiple conditions with same operator
			}
		}

		if (chosen != null) {
			for (Condition.RelationalOperators one : values) {
				if (one != chosen) {
					continue;
				}

				String[] arr = input.trim().replace(one.operator, ";").split(";", 2);

				if (arr.length > 1) {
					condition = new Condition(one, arr);
				}

				break;
			}
		}

		return this;
	}

	@Override
	public Condition getCondition() {
		return condition;
	}

	@Override
	public boolean parse(double receivedValue) {
		if (type != NodeType.PING) {
			return false;
		}

		int firstValue = (int) receivedValue;
		if (firstValue < 0)
			return false;

		int secondValue = (int) condition.getValue();
		if (secondValue < 0)
			return false;

		switch (condition.operator) {
			case GREATER_THAN:
				return firstValue > secondValue;
			case GREATER_THAN_OR_EQUAL:
				return firstValue >= secondValue;
			case LESS_THAN:
				return firstValue < secondValue;
			case LESS_THAN_OR_EQUAL:
				return firstValue <= secondValue;
			case EQUAL:
				return firstValue == secondValue;
			case NOT_EQUAL:
				return firstValue != secondValue;
			default:
				return false;
		}
	}
}
