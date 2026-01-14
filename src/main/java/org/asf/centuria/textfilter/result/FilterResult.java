package org.asf.centuria.textfilter.result;

import org.asf.centuria.textfilter.FilterSeverity;
import org.asf.centuria.textfilter.IResultStringBuilder;

/**
 * 
 * Filtering result object
 * 
 * @author Sky Swimmer
 *
 */
public class FilterResult {

	private FilterSeverity resultSeverity;
	private WordMatch[] matchedFilters;

	private String originalText;
	private String filteredResult;
	private TextPart[] textParts;
	private String primaryReason = null;

	private long time;

	public FilterResult(String originalText, WordMatch[] matches, TextPart[] textParts, String filteredResult) {
		this.originalText = originalText;
		this.matchedFilters = matches;
		this.textParts = textParts;
		this.filteredResult = filteredResult;

		resultSeverity = FilterSeverity.NONE;
		for (WordMatch match : matches) {
			if (match.getSeverity().ordinal() > resultSeverity.ordinal()) {
				resultSeverity = match.getSeverity();
				primaryReason = match.getReason();
			}
		}

		time = System.currentTimeMillis();
	}

	public FilterResult(String originalText, WordMatch[] matches, TextPart[] textParts, String filteredResult,
			FilterSeverity resultSeverity) {
		this.originalText = originalText;
		this.matchedFilters = matches;
		this.textParts = textParts;
		this.filteredResult = filteredResult;
		this.resultSeverity = resultSeverity;

		time = System.currentTimeMillis();
	}

	/**
	 * Retrieves the original text string of the message
	 * 
	 * @return Original text prior to filtering
	 */
	public String getOriginalText() {
		return originalText;
	}

	/**
	 * Retrieves the timestamp the result object was created
	 * 
	 * @return Result object creation timestamp in milliseconds
	 */
	public long getResultTime() {
		return time;
	}

	/**
	 * Retrieves the primary reason for the filter to be applied, or null if it
	 * cannot be determined
	 * 
	 * @return Primary reason for filtering or null
	 */
	public String getPrimaryFilterReason() {
		return primaryReason;
	}

	/**
	 * Retrieves the filtered text
	 * 
	 * @return Filtered string
	 */
	public String getFilterResult() {
		return filteredResult;
	}

	/**
	 * Retrieves the filtered text
	 * 
	 * @param stringBuilder String builder to use to build the filter result string
	 * @return Filtered string
	 */
	public String getFilterResult(IResultStringBuilder stringBuilder) {
		return stringBuilder.buildOutputString(textParts);
	}

	/**
	 * Retrieves the filter text parts
	 * 
	 * @return Array of TextPart instances
	 */
	public TextPart[] getTextParts() {
		return textParts.clone();
	}

	/**
	 * Retrieves filter severity
	 * 
	 * @return FilterSeverity value
	 */
	public FilterSeverity getSeverity() {
		return resultSeverity;
	}

	/**
	 * Retrieves matched filters
	 * 
	 * @return Array of WordMatch instances
	 */
	public WordMatch[] getMatches() {
		return matchedFilters.clone();
	}

	/**
	 * Checks if the filter was matched
	 * 
	 * @return True if matched, false otherwise
	 */
	public boolean isMatch() {
		return resultSeverity != FilterSeverity.NONE;
	}

}
