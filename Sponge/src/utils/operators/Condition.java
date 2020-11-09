package hu.montlikadani.tablist.sponge.utils.operators;

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
			return parseable.length > 1 ? Integer.parseInt(parseable[1]) : 0;
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	public String getColor() {
		return parseable.length != 0 ? parseable[0] : "";
	}
}
