package org.asf.centuria.textfilter.context;

import java.util.ArrayList;
import java.util.Collection;

import org.asf.centuria.textfilter.FilterSeverity;
import org.asf.centuria.textfilter.result.FilterResult;

public class WrappedTextFilterContextMemory extends TextFilterContextMemory {

	private TextFilterContextMemory[] delegates;

	public WrappedTextFilterContextMemory(TextFilterContextMemory... delegates) {
		super(FilterSeverity.NONE, -1);
		this.delegates = delegates;
	}

	@Override
	public void pushToContext(FilterResult result) {
		for (TextFilterContextMemory inst : delegates)
			inst.pushToContext(result);
	}

	@Override
	public void removeFromContext(FilterResult result) {
		for (TextFilterContextMemory inst : delegates)
			inst.removeFromContext(result);
	}

	@Override
	public void removeAll(FilterResult[] results) {
		for (TextFilterContextMemory inst : delegates)
			inst.removeAll(results);
	}

	@Override
	public void removeAll(Collection<FilterResult> results) {
		for (TextFilterContextMemory inst : delegates)
			inst.removeAll(results);
	}

	@Override
	public int count() {
		int i = 0;
		for (TextFilterContextMemory inst : delegates)
			i += inst.count();
		return i;
	}

	@Override
	public FilterResult[] getMessagesInContext() {
		ArrayList<FilterResult> res = new ArrayList<FilterResult>();
		for (TextFilterContextMemory inst : delegates) {
			for (FilterResult r : inst.getMessagesInContext())
				res.add(r);
		}
		return res.toArray(t -> new FilterResult[t]);
	}

}
