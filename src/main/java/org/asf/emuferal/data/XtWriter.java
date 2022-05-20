package org.asf.emuferal.data;

import java.util.ArrayList;
import java.util.Base64;

public class XtWriter {

	private ArrayList<String> objects = new ArrayList<String>();

	public String encode() {
		String d = "%xt%";
		for (String t : objects) {
			if (d.length() != 4)
				d += "%";
			d += t;
		}
		return d;

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
