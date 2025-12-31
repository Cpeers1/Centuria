package org.asf.centuria.textfilter.impl.defparsers;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Consumer;

import org.asf.centuria.textfilter.FilterMode;
import org.asf.centuria.textfilter.FilterSeverity;
import org.asf.centuria.textfilter.IFilterDefParser;
import org.asf.centuria.textfilter.PhraseFilter;
import org.asf.centuria.textfilter.PhraseFilterSet;

public class DefaultFilterDefParser implements IFilterDefParser {

	private class Holder<T> {
		public Holder(T value) {
			this.value = value;
		}

		public T value;
	}

	private String findLineIn(String buffer, Consumer<String> updater) {
		// Go over received content
		if (buffer.contains("\n")) {
			// Pending line found
			String line = buffer.substring(0, buffer.indexOf("\n"));

			// Push remaining bytes to the buffer
			buffer = buffer.substring(buffer.indexOf("\n") + 1);
			updater.accept(buffer);

			// Handle
			return line;
		}
		return null;
	}

	@Override
	public PhraseFilterSet loadFilter(InputStream filterFile, String filePath) throws IOException {
		// Load filter

		// Prepare set fields
		String name = null;
		String desc = null;
		String reason = null;
		FilterSeverity severity = FilterSeverity.NONE;

		// Prepare phrase scanning
		boolean inBlock = false;
		String phrase = null;
		FilterSeverity phraseSeverity = FilterSeverity.NONE;
		String phraseReason = null;
		ArrayList<String> variants = new ArrayList<String>();
		ArrayList<FilterMode> modes = new ArrayList<FilterMode>();
		PhraseFilterSet set = null;

		// Begin reading file
		int ln = 0;
		Holder<String> buffer = new Holder<String>("");
		while (true) {
			// Check buffer
			String line = findLineIn(buffer.value, (res) -> buffer.value = res);
			if (line == null) {
				// No line in buffer currently, continue reading file
				boolean eof = false;
				while (!eof) {
					// Read a number of bytes
					byte[] content = new byte[20480];
					int read = filterFile.read(content, 0, content.length);
					if (read <= -1) {
						// Get next line
						line = findLineIn(buffer.value, (res) -> buffer.value = res);
						if (line == null) {
							// End of file
							eof = true;
						}

						// End loop
						break;
					} else {
						// Trim array
						content = Arrays.copyOfRange(content, 0, read);

						// Update buffer
						String newData = new String(content, "UTF-8");
						buffer.value = buffer.value + newData.replace("\r", "");

						// Update line
						line = findLineIn(buffer.value, (res) -> buffer.value = res);

						// Break if needed
						if (line != null)
							break; // Found line
					}
				}
				if (eof) {
					// End of file reached
					// End loop
					if (line == null && !buffer.value.isEmpty()) {
						line = buffer.value;
						buffer.value = "";
					} else
						break;
				}
			}

			// Increase line count
			ln++;

			// Check syntax
			if (line.startsWith("#") || line.isEmpty())
				continue;
			while (line.startsWith(" "))
				line = line.substring(1);
			if (line.startsWith("#") || line.isEmpty())
				continue;

			// Parse command
			String command = line.trim();
			String args = command;

			// Check syntax
			if (!command.equals("Endphrase") && !command.contains(" ")) {
				throw new IOException("No argument for command " + command + " (" + filePath + ", line " + ln + ")");
			}
			if (!command.equals("Endphrase")) {
				args = args.substring(args.indexOf(" ") + 1).trim();
				if (args.isBlank())
					throw new IOException(
							"No argument for command " + command + " (" + filePath + ", line " + ln + ")");
				command = command.substring(0, command.indexOf(" "));
			}

			// Handle command
			switch (command.toLowerCase()) {

			// Set name
			case "name": {
				name = args;
				break;
			}

			// Tags
			case "add-tag": {
				// Create set if needed
				if (set == null)
					set = new PhraseFilterSet(name, desc, reason);
				set.addTag(args);
				break;
			}

			// Set description
			case "description": {
				desc = args;
				break;
			}

			// Set or phrase reason
			case "reason": {
				if (!inBlock)
					reason = args;
				else
					phraseReason = args;
				break;
			}

			// Set or phrase severity
			case "severity": {
				// Find
				FilterSeverity s;
				switch (args.toLowerCase()) {

				case "staff_highlight": {
					s = FilterSeverity.STAFF_HIGHLIGHT;
					break;
				}

				case "always_filtered": {
					s = FilterSeverity.ALWAYS_FILTERED;
					break;
				}

				case "user_strict_mode": {
					s = FilterSeverity.USER_STRICT_MODE;
					break;
				}

				case "instamute": {
					s = FilterSeverity.INSTAMUTE;
					break;
				}

				default:
					throw new IOException("Invalid severity: " + args + " (" + filePath + ", line " + ln + ")");

				}

				// Assign
				if (inBlock)
					phraseSeverity = s;
				else
					severity = s;
				break;
			}

			// Variant
			case "variant": {
				// Add mode
				if (!inBlock)
					throw new IOException("Unexpected variant command (" + filePath + ", line " + ln + ")");
				variants.add(args);
				break;
			}

			// Mode
			case "mode": {
				// Find
				FilterMode m;
				switch (args.toLowerCase()) {

				case "whole_phrase": {
					m = FilterMode.WHOLE_PHRASE;
					break;
				}

				case "phrase_combined": {
					m = FilterMode.PHRASE_COMBINED;
					break;
				}

				case "word_contains": {
					m = FilterMode.WORD_CONTAINS;
					break;
				}

				case "word_combined": {
					m = FilterMode.WORD_COMBINED;
					break;
				}

				default:
					throw new IOException("Invalid mode: " + args + " (" + filePath + ", line " + ln + ")");

				}

				// Add mode
				if (!inBlock)
					throw new IOException("Unexpected mode command (" + filePath + ", line " + ln + ")");
				modes.add(m);

				break;
			}

			// Create phrase
			case "phrase": {
				// Check
				if (name == null)
					throw new IOException("No set name assigned (" + filePath + ", line " + ln + ")");
				if (desc == null)
					throw new IOException("No set description assigned (" + filePath + ", line " + ln + ")");
				if (reason == null)
					throw new IOException("No set reason assigned (" + filePath + ", line " + ln + ")");

				// Create set if needed
				if (set == null)
					set = new PhraseFilterSet(name, desc, reason);

				// Setup
				phraseSeverity = severity;
				phraseReason = reason;
				phrase = args;
				inBlock = true;
				break;
			}

			// End phrase
			case "endphrase": {
				if (!inBlock)
					throw new IOException("Cannot end a nonexistent phrase (" + filePath + ", line " + ln + ")");

				// Check phrase
				if (phraseSeverity == FilterSeverity.NONE)
					throw new IOException("No phrase severity assigned (" + filePath + ", line " + ln + ")");

				// Check modes
				if (modes.size() == 0)
					throw new IOException("No phrase filter modes assigned (" + filePath + ", line " + ln + ")");

				// Create
				set.addPhraseFilter(phraseSeverity, modes.toArray(t -> new FilterMode[t]), phraseReason, phrase,
						variants.toArray(t -> new String[t]));

				// Reset
				modes.clear();
				variants.clear();
				phraseReason = null;
				phrase = null;
				phraseSeverity = FilterSeverity.NONE;
				inBlock = false;

				break;
			}

			// Load builtin file
			case "loadbuiltin": {
				if (inBlock)
					throw new IOException("Unexpected loadbuiltin command (" + filePath + ", line " + ln + ")");

				// Check
				if (name == null)
					throw new IOException("No set name assigned (" + filePath + ", line " + ln + ")");
				if (desc == null)
					throw new IOException("No set description assigned (" + filePath + ", line " + ln + ")");
				if (reason == null)
					throw new IOException("No set reason assigned (" + filePath + ", line " + ln + ")");

				// Create set if needed
				if (set == null)
					set = new PhraseFilterSet(name, desc, reason);

				// Load parent file
				InputStream strm = null;
				try {
					strm = getClass().getClassLoader().getResourceAsStream("textfilter/" + args);
					PhraseFilterSet filter = loadFilter(strm, "<builtin>/" + args);

					// Load filter phrases onto current
					for (PhraseFilter parentFilter : filter.getFilteredPhrases()) {
						// Add filters
						set.addPhraseFilter(parentFilter.getSeverity(), parentFilter.getModes(),
								parentFilter.getReason(), parentFilter.getPhrase(), parentFilter.getVariants());
					}
				} catch (IOException e) {
					if (strm != null)
						strm.close();
					throw new IOException("An error occurred while loading builtin textfilter file \"" + args + "\"",
							e);
				}
				strm.close();
				break;
			}

			default: {
				throw new IOException("Unrecognized command " + command + " (" + filePath + ", line " + ln + ")");
			}

			}
		}

		// Check
		if (inBlock)
			throw new IOException("Unclosed filter phrase block (" + filePath + ", line " + ln + ")");
		if (name == null)
			throw new IOException("No set name assigned (" + filePath + ", line " + ln + ")");
		if (desc == null)
			throw new IOException("No set description assigned (" + filePath + ", line " + ln + ")");
		if (reason == null)
			throw new IOException("No set reason assigned (" + filePath + ", line " + ln + ")");

		// Create set if needed
		if (set == null)
			set = new PhraseFilterSet(name, desc, reason);
		return set;
	}
}
