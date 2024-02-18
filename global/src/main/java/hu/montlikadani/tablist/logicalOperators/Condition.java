package hu.montlikadani.tablist.logicalOperators;

public class Condition {

	public final RelationalOperators operator;
	public final String color;

	private double value;

	public Condition(RelationalOperators operator, String[] content) {
		this.operator = operator;

		try {
			value = Double.parseDouble(content[1].trim());
		} catch (NumberFormatException ex) {
			ex.printStackTrace();
		}

		color = content[0].trim().replace("%tps%", "").replace("%tps-overflow%", "")
				.replace("%ping%", "").replace('&', '\u00a7');
	}

	public double getValue() {
		return value;
	}

	public enum RelationalOperators {

		GREATER_THAN(">"), GREATER_THAN_OR_EQUAL(">="), LESS_THAN("<"), LESS_THAN_OR_EQUAL("<="), EQUAL("=="), NOT_EQUAL("!=");

		public final String operator;

		RelationalOperators(String operator) {
			this.operator = operator;
		}
	}
}
