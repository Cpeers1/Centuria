package org.asf.centuria.textfilter;

import org.asf.centuria.textfilter.result.TextPart;

public interface IResultStringBuilder {

	/**
	 * Called to build the output filtered string for filter results
	 * 
	 * @param parts Array of text parts
	 * @return Output filtered string
	 */
	public String buildOutputString(TextPart[] parts);

}
