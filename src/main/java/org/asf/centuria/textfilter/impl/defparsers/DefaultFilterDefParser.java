package org.asf.centuria.textfilter.impl.defparsers;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.function.Consumer;

import org.asf.centuria.textfilter.ContextPhrase;
import org.asf.centuria.textfilter.FilterMode;
import org.asf.centuria.textfilter.FilterSeverity;
import org.asf.centuria.textfilter.IFilterDefParser;
import org.asf.centuria.textfilter.PhraseFilter;
import org.asf.centuria.textfilter.PhraseFilterSet;
import org.asf.centuria.textfilter.context.ContextCompareExpression;
import org.asf.centuria.textfilter.context.ContextEncasedExpression;
import org.asf.centuria.textfilter.context.ContextExpressionOperator;
import org.asf.centuria.textfilter.context.ContextResolvableWrappedPhrase;
import org.asf.centuria.textfilter.context.ContextWrappedPhrase;
import org.asf.centuria.textfilter.context.IContextExpression;

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
		boolean inContextPhrase = false;
		String phrase = null;
		FilterSeverity phraseSeverity = FilterSeverity.NONE;
		String phraseReason = null;
		ArrayList<String> variants = new ArrayList<String>();
		boolean onlyMatchOnFlag = false;
		ArrayList<IContextExpression> contextIncludeLocal = new ArrayList<IContextExpression>();
		ArrayList<IContextExpression> contextIncludeGlobal = new ArrayList<IContextExpression>();
		ArrayList<IContextExpression> contextExcludeLocal = new ArrayList<IContextExpression>();
		ArrayList<IContextExpression> contextExcludeGlobal = new ArrayList<IContextExpression>();
		boolean contextIncludeLocalAll = false;
		boolean contextIncludeGlobalAll = false;
		boolean contextExcludeLocalAll = false;
		boolean contextExcludeGlobalAll = false;
		ArrayList<PhraseFilter> filtersToPopulateContextIncludeAllLocal = new ArrayList<PhraseFilter>();
		ArrayList<PhraseFilter> filtersToPopulateContextIncludeAllGlobal = new ArrayList<PhraseFilter>();
		ArrayList<PhraseFilter> filtersToPopulateContextExcludeAllLocal = new ArrayList<PhraseFilter>();
		ArrayList<PhraseFilter> filtersToPopulateContextExcludeAllGlobal = new ArrayList<PhraseFilter>();
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
						buffer.value = buffer.value + newData;

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
			line = line.replace("\r", "").replace("\t", "    ");

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
			while (args.endsWith(" "))
				args = args.substring(0, args.length() - 1);

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
				// Add variant
				if (!inBlock)
					throw new IOException("Unexpected variant command (" + filePath + ", line " + ln + ")");
				variants.add(args);
				break;
			}

			// Context include
			case "context-include": {
				// Add include
				if (inContextPhrase)
					throw new IOException("Unexpected context-include command (" + filePath + ", line " + ln + ")");
				if (!inBlock)
					throw new IOException("Unexpected context-include command (" + filePath + ", line " + ln + ")");
				String scope = args;
				if (!scope.contains(" "))
					throw new IOException("Missing expression after scope in context-include command (" + filePath
							+ ", line " + ln + ")");
				scope = scope.substring(0, scope.indexOf(" ")).trim().toUpperCase();
				args = args.substring(args.indexOf(" ") + 1).trim();
				if (!scope.equals("GLOBAL") && !scope.equals("LOCAL"))
					throw new IOException(
							"Unrecognized scope " + scope + " in context-include command, expected LOCAL or GLOBAL ("
									+ filePath + ", line " + ln + ")");

				// Parse expression
				if (args.equals("*")) {
					// Add all local items for context sensitivity
					if (scope.equalsIgnoreCase("local"))
						contextIncludeLocalAll = true;
					else
						contextIncludeGlobalAll = true;
				} else {
					IContextExpression expr = parseExpression(args, filePath, ln, set);
					if (scope.equalsIgnoreCase("local"))
						contextIncludeLocal.add(expr);
					else
						contextIncludeGlobal.add(expr);
				}
				break;
			}

			// Context exclude
			case "context-exclude": {
				// Add exclude
				if (inContextPhrase)
					throw new IOException("Unexpected context-exclude command (" + filePath + ", line " + ln + ")");
				if (!inBlock)
					throw new IOException("Unexpected context-exclude command (" + filePath + ", line " + ln + ")");
				String scope = args;
				if (!scope.contains(" "))
					throw new IOException("Missing expression after scope in context-exclude command (" + filePath
							+ ", line " + ln + ")");
				scope = scope.substring(0, scope.indexOf(" ")).trim().toUpperCase();
				args = args.substring(args.indexOf(" ") + 1).trim();
				if (!scope.equals("GLOBAL") && !scope.equals("LOCAL"))
					throw new IOException(
							"Unrecognized scope " + scope + " in context-exclude command, expected LOCAL or GLOBAL ("
									+ filePath + ", line " + ln + ")");

				// Parse expression
				if (args.equals("*")) {
					// Add all local items for context sensitivity
					if (scope.equalsIgnoreCase("local"))
						contextExcludeLocalAll = true;
					else
						contextExcludeGlobalAll = true;
				} else {
					IContextExpression expr = parseExpression(args, filePath, ln, set);
					if (scope.equalsIgnoreCase("local"))
						contextExcludeLocal.add(expr);
					else
						contextExcludeGlobal.add(expr);
				}
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
				if (inBlock)
					throw new IOException("Unexpected phrase command (" + filePath + ", line " + ln + ")");

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
				if (inContextPhrase)
					throw new IOException("Unexpected endphrase command (" + filePath + ", line " + ln + ")");
				if (!inBlock)
					throw new IOException("Cannot end a nonexistent phrase (" + filePath + ", line " + ln + ")");

				// Check phrase
				if (phraseSeverity == FilterSeverity.NONE)
					throw new IOException("No phrase severity assigned (" + filePath + ", line " + ln + ")");

				// Check modes
				if (modes.size() == 0)
					throw new IOException("No phrase filter modes assigned (" + filePath + ", line " + ln + ")");

				// Create
				PhraseFilter filter = set.addPhraseFilter(phraseSeverity, modes.toArray(t -> new FilterMode[t]),
						contextIncludeLocal.toArray(t -> new IContextExpression[t]),
						contextExcludeLocal.toArray(t -> new IContextExpression[t]),
						contextIncludeGlobal.toArray(t -> new IContextExpression[t]),
						contextExcludeGlobal.toArray(t -> new IContextExpression[t]), phraseReason, phrase,
						variants.toArray(t -> new String[t]));
				if (contextIncludeLocalAll)
					filtersToPopulateContextIncludeAllLocal.add(filter);
				if (contextIncludeGlobalAll)
					filtersToPopulateContextIncludeAllGlobal.add(filter);
				if (contextExcludeLocalAll)
					filtersToPopulateContextExcludeAllLocal.add(filter);
				if (contextExcludeGlobalAll)
					filtersToPopulateContextExcludeAllGlobal.add(filter);

				// Reset
				modes.clear();
				variants.clear();
				phraseReason = null;
				phrase = null;
				phraseSeverity = FilterSeverity.NONE;
				inBlock = false;
				contextIncludeLocalAll = false;
				contextIncludeGlobalAll = false;
				contextExcludeLocalAll = false;
				contextExcludeGlobalAll = false;
				contextIncludeLocal.clear();
				contextExcludeLocal.clear();
				contextIncludeGlobal.clear();
				contextExcludeGlobal.clear();

				break;
			}

			// Create phrase
			case "contextphrase": {
				if (inBlock)
					throw new IOException("Unexpected contextphrase command (" + filePath + ", line " + ln + ")");

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
				phrase = args;
				inBlock = true;
				inContextPhrase = true;
				break;
			}
			// End phrase
			case "context-require-filterflag": {
				if (!inContextPhrase)
					throw new IOException("Unexpected endcontextphrase command (" + filePath + ", line " + ln + ")");
				switch (args.toLowerCase()) {

				case "true": {
					break;
				}

				case "false": {
					break;
				}

				default:
					throw new IOException("Invalid tate: " + args + " (" + filePath + ", line " + ln + ")");

				}
				onlyMatchOnFlag = args.equalsIgnoreCase("true");
				break;
			}

			// End phrase
			case "endcontextphrase": {
				if (!inContextPhrase)
					throw new IOException("Unexpected endcontextphrase command (" + filePath + ", line " + ln + ")");
				if (!inBlock)
					throw new IOException("Cannot end a nonexistent phrase (" + filePath + ", line " + ln + ")");

				// Check modes
				if (modes.size() == 0)
					throw new IOException("No phrase filter modes assigned (" + filePath + ", line " + ln + ")");

				// Create
				set.addContextPhrase(modes.toArray(t -> new FilterMode[t]), onlyMatchOnFlag, phrase,
						variants.toArray(t -> new String[t]));

				// Reset
				modes.clear();
				variants.clear();
				phrase = null;
				inBlock = false;
				onlyMatchOnFlag = false;
				inContextPhrase = false;
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
								parentFilter.getContextIncludeExpressions(),
								parentFilter.getContextExcludeExpressions(),
								parentFilter.getGlobalContextIncludeExpressions(),
								parentFilter.getGlobalContextExcludeExpressions(),
								filter.getFilteringReason().equals(parentFilter.getReason()) ? reason
										: parentFilter.getReason(),
								parentFilter.getPhrase(), parentFilter.getVariants());
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

		// Populate
		for (PhraseFilter filter : filtersToPopulateContextIncludeAllLocal) {
			ArrayList<IContextExpression> exprs = new ArrayList<IContextExpression>();
			for (IContextExpression expr : filter.getContextIncludeExpressions())
				exprs.add(expr);
			for (PhraseFilter f : set.getFilteredPhrases()) {
				if (f.getContextIncludeExpressions().length == 0 && f.getContextExcludeExpressions().length == 0
						&& !filtersToPopulateContextIncludeAllLocal.contains(f)) {
					exprs.add(new ContextWrappedPhrase(
							new ContextPhrase(f.getModes(), f.getPhrase(), f.getVariants(), false)));
				}
			}
			filter.updateContexts(exprs.toArray(t -> new IContextExpression[t]), filter.getContextExcludeExpressions(),
					filter.getGlobalContextIncludeExpressions(), filter.getGlobalContextExcludeExpressions());
		}
		for (PhraseFilter filter : filtersToPopulateContextIncludeAllGlobal) {
			ArrayList<IContextExpression> exprs = new ArrayList<IContextExpression>();
			for (IContextExpression expr : filter.getGlobalContextIncludeExpressions())
				exprs.add(expr);
			for (PhraseFilter f : set.getFilteredPhrases()) {
				if (f.getContextIncludeExpressions().length == 0 && f.getContextExcludeExpressions().length == 0
						&& !filtersToPopulateContextIncludeAllGlobal.contains(f)) {
					exprs.add(new ContextWrappedPhrase(
							new ContextPhrase(f.getModes(), f.getPhrase(), f.getVariants(), false)));
				}
			}
			filter.updateContexts(filter.getContextIncludeExpressions(), filter.getContextExcludeExpressions(),
					exprs.toArray(t -> new IContextExpression[t]), filter.getGlobalContextExcludeExpressions());
		}
		for (PhraseFilter filter : filtersToPopulateContextExcludeAllLocal) {
			ArrayList<IContextExpression> exprs = new ArrayList<IContextExpression>();
			for (IContextExpression expr : filter.getContextExcludeExpressions())
				exprs.add(expr);
			for (PhraseFilter f : set.getFilteredPhrases()) {
				if (f.getContextExcludeExpressions().length == 0 && f.getContextExcludeExpressions().length == 0
						&& !filtersToPopulateContextExcludeAllLocal.contains(f)) {
					exprs.add(new ContextWrappedPhrase(
							new ContextPhrase(f.getModes(), f.getPhrase(), f.getVariants(), false)));
				}
			}
			filter.updateContexts(filter.getContextIncludeExpressions(), exprs.toArray(t -> new IContextExpression[t]),
					filter.getGlobalContextExcludeExpressions(), filter.getGlobalContextExcludeExpressions());
		}
		for (PhraseFilter filter : filtersToPopulateContextExcludeAllGlobal) {
			ArrayList<IContextExpression> exprs = new ArrayList<IContextExpression>();
			for (IContextExpression expr : filter.getGlobalContextExcludeExpressions())
				exprs.add(expr);
			for (PhraseFilter f : set.getFilteredPhrases()) {
				if (f.getContextExcludeExpressions().length == 0 && f.getContextExcludeExpressions().length == 0
						&& !filtersToPopulateContextExcludeAllGlobal.contains(f)) {
					exprs.add(new ContextWrappedPhrase(
							new ContextPhrase(f.getModes(), f.getPhrase(), f.getVariants(), false)));
				}
			}
			filter.updateContexts(filter.getContextIncludeExpressions(), filter.getContextExcludeExpressions(),
					filter.getGlobalContextIncludeExpressions(), exprs.toArray(t -> new IContextExpression[t]));
		}

		// Return
		return set;
	}

	private IContextExpression parseExpression(String args, String filePath, int ln, PhraseFilterSet set)
			throws IOException {
		// Parse
		char[] chars = args.toCharArray();
		boolean hadPhrase = false;
		boolean inOperator = false;
		boolean quoteBlock = false;
		boolean phraseStarted = false;
		boolean wasAnd = false;
		String phraseBlock = "";
		IContextExpression resultExpression = null;
		IContextExpression queued = null;
		for (int i = 0; i < args.length(); i++) {
			char ch = chars[i];
			if (ch == '(' && !quoteBlock) {
				// Block
				// Parse block
				int lI = i + 1;
				int i2 = lI;
				int boxes = 1;
				boolean hadPhraseWrapped = false;
				boolean inOperatorWrapped = false;
				boolean quoteBlockWrapped = false;
				boolean phraseStartedWrapped = false;
				String boxExpression = "";
				String phraseBlockWrapped = "";
				for (i2 = lI; i2 < args.length(); i2++) {
					char ch2 = chars[i2];
					if (ch2 == '(' && !phraseStartedWrapped && !quoteBlockWrapped) {
						boxes++;
						boxExpression += ch2;
					} else if (ch2 == ')' && !quoteBlockWrapped) {
						if (phraseStartedWrapped) {
							// Clean
							String blockTrailer = "";
							while (phraseBlockWrapped.endsWith(" ")) {
								blockTrailer += phraseBlockWrapped.substring(phraseBlockWrapped.length() - 1);
								phraseBlockWrapped = phraseBlockWrapped.substring(0, phraseBlockWrapped.length() - 1);
							}

							// Write
							boxExpression += phraseBlockWrapped + blockTrailer;
							phraseBlockWrapped = "";
							inOperatorWrapped = false;
							phraseStartedWrapped = false;
							hadPhraseWrapped = true;
						} else if (inOperatorWrapped) {
							throw new IOException(
									"Parenthesis block misformatted: operator does not have a left-hand phrase comparison ("
											+ filePath + ", line " + ln + ")");
						} else if (!hadPhraseWrapped) {
							throw new IOException("Parenthesis block misformatted: no expression present (" + filePath
									+ ", line " + ln + ")");
						}
						boxes--;
						if (boxes <= 0) {
							break;
						}
						boxExpression += ch2;
					} else {
						if (ch2 != '\\' || (i2 + 1 <= chars.length && chars[i2 + 1] != '"')) {
							if (ch2 == '"' && (i2 == 0 || chars[i2 - 1] != '\\')) {
								quoteBlockWrapped = !quoteBlockWrapped;
							} else {
								if (!quoteBlockWrapped) {
									if (ch2 == '&') {
										// Check next character
										if (chars[i2 + 1] == '&')
											i2++; // Skip character

										// Check phrase
										if (!phraseStartedWrapped)
											throw new IOException("Unexpected AND operator in in context expression ("
													+ filePath + ", line " + ln + ")");

										// Clean
										String blockTrailer = "";
										while (phraseBlockWrapped.endsWith(" ")) {
											blockTrailer += phraseBlockWrapped
													.substring(phraseBlockWrapped.length() - 1);
											phraseBlockWrapped = phraseBlockWrapped.substring(0,
													phraseBlockWrapped.length() - 1);
										}

										// Write
										boxExpression += phraseBlockWrapped + blockTrailer + "&&";
										phraseBlockWrapped = "";
										inOperatorWrapped = true;
										phraseStartedWrapped = false;
										hadPhraseWrapped = true;
									} else if (ch2 == '|') {
										// Check next character
										if (chars[i2 + 1] == '|')
											i2++; // Skip

										// Check phrase
										if (!phraseStartedWrapped)
											throw new IOException("Unexpected OR operator in in context expression ("
													+ filePath + ", line " + ln + ")");

										// Clean
										String blockTrailer = "";
										while (phraseBlockWrapped.endsWith(" ")) {
											blockTrailer += phraseBlockWrapped
													.substring(phraseBlockWrapped.length() - 1);
											phraseBlockWrapped = phraseBlockWrapped.substring(0,
													phraseBlockWrapped.length() - 1);
										}

										// Write
										boxExpression += phraseBlockWrapped + blockTrailer + "||";
										phraseBlockWrapped = "";
										inOperatorWrapped = true;
										phraseStartedWrapped = false;
										hadPhraseWrapped = true;
									} else if (!phraseStartedWrapped) {
										// Havent started with a phrase yet

										// Check character
										if (ch2 != ' ') {
											// Non-space character
											// Start phrase
											phraseStartedWrapped = true;
											phraseBlockWrapped += ch2;
										} else if (ch2 == ' ') {
											// Add to box
											boxExpression += ' ';
										}
									} else {
										// In phrase block
										if (ch2 != ' ') {
											// Add character
											phraseBlockWrapped += ch2;
										} else {
											// Space
											phraseBlockWrapped += ch2;
										}
									}
								} else {
									// Append character to proper block
									if (phraseStartedWrapped)
										phraseBlockWrapped += ch2;
									else
										boxExpression += ch2;
								}
							}
						}
					}
				}
				if (boxes > 0) {
					throw new IOException(
							"Unclosed parenthesis block in context expression (" + filePath + ", line " + ln + ")");
				}
				while (boxExpression.endsWith(" "))
					boxExpression = boxExpression.substring(0, boxExpression.length() - 1);
				while (boxExpression.startsWith(" "))
					boxExpression = boxExpression.substring(1);
				i = i2;

				// Add
				IContextExpression expr = parseExpression(boxExpression, filePath, ln, set);

				// Push expression
				ContextEncasedExpression encased = new ContextEncasedExpression(expr);
				queued = encased;
			} else if (ch == ')') {
				throw new IOException("Unexpected ')' symbol (" + filePath + ", line " + ln + ")");
			} else {
				if (ch != '\\' || (i + 1 <= chars.length && chars[i + 1] != '"')) {
					if (ch == '"' && (i == 0 || chars[i - 1] != '\\')) {
						quoteBlock = !quoteBlock;
					} else {
						if (!quoteBlock) {
							if (ch == '&') {
								// Check next character
								if (chars[i + 1] == '&')
									i++; // Skip character

								// Check phrase
								if (!phraseStarted && queued == null)
									throw new IOException("Unexpected AND operator in in context expression ("
											+ filePath + ", line " + ln + ")");

								// Add
								if (phraseStarted) {
									// Clean
									while (phraseBlock.endsWith(" "))
										phraseBlock = phraseBlock.substring(0, phraseBlock.length() - 1);

									// Check previous
									String phrase = phraseBlock;
									if (inOperator) {
										// Was operator
										IContextExpression left = resultExpression;
										IContextExpression right = new ContextResolvableWrappedPhrase(() -> {
											return resolve(phrase, set);
										});
										resultExpression = new ContextCompareExpression(left,
												wasAnd ? ContextExpressionOperator.AND : ContextExpressionOperator.OR,
												right);
									} else {
										// Was unwrapped
										resultExpression = new ContextResolvableWrappedPhrase(() -> {
											return resolve(phrase, set);
										});
									}
								} else {
									if (inOperator) {
										// Was operator
										IContextExpression left = resultExpression;
										IContextExpression right = queued;
										resultExpression = new ContextCompareExpression(left,
												wasAnd ? ContextExpressionOperator.AND : ContextExpressionOperator.OR,
												right);
										queued = null;
									} else {
										// Was unwrapped
										resultExpression = queued;
										queued = null;
									}
								}

								// Write
								phraseBlock = "";
								inOperator = true;
								phraseStarted = false;
								hadPhrase = true;
								wasAnd = true;
							} else if (ch == '|') {
								// Check next character
								if (chars[i + 1] == '|')
									i++; // Skip

								// Check phrase
								if (!phraseStarted && queued == null)
									throw new IOException("Unexpected OR operator in in context expression (" + filePath
											+ ", line " + ln + ")");

								// Clean
								while (phraseBlock.endsWith(" "))
									phraseBlock = phraseBlock.substring(0, phraseBlock.length() - 1);

								// Add
								if (phraseStarted) {
									// Clean
									while (phraseBlock.endsWith(" "))
										phraseBlock = phraseBlock.substring(0, phraseBlock.length() - 1);

									// Check previous
									String phrase = phraseBlock;
									if (inOperator) {
										// Was operator
										IContextExpression left = resultExpression;
										IContextExpression right = new ContextResolvableWrappedPhrase(() -> {
											return resolve(phrase, set);
										});
										resultExpression = new ContextCompareExpression(left,
												wasAnd ? ContextExpressionOperator.AND : ContextExpressionOperator.OR,
												right);
									} else {
										// Was unwrapped
										resultExpression = new ContextResolvableWrappedPhrase(() -> {
											return resolve(phrase, set);
										});
									}
								} else {
									if (inOperator) {
										// Was operator
										IContextExpression left = resultExpression;
										IContextExpression right = queued;
										resultExpression = new ContextCompareExpression(left,
												wasAnd ? ContextExpressionOperator.AND : ContextExpressionOperator.OR,
												right);
										queued = null;
									} else {
										// Was unwrapped
										resultExpression = queued;
										queued = null;
									}
								}

								// Write
								phraseBlock = "";
								inOperator = true;
								phraseStarted = false;
								hadPhrase = true;
								wasAnd = false;
							} else if (!phraseStarted) {
								// Havent started with a phrase yet

								// Check character
								if (ch != ' ') {
									// Non-space character
									// Start phrase
									phraseStarted = true;
									phraseBlock += ch;
								}
							} else {
								// In phrase block
								if (ch != ' ') {
									// Add character
									phraseBlock += ch;
								} else {
									// Space
									phraseBlock += ch;
								}
							}
						} else {
							// Append character to proper block
							if (phraseStarted)
								phraseBlock += ch;
						}
					}
				}
			}
		}

		if (phraseStarted && inOperator) {
			// Trailing expression from operator

			// Clean
			while (phraseBlock.endsWith(" "))
				phraseBlock = phraseBlock.substring(0, phraseBlock.length() - 1);

			// Write
			String phrase = phraseBlock;
			phraseBlock = "";
			inOperator = false;
			phraseStarted = false;
			hadPhrase = true;

			// Create wrapped
			IContextExpression left = resultExpression;
			IContextExpression right = new ContextResolvableWrappedPhrase(() -> {
				return resolve(phrase, set);
			});
			resultExpression = new ContextCompareExpression(left,
					wasAnd ? ContextExpressionOperator.AND : ContextExpressionOperator.OR, right);
		} else if (inOperator) {
			if (queued == null) {
				// Unclosed operator
				throw new IOException("Expression misformatted: operator does not have a left-hand phrase comparison ("
						+ filePath + ", line " + ln + ")");
			} else {
				// Write
				phraseBlock = "";
				inOperator = false;
				phraseStarted = false;
				hadPhrase = true;

				// Create wrapped
				IContextExpression left = resultExpression;
				IContextExpression right = queued;
				resultExpression = new ContextCompareExpression(left,
						wasAnd ? ContextExpressionOperator.AND : ContextExpressionOperator.OR, right);
			}
		} else if (phraseStarted) {
			// Phrase without operator

			// Clean
			while (phraseBlock.endsWith(" "))
				phraseBlock = phraseBlock.substring(0, phraseBlock.length() - 1);

			// Write
			String phrase = phraseBlock;
			phraseBlock = "";
			inOperator = false;
			phraseStarted = false;
			hadPhrase = true;

			// Create wrapped
			resultExpression = new ContextResolvableWrappedPhrase(() -> {
				return resolve(phrase, set);
			});
		} else if (queued != null) {
			// Phrase without operator

			// Write
			phraseBlock = "";
			inOperator = false;
			phraseStarted = false;
			hadPhrase = true;

			// Create wrapped
			resultExpression = queued;
		} else if (!hadPhrase && resultExpression == null) {
			// Nothing at all
			throw new IOException("Expression misformatted: no expression present (" + filePath + ", line " + ln + ")");
		}

		return resultExpression;
	}

	private ContextPhrase resolve(String phrase, PhraseFilterSet set) {
		// Resolve
		// Find phrase instances
		FilterMode[] preferredModes = null;
		boolean matchOnlyOnFlag = false;
		HashMap<String, FilterMode[]> variants = new HashMap<String, FilterMode[]>();
		for (ContextPhrase ct : set.getContextPhrases()) {
			if (ct.getPhrase().equalsIgnoreCase(phrase)) {
				if (preferredModes == null) {
					preferredModes = ct.getModesFor(ct.getPhrase());
					matchOnlyOnFlag = ct.shouldOnlyMatchOnFilterFlag();
				}
				for (String variant : ct.getAllPhrases()) {
					if (!variants.containsKey(variant)) {
						variants.put(variant, ct.getModesFor(variant));
					}
				}
			}
		}
		for (PhraseFilter ct : set.getFilteredPhrases()) {
			if (ct.getPhrase().equalsIgnoreCase(phrase)) {
				if (preferredModes == null)
					preferredModes = ct.getModes();
				for (String variant : ct.getAllPhrases()) {
					if (!variants.containsKey(variant)) {
						variants.put(variant, ct.getModes());
						if (preferredModes == null)
							preferredModes = ct.getModes();
					}
				}
			}
		}
		if (preferredModes == null)
			preferredModes = new FilterMode[] { FilterMode.WHOLE_PHRASE };
		if (!variants.containsKey(phrase))
			variants.put(phrase, preferredModes);
		return new ContextPhrase(preferredModes, phrase, variants, matchOnlyOnFlag);
	}
}
