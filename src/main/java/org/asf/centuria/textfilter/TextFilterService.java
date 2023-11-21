package org.asf.centuria.textfilter;

import org.asf.centuria.textfilter.impl.TextFilterServiceImpl;
import org.asf.centuria.textfilter.result.FilterResult;

/**
 * 
 * Text filtering service
 * 
 * @author Sky Swimmer
 *
 */
public abstract class TextFilterService {

	protected static TextFilterService implementation = new TextFilterServiceImpl();

	/**
	 * Called to initialize the service
	 */
	public abstract void initService();

	/**
	 * Retrieves the text filter service instance
	 * 
	 * @return TextFilterService instance
	 */
	public static TextFilterService getInstance() {
		return implementation;
	}

	/**
	 * Checks if a string is filtered
	 * 
	 * @param text       String to check
	 * @param strictMode True for strict-mode filtering, false otherwise
	 * @param tags       Tags to use for selecting filter sets
	 * @return True if filtered, false otherwise
	 */
	public abstract boolean isFiltered(String text, boolean strictMode, String... tags);

	/**
	 * Checks if a string results in a mute
	 * 
	 * @param text String to check
	 * @param tags Tags to use for selecting filter sets
	 * @return True if severely filtered, false otherwise
	 */
	public abstract boolean shouldFilterMute(String text, String... tags);

	/**
	 * Filters strings
	 * 
	 * @param text       String to filter
	 * @param strictMode True for strict-mode filtering, false otherwise
	 * @param tags       Tags to use for selecting filter sets
	 * @return Result string
	 */
	public String filterString(String text, boolean strictMode, String... tags) {
		return filter(text, strictMode, tags).getFilterResult();
	}

	/**
	 * Filters strings
	 * 
	 * @param text       String to filter
	 * @param strictMode True for strict-mode filtering, false otherwise
	 * @param tags       Tags to use for selecting filter sets
	 * @return FilterResult value
	 */
	public abstract FilterResult filter(String text, boolean strictMode, String... tags);

	/**
	 * Retrieves all filter sets
	 * 
	 * @return Array of PhraseFilterSet instances
	 */
	public abstract PhraseFilterSet[] getFilterSets();

	/**
	 * Retrieves filter sets by name
	 * 
	 * @param name Filter set name
	 * @return PhraseFilterSet instance or null
	 */
	public abstract PhraseFilterSet getFilterSet(String name);

	/**
	 * Adds filter sets
	 * 
	 * @param set Filter set to add
	 */
	public abstract void addFilterSet(PhraseFilterSet set);

	/**
	 * Called to reload the text filter service
	 */
	public abstract void reload();

}
