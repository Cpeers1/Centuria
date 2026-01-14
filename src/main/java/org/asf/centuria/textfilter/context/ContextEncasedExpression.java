package org.asf.centuria.textfilter.context;

import java.util.function.Function;

import org.asf.centuria.textfilter.ContextPhrase;

public class ContextEncasedExpression implements IContextExpression {
	private IContextExpression expression;

	public ContextEncasedExpression(IContextExpression expression) {
		this.expression = expression;
	}

	@Override
	public boolean compare(Function<ContextPhrase, Boolean> comparisonCall) {
		return expression.compare(comparisonCall);
	}
}
