package org.asf.centuria.textfilter.context;

import java.util.function.Function;

import org.asf.centuria.textfilter.ContextPhrase;

public class ContextWrappedPhrase implements IContextExpression {
	private ContextPhrase phrase;

	public ContextWrappedPhrase(ContextPhrase phrase) {
		this.phrase = phrase;
	}

	@Override
	public boolean compare(Function<ContextPhrase, Boolean> comparisonCall) {
		return comparisonCall.apply(phrase);
	}
}
