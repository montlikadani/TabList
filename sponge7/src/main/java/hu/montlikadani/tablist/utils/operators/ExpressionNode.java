package hu.montlikadani.tablist.utils.operators;

public interface ExpressionNode {

	String getParseExpression();

	void setParseExpression(String parseExpression);

	Condition getCondition();

	String[] getExpressions();

	boolean parse(int rightCond);
}
