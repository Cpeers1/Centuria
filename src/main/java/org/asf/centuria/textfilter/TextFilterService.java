package org.asf.centuria.textfilter;

import org.asf.centuria.textfilter.context.TextFilterContextMemory;
import org.asf.centuria.textfilter.impl.FileBasedTextServiceImpl;
import org.asf.centuria.textfilter.result.FilterResult;

/**
 * 
 * Text filtering service
 * 
 * @author Sky Swimmer
 *
 */
public abstract class TextFilterService {

	protected static TextFilterService implementation = new FileBasedTextServiceImpl();

	/**
	 * Defines the default string builder
	 * 
	 * @return IResultStringBuilder instance
	 */
	protected abstract IResultStringBuilder defaultStringBuilder();

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
	 * @param memory     Text filter memory object to use
	 * @param text       String to check
	 * @param strictMode True for strict-mode filtering, false otherwise
	 * @param tags       Tags to use for selecting filter sets
	 * @return True if filtered, false otherwise
	 */
	public abstract boolean isFiltered(TextFilterContextMemory memory, String text, boolean strictMode, String... tags);

	/**
	 * Checks if a string results in a mute
	 * 
	 * @param memory Text filter memory object to use
	 * @param text   String to check
	 * @param tags   Tags to use for selecting filter sets
	 * @return True if severely filtered, false otherwise
	 */
	public abstract boolean shouldFilterMute(TextFilterContextMemory memory, String text, String... tags);

	/**
	 * Filters strings
	 * 
	 * @param memory     Text filter memory object to use
	 * @param text       String to filter
	 * @param strictMode True for strict-mode filtering, false otherwise
	 * @param tags       Tags to use for selecting filter sets
	 * @return Result string
	 */
	public String filterString(TextFilterContextMemory memory, String text, boolean strictMode, String... tags) {
		return filter(memory, text, strictMode, tags).getFilterResult();
	}

	/**
	 * Filters strings
	 * 
	 * @param memory     Text filter memory object to use
	 * @param text       String to filter
	 * @param strictMode True for strict-mode filtering, false otherwise
	 * @param tags       Tags to use for selecting filter sets
	 * @return FilterResult value
	 */
	public FilterResult filter(TextFilterContextMemory memory, String text, boolean strictMode, String... tags) {
		return filter(memory, text, strictMode, defaultStringBuilder(), tags);
	}

	/**
	 * Filters strings
	 * 
	 * @param memory        Text filter memory object to use
	 * @param text          String to filter
	 * @param strictMode    True for strict-mode filtering, false otherwise
	 * @param stringBuilder Filter string builder to user
	 * @param tags          Tags to use for selecting filter sets
	 * @return Result string
	 */
	public String filterString(TextFilterContextMemory memory, String text, boolean strictMode,
			IResultStringBuilder stringBuilder, String... tags) {
		return filter(memory, text, strictMode, stringBuilder, tags).getFilterResult();
	}

	/**
	 * Filters strings
	 * 
	 * @param memory        Text filter memory object to use
	 * @param text          String to filter
	 * @param strictMode    True for strict-mode filtering, false otherwise
	 * @param stringBuilder Filter string builder to user
	 * @param tags          Tags to use for selecting filter sets
	 * @return FilterResult value
	 */
	public abstract FilterResult filter(TextFilterContextMemory memory, String text, boolean strictMode,
			IResultStringBuilder stringBuilder, String... tags);

	/**
	 * Checks if a string is filtered
	 * 
	 * @param text       String to check
	 * @param strictMode True for strict-mode filtering, false otherwise
	 * @param tags       Tags to use for selecting filter sets
	 * @return True if filtered, false otherwise
	 */
	public boolean isFiltered(String text, boolean strictMode, String... tags) {
		return isFiltered(null, text, strictMode, tags);
	}

	/**
	 * Checks if a string results in a mute
	 * 
	 * @param text String to check
	 * @param tags Tags to use for selecting filter sets
	 * @return True if severely filtered, false otherwise
	 */
	public boolean shouldFilterMute(String text, String... tags) {
		return shouldFilterMute(null, text, tags);
	}

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
	public FilterResult filter(String text, boolean strictMode, String... tags) {
		return filter(text, strictMode, defaultStringBuilder(), tags);
	}

	/**
	 * Filters strings
	 * 
	 * @param text          String to filter
	 * @param strictMode    True for strict-mode filtering, false otherwise
	 * @param stringBuilder Filter string builder to user
	 * @param tags          Tags to use for selecting filter sets
	 * @return Result string
	 */
	public String filterString(String text, boolean strictMode, IResultStringBuilder stringBuilder, String... tags) {
		return filter(text, strictMode, stringBuilder, tags).getFilterResult();
	}

	/**
	 * Filters strings
	 * 
	 * @param text          String to filter
	 * @param strictMode    True for strict-mode filtering, false otherwise
	 * @param stringBuilder Filter string builder to user
	 * @param tags          Tags to use for selecting filter sets
	 * @return FilterResult value
	 */
	public FilterResult filter(String text, boolean strictMode, IResultStringBuilder stringBuilder, String... tags) {
		return filter(null, text, strictMode, stringBuilder, tags);
	}

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
