package org.asf.centuria.textfilter;

import java.io.IOException;
import java.io.InputStream;

/**
 * 
 * Textfilter Definition File Parser
 * 
 * @author Sky Swimmer
 * 
 */
public interface IFilterDefParser {

	/**
	 * Called to load a filter set from a stream
	 * 
	 * @param filterFile Filter file stream, note: this does not close the stream,
	 *                   and only reads from it
	 * @param filePath   Filter file path used in errors
	 * @return PhraseFilterSet instance
	 * @throws IOException If loading the filter fails
	 */
	public PhraseFilterSet loadFilter(InputStream filterFile, String filePath) throws IOException;

}
