package hu.montlikadani.tablist.bukkit.utils.operators;

public interface ExpressionNode {

	String getParseExpression();

	void setParseExpression(String parseExpression);

	Condition getCondition();

	char[] getExpressions();

	boolean parse(int rightCond);
}
