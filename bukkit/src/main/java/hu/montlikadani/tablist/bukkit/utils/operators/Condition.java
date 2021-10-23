package hu.montlikadani.tablist.bukkit.utils.operators;

public class Condition {

	private final String operator;
	private final String[] parseable;

	private String color = "";
	private double secondCondition = 0D;

	public Condition(String operator, String[] parseable) {
		this.operator = operator;
		this.parseable = parseable;

		if (parseable.length > 1) {
			try {
				secondCondition = Double.parseDouble(
						parseable[(parseable[0].indexOf("%tps%") >= 0 || parseable[0].indexOf("%tps-overflow%") >= 0
								|| parseable[0].indexOf("%ping%") >= 0) ? 1 : 0]);
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
