package org.asf.centuria.textfilter.context;

import java.util.function.Function;

import org.asf.centuria.textfilter.ContextPhrase;

public class ContextCompareExpression implements IContextExpression {
	private IContextExpression leftHand;
	private ContextExpressionOperator operator;
	private IContextExpression rightHand;

	public ContextCompareExpression(IContextExpression leftHand, ContextExpressionOperator operator,
			IContextExpression rightHand) {
		this.leftHand = leftHand;
		this.operator = operator;
		this.rightHand = rightHand;
	}

	@Override
	public boolean compare(Function<ContextPhrase, Boolean> comparisonCall) {
		boolean left = leftHand.compare(comparisonCall);
		if (left && operator == ContextExpressionOperator.OR)
			return true;
		else if (left && operator == ContextExpressionOperator.AND)
			return rightHand.compare(comparisonCall);
		else if (operator == ContextExpressionOperator.OR)
			return rightHand.compare(comparisonCall);
		return false;
	}
}
