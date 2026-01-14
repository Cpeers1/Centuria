package org.asf.centuria.textfilter.context;

import java.util.function.Function;
import java.util.function.Supplier;

import org.asf.centuria.textfilter.ContextPhrase;

public class ContextResolvableWrappedPhrase implements IContextExpression {
	private ContextPhrase phrase;
	private Supplier<ContextPhrase> resolver;

	public ContextResolvableWrappedPhrase(Supplier<ContextPhrase> resolver) {
		this.resolver = resolver;
	}

	@Override
	public boolean compare(Function<ContextPhrase, Boolean> comparisonCall) {
		if (phrase == null)
			phrase = resolver.get();
		return comparisonCall.apply(phrase);
	}
}
