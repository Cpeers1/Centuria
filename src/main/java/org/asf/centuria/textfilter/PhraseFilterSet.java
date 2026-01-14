package org.asf.centuria.textfilter;

import java.util.ArrayList;
import java.util.Map;

import org.asf.centuria.textfilter.context.IContextExpression;

/**
 * 
 * Set of filtered phrases
 * 
 * @author Sky Swimmer
 *
 */
public class PhraseFilterSet {

	private String setName;
	private String setDescription;
	private String filterReason;

	private ArrayList<PhraseFilter> phrases = new ArrayList<PhraseFilter>();
	private ArrayList<ContextPhrase> contextPhrases = new ArrayList<ContextPhrase>();
	private ArrayList<String> tags = new ArrayList<String>();

	public PhraseFilterSet(String name, String description, String reason) {
		this.setName = name;
		this.setDescription = description;
		this.filterReason = reason;
	}

	/**
	 * Adds phrase filters
	 * 
	 * @param severity Filter severity
	 * @param modes    Filter modes
	 * @param phrase   Phrase to filter
	 * @return PhraseFilter instance
	 */
	public PhraseFilter addPhraseFilter(FilterSeverity severity, FilterMode[] modes,
			IContextExpression[] contextInclude, String phrase) {
		return addPhraseFilter(severity, modes, contextInclude, new IContextExpression[0], filterReason, phrase);
	}

	/**
	 * Adds phrase filters
	 * 
	 * @param severity Filter severity
	 * @param modes    Filter modes
	 * @param reason   Filter reason
	 * @param phrase   Phrase to filter
	 * @param variants Variants
	 * @return PhraseFilter instance
	 */
	public PhraseFilter addPhraseFilter(FilterSeverity severity, FilterMode[] modes,
			IContextExpression[] contextInclude, String reason, String phrase, String... variants) {
		PhraseFilter filter = new PhraseFilter(this, modes, contextInclude, new IContextExpression[0],
				new IContextExpression[0], new IContextExpression[0], phrase, variants, reason, severity);
		phrases.add(filter);
		return filter;
	}

	/**
	 * Adds phrase filters
	 * 
	 * @param severity Filter severity
	 * @param modes    Filter modes
	 * @param phrase   Phrase to filter
	 * @return PhraseFilter instance
	 */
	public PhraseFilter addPhraseFilter(FilterSeverity severity, FilterMode[] modes,
			IContextExpression[] contextInclude, IContextExpression[] contextExclude, String phrase) {
		return addPhraseFilter(severity, modes, contextInclude, contextExclude, filterReason, phrase);
	}

	/**
	 * Adds phrase filters
	 * 
	 * @param severity Filter severity
	 * @param modes    Filter modes
	 * @param reason   Filter reason
	 * @param phrase   Phrase to filter
	 * @param variants Variants
	 * @return PhraseFilter instance
	 */
	public PhraseFilter addPhraseFilter(FilterSeverity severity, FilterMode[] modes,
			IContextExpression[] contextInclude, IContextExpression[] contextExclude, String reason, String phrase,
			String... variants) {
		PhraseFilter filter = new PhraseFilter(this, modes, contextInclude, contextExclude, new IContextExpression[0],
				new IContextExpression[0], phrase, variants, reason, severity);
		phrases.add(filter);
		return filter;
	}

	/**
	 * Adds phrase filters
	 * 
	 * @param severity Filter severity
	 * @param modes    Filter modes
	 * @param reason   Filter reason
	 * @param phrase   Phrase to filter
	 * @param variants Variants
	 * @return PhraseFilter instance
	 */
	public PhraseFilter addPhraseFilter(FilterSeverity severity, FilterMode[] modes,
			IContextExpression[] contextInclude, IContextExpression[] contextExclude,
			IContextExpression[] contextIncludeGlobal, IContextExpression[] contextExcludeGlobal, String reason,
			String phrase, String... variants) {
		PhraseFilter filter = new PhraseFilter(this, modes, contextInclude, contextExclude, contextIncludeGlobal,
				contextExcludeGlobal, phrase, variants, reason, severity);
		phrases.add(filter);
		return filter;
	}

	/**
	 * Adds phrase filters
	 * 
	 * @param severity Filter severity*
	 * @param modes    Filter modes*
	 * @param phrase   Phrase to filter*@return PhraseFilter instance
	 */

	public PhraseFilter addPhraseFilter(FilterSeverity severity, FilterMode[] modes,
			IContextExpression[] contextInclude, IContextExpression[] contextExclude,
			IContextExpression[] contextIncludeGlobal, IContextExpression[] contextExcludeGlobal, String phrase) {
		return addPhraseFilter(severity, modes, contextInclude, contextExclude, filterReason, phrase);
	}

