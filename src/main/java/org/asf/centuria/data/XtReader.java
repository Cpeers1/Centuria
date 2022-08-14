package org.asf.centuria.data;

import java.util.Base64;

public class XtReader {
	private String[] objects;
	private int position = 0;

	public XtReader(String data) {
		objects = parseXT(data);
	}

	private String[] parseXT(String packet) {
		if (packet.startsWith("%xt%"))
			return packet.substring(4).split("%");
		else
			return packet.split("%");
	}

	public boolean hasNext() {
		return position < objects.length;
	}

	public String read() {
		if (!hasNext())
			return null;
		return objects[position++];
	}

	public int readInt() {
		String data = read();
		if (data == null)
			return 0;
		return Integer.parseInt(data);
	}

	public long readLong() {
		String data = read();
		if (data == null)
			return 0;
		return Long.parseLong(data);
	}

	public double readDouble() {
		String data = read();
		if (data == null)
			return 0;
		return Double.parseDouble(data);
	}

	public float readFloat() {
		String data = read();
		if (data == null)
			return 0;
		return Float.parseFloat(data);
	}

	public boolean readBoolean() {
		String data = read();
		if (data == null)
			return false;
		return Boolean.parseBoolean(data);
	}

	public byte[] readBytes() {
		String data = read();
		if (data == null)
			return null;
		return Base64.getDecoder().decode(data);
	}

	public String readRemaining() {
		String rest = "";
		while (hasNext()) {
			if (rest.isEmpty())
				rest = read();
			else
				rest += "%" + read();
		}
		return rest;
	}

}
