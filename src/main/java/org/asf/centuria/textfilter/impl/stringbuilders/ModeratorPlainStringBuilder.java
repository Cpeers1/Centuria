package org.asf.centuria.textfilter.impl.stringbuilders;

import org.asf.centuria.textfilter.IResultStringBuilder;
import org.asf.centuria.textfilter.result.TextPart;

public class ModeratorPlainStringBuilder implements IResultStringBuilder {

	@Override
	public String buildOutputString(TextPart[] parts) {
		String output = "";
		for (TextPart part : parts) {
			String value = part.getText();
			if (part.isFlagged()) {
				// Create highlight
				String highlight = "[!]" + value + "[!]";

				// Add message part
				value = highlight;
			}
			if (output.isEmpty())
				output = value;
			else
				output += " " + value;
		}
		return output;
	}

}
