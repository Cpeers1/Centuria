package org.asf.centuria.textfilter.result;

import org.asf.centuria.textfilter.FilterSeverity;
import org.asf.centuria.textfilter.PhraseFilter;

/**
 * 
 * Word match information
 * 
 * @author Sky Swimmer
 *
 */
public class WordMatch {

	private String phrase;
	private String reason;

	private String[] variants;
	private FilterSeverity severity;

	private PhraseFilter filter;

	public WordMatch(PhraseFilter filter, String phrase) {
		this.phrase = phrase;
		String[] v = filter.getVariants();
		this.variants = new String[v.length + 1];
		for (int i = 0; i < v.length; i++)
			this.variants[i] = v[i];
		this.variants[v.length] = filter.getPhrase();
		this.reason = filter.getReason();
		this.severity = filter.getSeverity();
		this.filter = filter;
	}

	public WordMatch(PhraseFilter filter, String phrase, String reason, String[] variants, FilterSeverity severity) {
		this.phrase = phrase;
		this.variants = variants;
		this.reason = reason;
		this.severity = severity;
		this.filter = filter;
	}

	/**
	 * Retrieves the phrase filter
	 * 
	 * @return PhraseFilter instance
	 */
	public PhraseFilter getPhraseFilter() {
		return filter;
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
	 * Retrieves the phrase or word that was matched
	 * 
	 * @return Filtered word or phrase
	 */
	public String getMatchedPhrase() {
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

}
