package hu.montlikadani.tablist.utils.operators;

public class Condition {

	private final String operator;
	private final String[] parseable;

	private String color = "";
	private int secondCondition = 0;

	public Condition(String operator, String[] parseable) {
		this.operator = operator;

		if (parseable == null) {
			parseable = new String[0];
		}

		this.parseable = parseable;

		if (parseable.length > 1) {
			try {
				secondCondition = Integer.parseInt(parseable[1]);
			} catch (NumberFormatException e) {
			}

			color = parseable[0].replace("%player-ping%", "");
		}
	}

	public String[] getParseable() {
		return parseable;
	}

	public String getOperator() {
		return operator;
	}

	public int getSecondCondition() {
		return secondCondition;
	}

	public String getColor() {
		return color;
	}
}
