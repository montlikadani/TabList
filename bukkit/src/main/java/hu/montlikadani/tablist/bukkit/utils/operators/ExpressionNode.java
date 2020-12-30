package hu.montlikadani.tablist.bukkit.utils.operators;

public interface ExpressionNode {

	int getType();

	String getParseExpression();

	void setParseExpression(String parseExpression);

	Condition getCondition();

	String[] getExpressions();

	boolean parse(double rightCond);
}
