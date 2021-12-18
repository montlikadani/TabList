package hu.montlikadani.tablist.logicalOperators;

import java.util.List;
import java.util.regex.Pattern;

public class OperatorNodes implements LogicalNode {

	protected final int type;
	protected Condition condition;

	public OperatorNodes(int type) {
		this.type = type;
	}

	@Override
	public int getType() {
		return type;
	}

	@Override
	public LogicalNode parseInput(String input) {
		condition = makeConditionFromInput(input);
		return this;
	}

	@Override
	public Condition getCondition() {
		return condition;
	}

	private Condition makeConditionFromInput(String str) {
		String operator = "";

		for (String logicalOperator : new String[] { ">", ">=", "<", "<=", "==", "!=" }) {
			String s = str.replaceAll("[^" + logicalOperator + "]", "");

			if (s.equals(logicalOperator)) {
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
		if (type == NodeType.getLastPing()) {
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

	private static final Pattern VARIABLE_PATTERN = Pattern.compile("%tps%|%tps-overflow%|%ping%");

	public static String parseCondition(double value, int type, List<LogicalNode> nodes) {
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

		return (type == NodeType.getLastPing() ? builder.append((int) value) : builder.append(value)).toString();
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

				boolean firstPing = node.getType() == NodeType.getLastPing();

				if ((firstPing && node2.getType() == NodeType.getLastPing()
						&& node.getCondition().getSecondCondition() < node2.getCondition().getSecondCondition())
						|| (firstPing && node2.getType() == NodeType.getLastTps() && node.getCondition()
								.getSecondCondition() > node2.getCondition().getSecondCondition())) {
					nodes.set(i, node2);
					nodes.set(j, node);
				}
			}
		}
	}
}
