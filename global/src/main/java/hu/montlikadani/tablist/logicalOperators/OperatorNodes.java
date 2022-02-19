package hu.montlikadani.tablist.logicalOperators;

import java.util.List;
import java.util.regex.Pattern;

public class OperatorNodes implements LogicalNode {

	protected final NodeType type;
	protected Condition condition;

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
	public boolean parse(double leftCond) {
		if (type == NodeType.PING) {
			int secondCondition = (int) condition.getSecondCondition();
			if (secondCondition < 0)
				return false;

			int firstCondition = (int) leftCond;
			if (firstCondition < 0)
				return false;

			switch (condition.getOperator()) {
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

	private static final Pattern VARIABLE_PATTERN = Pattern.compile("%tps%|%tps-overflow%|%ping%");

	public static String parseCondition(double value, NodeType type, List<LogicalNode> nodes) {
		String color = "";

		for (LogicalNode node : nodes) {
			if (node.getType() == type && node.parse(value)) {
				color = node.getCondition().getColor();
			}
		}

		StringBuilder builder = new StringBuilder();

		if (!color.isEmpty()) {
			builder.append(VARIABLE_PATTERN.matcher(color).replaceAll("").replace('&', '\u00a7'));
		}

		return (type == NodeType.PING ? builder.append((int) value) : builder.append(value)).toString();
	}

	public static void reverseOrderOfArray(List<LogicalNode> nodes) {
		int size = nodes.size();
		int start = size - 1;

		// Sort
		// ping in descending order
		// tps in ascending order
		for (int i = 0; i < size; i++) {
			for (int j = start; j > i; j--) {
				LogicalNode node = nodes.get(i), node2 = nodes.get(j);

				boolean firstPing = node.getType() == NodeType.PING;

				if ((firstPing && node2.getType() == NodeType.PING
						&& node.getCondition().getSecondCondition() < node2.getCondition().getSecondCondition())
						|| (firstPing && node2.getType() == NodeType.TPS
								&& node.getCondition().getSecondCondition() > node2.getCondition().getSecondCondition())) {
					nodes.set(i, node2);
					nodes.set(j, node);
				}
			}
		}
	}
}
