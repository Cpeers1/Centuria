package org.asf.centuria.textfilter;

import java.util.stream.Stream;

import org.asf.centuria.textfilter.context.IContextExpression;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * 
 * Phrase and word filtering
 * 
 * @author Sky Swimmer
 *
 */
public class PhraseFilter {

	private PhraseFilterSet set;

	private String phrase;
	private String reason;

	private String[] variants;
	private FilterSeverity severity;
	private FilterMode[] modes;

	private IContextExpression[] contextInclude;
	private IContextExpression[] contextExclude;
	private IContextExpression[] contextIncludeGlobal;
	private IContextExpression[] contextExcludeGlobal;

	public PhraseFilter(PhraseFilterSet set, FilterMode[] modes, IContextExpression[] contextInclude,
			IContextExpression[] contextExclude, IContextExpression[] contextIncludeGlobal,
			IContextExpression[] contextExcludeGlobal, String phrase, String[] variants, String reason,
			FilterSeverity severity) {
		this.set = set;
		this.phrase = phrase;
		this.variants = variants;
		this.reason = reason;
		this.severity = severity;
		this.modes = modes;
		this.contextInclude = contextInclude;
		this.contextExclude = contextExclude;
		this.contextIncludeGlobal = contextIncludeGlobal;
		this.contextExcludeGlobal = contextExcludeGlobal;
	}

	public void updateContexts(IContextExpression[] contextInclude, IContextExpression[] contextExclude,
			IContextExpression[] contextIncludeGlobal, IContextExpression[] contextExcludeGlobal) {
		this.contextInclude = contextInclude;
		this.contextExclude = contextExclude;
		this.contextIncludeGlobal = contextIncludeGlobal;
		this.contextExcludeGlobal = contextExcludeGlobal;
	}

	/**
	 * Retrieves the context-include expressions
	 * 
	 * @return Array of IContextExpression instances
	 */
	public IContextExpression[] getGlobalContextIncludeExpressions() {
		return contextIncludeGlobal.clone();
	}

	/**
	 * Retrieves the context-include expressions
	 * 
	 * @return Array of IContextExpression instances
	 */
	public IContextExpression[] getGlobalContextExcludeExpressions() {
		return contextExcludeGlobal.clone();
	}

	/**
	 * Retrieves the context-include expressions
	 * 
	 * @return Array of IContextExpression instances
	 */
	public IContextExpression[] getContextIncludeExpressions() {
		return contextInclude.clone();
	}

	/**
	 * Retrieves the context-include expressions
	 * 
	 * @return Array of IContextExpression instances
	 */
	public IContextExpression[] getContextExcludeExpressions() {
		return contextExclude.clone();
	}

	/**
	 * Retrieves filtering modes
	 * 
	 * @return Array of FilterMode values
	 */
	public FilterMode[] getModes() {
		return modes.clone();
	}

	/**
	 * Adds filter mode
	 * 
	 * @param mode Mode to add
	 */
	public void addMode(FilterMode mode) {
		if (hasMode(mode))
			return;
		FilterMode[] modesU = new FilterMode[modes.length];
		for (int i = 0; i < modes.length; i++)
			modesU[i] = modes[i];
		modesU[modes.length] = mode;
		modes = modesU;
	}

	/**
	 * Checks if a filtering mode is present
	 * 
	 * @param mode Filter mode
	 * @return True if present, false otherwise
	 */
	public boolean hasMode(FilterMode mode) {
		return Stream.of(modes).anyMatch(m -> m == mode);
	}

	/**
	 * Adds variants to the filter
	 * 
	 * @param variant Variant to add
	 */
	public void addVariant(String variant) {
		String[] variantsU = new String[variants.length];
		for (int i = 0; i < variants.length; i++)
			variantsU[i] = variants[i];
		variantsU[variants.length] = variant;
		variants = variantsU;
	}

	/**
	 * Retrieves filter severity
	 * 
	 * @return Filtering severity
	 */
	public FilterSeverity getSeverity() {
		return severity;
	}

	/**
	 * Retrieves the phrase or word that should be filtered
	 * 
	 * @return Filtered word or phrase
	 */
	public String getPhrase() {
		return phrase;
	}

	/**
	 * Retrieves word variants
	 * 
	 * @return Word variants
	 */
	public String[] getVariants() {
		return variants.clone();
	}

	/**
	 * Retrieves the reason for filtering
	 * 
	 * @return Filtering reason
	 */
	public String getReason() {
		return reason;
	}

	/**
	 * Retrieves the phrase filter set
	 * 
	 * @return Filter set
	 */
	@JsonIgnore
	public PhraseFilterSet getSet() {
		return set;
	}

	/**
	 * Retrieves all phrases that should be filtered
	 * 
	 * @return Array of phrase strings
	 */
	public String[] getAllPhrases() {
		String[] v = getVariants();
		String[] v2 = new String[v.length + 1];
		for (int i = 0; i < v.length; i++)
			v2[i] = v[i];
		v2[v.length] = getPhrase();
		return v2;
	}

}
