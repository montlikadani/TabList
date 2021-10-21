package hu.montlikadani.tablist.bukkit.utils.operators;

public class Condition {

	private String operator;
	private String[] parseable;

	private String color = "";
	private double secondCondition = 0D;

	public Condition(String operator, String[] parseable) {
		this.operator = operator;

		if (parseable == null) {
			parseable = new String[0];
		}

		this.parseable = parseable;

		if (parseable.length > 1) {
			try {
				secondCondition = Double.parseDouble(
						parseable[(parseable[0].contains("%tps%") || parseable[0].contains("%ping%")) ? 1 : 0]);
			} catch (NumberFormatException e) {
			}

			color = (parseable[1].matches("&|#") ? parseable[1] : parseable[0]).trim();
		}
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
