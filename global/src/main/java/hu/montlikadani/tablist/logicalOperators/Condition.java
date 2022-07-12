package hu.montlikadani.tablist.logicalOperators;

public class Condition {

	public final RelationalOperators operator;
	public final String[] parseable;

	private String color = "";
	private double secondCondition = 0D;

	public Condition(RelationalOperators operator, String[] parseable) {
		this.operator = operator;
		this.parseable = parseable;

		if (parseable.length < 2) {
			return;
		}

		String first = parseable[0];
		String second = parseable[1];

		try {
			secondCondition = Double.parseDouble(
					(first.indexOf("%ping%") != -1 || first.indexOf("%tps%") != -1 || first.indexOf("%tps-overflow%") != -1)
							? second
							: first);
		} catch (NumberFormatException e) {
		}

		color = (second.matches("&|#") ? second : first).trim().replace("%tps%", "").replace("%tps-overflow%", "")
				.replace("%ping%", "").replace('&', '\u00a7');
	}

	public double getSecondCondition() {
		return secondCondition;
	}

	public String getColor() {
		return color;
	}

	public enum RelationalOperators {

		GREATER_THAN(">"), GREATER_THAN_OR_EQUAL(">="), LESS_THAN("<"), LESS_THAN_OR_EQUAL("<="), EQUAL("=="), NOT_EQUAL("!=");

		public final String operator;

		RelationalOperators(String operator) {
			this.operator = operator;
		}

		public static RelationalOperators getByOperator(String operator) {
			if (!operator.isEmpty()) {
				for (RelationalOperators one : RelationalOperators.values()) {
					if (one.operator.equals(operator)) {
						return one;
					}
				}
			}

			return null;
		}
	}
}
