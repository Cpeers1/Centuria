package org.asf.centuria.data;

import java.util.ArrayList;
import java.util.Base64;

public class XtWriter {

	private ArrayList<String> objects = new ArrayList<String>();

	public String encode() {
		boolean first = true;
		String d = "%xt%";
		for (String t : objects) {
			if (!first)
				d += "%";
			d += t;
			first = false;
		}
		return d;

	}

	@Override
	public String toString() {
		return encode();
	}

	public void add(String object) {
		objects.add(object);
	}

	public void writeString(String data) {
		add(data);
	}

	public void writeInt(int num) {
		add(Integer.toString(num));
	}

	public void writeLong(long num) {
		add(Long.toString(num));
	}

	public void writeFloat(float num) {
		add(Float.toString(num));
	}

	public void writeDouble(double num) {
		add(Double.toString(num));
	}

	public void writeBoolean(boolean v) {
		add(Boolean.toString(v));
	}

	public void writeBytes(byte[] bytes) {
		add(Base64.getEncoder().encodeToString(bytes));
	}

}
