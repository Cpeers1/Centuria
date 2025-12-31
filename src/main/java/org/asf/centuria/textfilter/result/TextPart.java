package org.asf.centuria.textfilter.result;

import org.asf.centuria.textfilter.FilterSeverity;
import org.asf.centuria.textfilter.PhraseFilter;

/**
 * 
 * Filter textpart object, message parts in a filtered string, details on a
 * message string and which parts are flagged and which filter exactly triggered
 * the censor
 * 
 * @author Sky Swimmer
 *
 */
public class TextPart {

	private String text;
	private boolean flagged;

	private FilterSeverity severity;
	private PhraseFilter filter;

	private String reason;
	private WordMatch[] matchedWords;

	public TextPart(String text, boolean flagged, FilterSeverity severity, PhraseFilter filter, String reason,
			WordMatch[] matchedWords) {
		this.text = text;
		this.flagged = flagged;
		this.severity = severity;
		this.filter = filter;
		this.reason = reason;
		this.matchedWords = matchedWords;
	}

	/**
	 * The text of this TextPart instance
	 * 
	 * @return Text string
	 */
	public String getText() {
		return text;
	}

	/**
	 * The censor state of the text part value
	 * 
	 * @return True if flagged, false otherwise
	 */
	public boolean isFlagged() {
		return flagged;
	}

	/**
	 * Retrieves the phrase filter
	 * 
	 * @return PhraseFilter instance, null if not flagged
	 */
	public PhraseFilter getPhraseFilter() {
		return filter;
	}

	/**
	 * Retrieves filter severity
	 * 
	 * @return Filtering severity, NONE if not flagged
	 */
	public FilterSeverity getSeverity() {
		return severity;
	}

	/**
	 * Retrieves the main reason for filtering
	 * 
	 * @return Filtering reason, null if not flagged
	 */
	public String getPrimaryFilteringReason() {
		return reason;
	}

	/**
	 * Retrieves the list of matched words
	 * 
	 * @return Array of WordMatch instances, null if not flagged
	 */
	public WordMatch[] getMatchedWords() {
		return matchedWords.clone();
	}

	public String toString() {
		return "TextPart: [" + text + ", flagged: " + flagged + ", severity: " + severity + ", reason: " + reason + "]";
	}

}
