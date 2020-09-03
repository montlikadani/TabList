package hu.montlikadani.tablist.bukkit.utils.operators;

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

	public double getSecondCondition() {
		try {
			return parseable.length > 1 ? Double.parseDouble(parseable[0]) : 0D;
		} catch (NumberFormatException e) {
			return 0;
		}
	}
}
