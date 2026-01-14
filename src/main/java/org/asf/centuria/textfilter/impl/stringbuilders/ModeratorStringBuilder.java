package org.asf.centuria.textfilter.impl.stringbuilders;

import org.asf.centuria.textfilter.IResultStringBuilder;
import org.asf.centuria.textfilter.result.TextPart;

public class ModeratorStringBuilder implements IResultStringBuilder {

	private String highlightColor;

	public ModeratorStringBuilder(String highlightColor) {
		this.highlightColor = highlightColor;
	}

	@Override
	public String buildOutputString(TextPart[] parts) {
		String output = "";
		for (TextPart part : parts) {
			String value = part.getText();
			if (part.isFlagged()) {
				// Create highlight
				String highlight = "</noparse><color=" + highlightColor + "><noparse>" + value
						+ "</noparse></color><noparse>";

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
