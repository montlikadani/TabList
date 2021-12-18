package hu.montlikadani.tablist.logicalOperators;

public class Condition {

	private final String operator;
	private final String[] parseable;

	private String color = "";
	private double secondCondition = 0D;

	public Condition(String operator, String[] parseable) {
		this.operator = operator;
		this.parseable = parseable;

		if (parseable.length < 2) {
			return;
		}

		String first = parseable[0];
		String second = parseable[1];

		try {
			secondCondition = Double.parseDouble((first.indexOf("%ping%") != -1 || first.indexOf("%tps%") != -1
					|| first.indexOf("%tps-overflow%") != -1) ? second : first);
		} catch (NumberFormatException e) {
		}

		color = (second.matches("&|#") ? second : first).trim();
	}

	public String[] getParseable() {
		return parseable;
	}

	public String getOperator() {
		return operator;
	}

	public double getSecondCondition() {
		return secondCondition;
	}

	public String getColor() {
		return color;
	}
}
