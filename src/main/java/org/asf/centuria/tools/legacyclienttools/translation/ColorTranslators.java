package org.asf.centuria.tools.legacyclienttools.translation;

import com.google.gson.JsonObject;

public class ColorTranslators {

	/**
	 * Tool for modern-to-beta color translation
	 * 
	 * @param colorInfo Modern-style color information object
	 */
	public static void translateColorToBeta(JsonObject colorInfo) {
		if (colorInfo.has("_hsv")) {
			String hsv = colorInfo.get("_hsv").getAsString();
			String[] hsvs = hsv.split(",");
			if (hsvs.length == 3) {
				if (hsvs[0].matches("^[0-9]+$") && hsvs[1].matches("^[0-9]+$") && hsvs[2].matches("^[0-9]+$")) {
					if (colorInfo.has("_h"))
						colorInfo.remove("_h");
					if (colorInfo.has("_s"))
						colorInfo.remove("_s");
					if (colorInfo.has("_v"))
						colorInfo.remove("_v");
					colorInfo.addProperty("_h", Double.toString((double) Integer.parseInt(hsvs[0]) / 10000d));
					colorInfo.addProperty("_s", Double.toString((double) Integer.parseInt(hsvs[1]) / 10000d));
					colorInfo.addProperty("_v", Double.toString((double) Integer.parseInt(hsvs[2]) / 10000d));
					colorInfo.remove("_hsv");
				}
			}
		}
	}

	/**
	 * Tool for beta-to-modern color translation
	 * 
	 * @param colorInfo Beta-style color information object
	 */
	public static void translateColorToModern(JsonObject colorInfo) {
		if (colorInfo.has("_h") && colorInfo.has("_s") && colorInfo.has("_v")) {
			double h = colorInfo.get("_h").getAsDouble() * 10000d;
			double s = colorInfo.get("_s").getAsDouble() * 10000d;
			double v = colorInfo.get("_v").getAsDouble() * 10000d;
			colorInfo.addProperty("_hsv", (int) h + "," + (int) s + "," + (int) v);
			colorInfo.addProperty("_h", "");
			colorInfo.addProperty("_s", "");
			colorInfo.addProperty("_v", "");
		}
	}

}
