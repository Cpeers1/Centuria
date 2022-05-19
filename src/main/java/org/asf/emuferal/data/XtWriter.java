package org.asf.emuferal.data;

import java.util.ArrayList;

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

}
