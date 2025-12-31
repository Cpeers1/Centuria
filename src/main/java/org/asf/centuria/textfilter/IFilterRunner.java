package org.asf.centuria.textfilter;

import java.util.Map;

import org.asf.centuria.textfilter.result.FilterResult;

/**
 * 
 * Text Filter Implementation Interface
 * 
 * @author Sky Swimmer
 * 
 */
public interface IFilterRunner {

	/**
	 * Checks if a string is filtered
	 * 
	 * @param text       String to check
	 * @param strictMode True for strict-mode filtering, false otherwise
	 * @param filters    Filter sets to use
	 * @param tags       Tags to use for selecting filter sets
	 * @return True if filtered, false otherwise
	 */
	public boolean isFiltered(String text, boolean strictMode, Map<String, PhraseFilterSet> filters, String[] tags);

	/**
	 * Checks if a string results in a mute
	 * 
	 * @param text    String to check
	 * @param filters Filter sets to use
	 * @param tags    Tags to use for selecting filter sets
	 * @return True if severely filtered, false otherwise
	 */
	public boolean shouldFilterMute(String text, Map<String, PhraseFilterSet> filters, String[] tags);

	/**
	 * Filters strings
	 * 
	 * @param text          String to filter
	 * @param strictMode    True for strict-mode filtering, false otherwise
	 * @param filters       Filter sets to use
	 * @param tags          Tags to use for selecting filter sets
	 * @param stringBuilder Filter string builder to user
	 * @return Result string
	 */
	public default String filterString(String text, boolean strictMode, Map<String, PhraseFilterSet> filters,
			String[] tags, IResultStringBuilder stringBuilder) {
		return filter(text, strictMode, filters, tags, stringBuilder).getFilterResult();
	}

	/**
	 * Filters strings
	 * 
	 * @param text          String to filter
	 * @param strictMode    True for strict-mode filtering, false otherwise
	 * @param filters       Filter sets to use
	 * @param tags          Tags to use for selecting filter sets
	 * @param stringBuilder Filter string builder to user
	 * @return FilterResult value
	 */
	public FilterResult filter(String text, boolean strictMode, Map<String, PhraseFilterSet> filters, String[] tags,
			IResultStringBuilder stringBuilder);
}
