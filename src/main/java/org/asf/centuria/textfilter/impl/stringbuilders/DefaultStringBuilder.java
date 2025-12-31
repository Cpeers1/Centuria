package org.asf.centuria.textfilter.impl.stringbuilders;

import org.asf.centuria.textfilter.IResultStringBuilder;
import org.asf.centuria.textfilter.result.TextPart;

public class DefaultStringBuilder implements IResultStringBuilder {

	@Override
	public String buildOutputString(TextPart[] parts) {
		String output = "";
		for (TextPart part : parts) {
			String value = part.getText();
			if (part.isFlagged()) {
				// Filter
				int valueLength = value.length();
				value = "";
				for (int i = 0; i < valueLength; i++)
					value += "#";
			}
			if (output.isEmpty())
				output = value;
			else
				output += " " + value;
		}
		return output;
	}

}