	/**
	 * Adds phrase filters
	 * 
	 * @param severity Filter severity
	 * @param modes    Filter modes
	 * @param phrase   Phrase to filter
	 * @return PhraseFilter instance
	 */
	public PhraseFilter addPhraseFilter(FilterSeverity severity, FilterMode[] modes, String phrase) {
		return addPhraseFilter(severity, modes, filterReason, phrase);
	}

	/**
	 * Adds phrase filters
	 * 
	 * @param severity Filter severity
	 * @param modes    Filter modes
	 * @param reason   Filter reason
	 * @param phrase   Phrase to filter
	 * @param variants Variants
	 * @return PhraseFilter instance
	 */
	public PhraseFilter addPhraseFilter(FilterSeverity severity, FilterMode[] modes, String reason, String phrase,
			String... variants) {
		PhraseFilter filter = new PhraseFilter(this, modes, new IContextExpression[0], new IContextExpression[0],
				new IContextExpression[0], new IContextExpression[0], phrase, variants, reason, severity);
		phrases.add(filter);
		return filter;
	}

	/**
	 * Adds phrase definitions to the context system
	 * 
	 * @param defaultModes    Default modes for the phrase entry for the context
	 *                        system
	 * @param onlyMatchOnFlag True to only make this phrase flag if the text filter
	 *                        is flagging the context phrase, false to flag
	 *                        regardless
	 * @param phrase          Phrase to filter
	 * @return PhraseFilter instance
	 */
	public ContextPhrase addContextPhrase(FilterMode[] defaultModes, boolean onlyMatchOnFlag, String phrase) {
		return addContextPhrase(defaultModes, onlyMatchOnFlag, filterReason, phrase);
	}

	/**
	 * Adds phrase definitions to the context system
	 * 
	 * @param defaultModes    Default modes for the phrase entry for the context
	 *                        system
	 * @param onlyMatchOnFlag True to only make this phrase flag if the text filter
	 *                        is flagging the context phrase, false to flag
	 *                        regardless
	 * @param phrase          Phrase to filter
	 * @param variants        Variants
	 * @return PhraseFilter instance
	 */
	public ContextPhrase addContextPhrase(FilterMode[] defaultModes, boolean onlyMatchOnFlag, String phrase,
			Map<String, FilterMode[]> variants) {
		ContextPhrase filter = new ContextPhrase(defaultModes, phrase, variants, onlyMatchOnFlag);
		contextPhrases.add(filter);
		return filter;
	}

	/**
	 * Adds phrase definitions to the context system
	 * 
	 * @param defaultModes    Default modes for the phrase entry for the context
	 *                        system
	 * @param onlyMatchOnFlag True to only make this phrase flag if the text filter
	 *                        is flagging the context phrase, false to flag
	 *                        regardless
	 * @param phrase          Phrase to filter
	 * @param variants        Variants
	 * @return PhraseFilter instance
	 */
	public ContextPhrase addContextPhrase(FilterMode[] defaultModes, boolean onlyMatchOnFlag, String phrase,
			String... variants) {
		ContextPhrase filter = new ContextPhrase(defaultModes, phrase, variants, onlyMatchOnFlag);
		contextPhrases.add(filter);
		return filter;
	}

	/**
	 * Retrieves filtered phrases
	 * 
	 * @return Array of PhraseFilter instances
	 */
	public PhraseFilter[] getFilteredPhrases() {
		return phrases.toArray(t -> new PhraseFilter[t]);
	}

	/**
	 * Retrieves the list of context phrase blocks
	 * 
	 * @return Array of ContextPhrase instances
	 */
	public ContextPhrase[] getContextPhrases() {
		return contextPhrases.toArray(t -> new ContextPhrase[t]);
	}

	/**
	 * Adds tags, when tags are added, this filter will only be used when the
	 * request tags contains any tag of this set
	 * 
	 * @param tag Tag to add
	 */
	public void addTag(String tag) {
		tags.add(tag);
	}

	/**
	 * Retrieves filter set tags, if there are tags present, this filter will only
	 * be used when the request tags contains any tag of this set
	 * 
	 * @return Array of of tag strings
	 */
	public String[] getSetTags() {
		return tags.toArray(t -> new String[t]);
	}

	/**
	 * Retrieves the set name
	 * 
	 * @return Set name string
	 */
	public String getSetName() {
		return setName;
	}

	/**
	 * Retrieves the set description
	 * 
	 * @return Set description string
	 */
	public String getSetDescription() {
		return setDescription;
	}

	/**
	 * Retrieves the reason string
	 * 
	 * @return Filter reason string
	 */
	public String getFilteringReason() {
		return filterReason;
	}

}
