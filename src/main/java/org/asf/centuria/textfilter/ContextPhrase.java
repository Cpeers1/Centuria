package org.asf.centuria.textfilter;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * 
 * Context phrase object, variants for phrases and such
 * 
 * @author Sky Swimmer
 *
 */
public class ContextPhrase {

	private FilterMode[] modesDefault;
	private String phrase;
	private HashMap<String, FilterMode[]> variants = new HashMap<String, FilterMode[]>();
	private boolean onlyMatchOnFlag;

	public ContextPhrase(FilterMode[] modes, String phrase, String[] variants, boolean onlyMatchOnFlag) {
		this.phrase = phrase;
		for (String variant : variants)
			this.variants.put(variant, null);
		this.variants.put(phrase, modes);
		this.modesDefault = modes;
		this.onlyMatchOnFlag = onlyMatchOnFlag;
	}

	public ContextPhrase(FilterMode[] modes, String phrase, Map<String, FilterMode[]> variants,
			boolean onlyMatchOnFlag) {
		this.phrase = phrase;
		for (String variant : variants.keySet())
			this.variants.put(variant, variants.get(variant));
		this.variants.put(phrase, modes);
		this.modesDefault = modes;
		this.onlyMatchOnFlag = onlyMatchOnFlag;
	}

	/**
	 * Adds filter mode to the default filter modes
	 * 
	 * @param mode Mode to add
	 */
	public void addDefaultMode(FilterMode mode) {
		if (hasDefaultMode(mode))
			return;
		FilterMode[] modesU = new FilterMode[modesDefault.length];
		for (int i = 0; i < modesDefault.length; i++)
			modesU[i] = modesDefault[i];
		modesU[modesDefault.length] = mode;
		modesDefault = modesU;
	}

	/**
	 * Checks if a filtering mode is present
	 * 
	 * @param mode Filter mode
	 * @return True if present, false otherwise
	 */
	public boolean hasDefaultMode(FilterMode mode) {
		return Stream.of(modesDefault).anyMatch(m -> m == mode);
	}

	/**
	 * Determines if this ContextPhrase should only be considered if the text filter
	 * flagged the phrase
	 * 
	 * @return True if onlyMatchOnFlag is on, false otherwise
	 */
	public boolean shouldOnlyMatchOnFilterFlag() {
		return onlyMatchOnFlag;
	}

	/**
	 * Adds variants to the filter
	 * 
	 * @param modes   Variant modes
	 * @param variant Variant to add
	 */
	public void addVariant(FilterMode[] modes, String variant) {
		variants.put(variant, null);
	}

	/**
	 * Adds variants to the filter
	 * 
	 * @param variant Variant to add
	 */
	public void addVariant(String variant) {
		variants.put(variant, null);
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
	 * Retrieves the filter modes for a specific variant
	 * 
	 * @param variant Variant to retrieve the filter mode for
	 * @return FilterMode value
	 */
	public FilterMode[] getModesFor(String variant) {
		if (variants.containsKey(variant)) {
			FilterMode[] modes = variants.get(variant);
			if (modes != null)
				return modes;
		}
		return modesDefault; // Fallback
	}

	/**
	 * Retrieves word variants
	 * 
	 * @return Word variants
	 */
	public String[] getVariants() {
		return variants.keySet().toArray(t -> new String[t]);
	}

	/**
	 * Retrieves all phrases that should be filtered
	 * 
	 * @return Array of phrase strings
	 */
	public String[] getAllPhrases() {
		String[] v = getVariants();
		String[] v2 = new String[v.length + 1];
		v2[0] = getPhrase();
		for (int i = 0; i < v.length; i++)
			v2[i + 1] = v[i];
		return v2;
	}

}
