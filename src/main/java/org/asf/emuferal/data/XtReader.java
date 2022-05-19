package org.asf.emuferal.data;

public class XtReader {
	private String[] objects;
	private int position = 0;

	public XtReader(String data) {
		objects = parseXT(data);
	}

	private String[] parseXT(String packet) {
		return packet.substring(4).split("%");
	}

	public boolean hasNext() {
		return position < objects.length;
	}

	public String read() {
		if (!hasNext())
			return null;
		return objects[position++];
	}

}
