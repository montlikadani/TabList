package hu.montlikadani.tablist.utils.operators;

public class Condition {

	private String operator;
	private String[] parseable;

	public Condition(String operator, String[] parseable) {
		this.operator = operator;
		this.parseable = parseable == null ? new String[0] : parseable;
	}

	public String[] getParseable() {
		return parseable;
	}

	public String getOperator() {
		return operator;
	}

	public int getSecondCondition() {
		try {
			return parseable.length > 1 ? Integer.parseInt(parseable[1].trim()) : 0;
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	public String getColor() {
		String color = parseable.length != 0 ? parseable[0] : "";
		if (!color.trim().isEmpty()) {
			if (color.contains("%player-ping%")) {
				color = color.replace("%player-ping%", "");
			}

			color = color.trim();
		}

		return color;
	}
}
