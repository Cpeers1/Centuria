package org.asf.centuria.textfilter.context;

import java.util.function.Function;

import org.asf.centuria.textfilter.ContextPhrase;

public interface IContextExpression {

	public boolean compare(Function<ContextPhrase, Boolean> comparisonCall);

}
