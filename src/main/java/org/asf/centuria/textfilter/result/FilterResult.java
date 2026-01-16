package org.asf.centuria.textfilter.result;

import org.asf.centuria.textfilter.FilterSeverity;

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

	private String filteredResult;
	private String primaryReason = null;

	public FilterResult(WordMatch[] matches, String filteredResult) {
		this.matchedFilters = matches;
		this.filteredResult = filteredResult;

		resultSeverity = FilterSeverity.NONE;
		for (WordMatch match : matches) {
			if (match.getSeverity().ordinal() > resultSeverity.ordinal()) {
				resultSeverity = match.getSeverity();
				primaryReason = match.getReason();
			}
		}
	}

	public FilterResult(WordMatch[] matches, String filteredResult, FilterSeverity resultSeverity) {
		this.matchedFilters = matches;
		this.filteredResult = filteredResult;
		this.resultSeverity = resultSeverity;
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
