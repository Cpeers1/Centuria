package org.asf.centuria.tools.legacyclienttools.translation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class InventoryTranslators {

	/**
	 * Translates an IL packet content json to beta
	 * 
	 * @param ilData IL packet content json
	 * @return Translated content
	 */
	public static String translateILToBeta(String ilData) {
		JsonArray data = JsonParser.parseString(ilData).getAsJsonArray();
		for (JsonElement ele : data) {
			JsonObject item = ele.getAsJsonObject();
			translateInventoryObject(item.get("type").getAsInt(), item.get("defId").getAsInt(),
					item.get("components").getAsJsonObject());
		}
		return data.toString();
	}

	/**
	 * Translates an inventory object to beta
	 * 
	 * @param type       Inventory type
	 * @param defId      Inventory item defID
	 * @param components Item components
	 */
	public static void translateInventoryObject(int type, int defId, JsonObject components) {
		// Type translation
		switch (type) {
		case 200: {
			// Avatar

			// Find look info
			if (components.has("AvatarLook")) {
				JsonObject look = components.get("AvatarLook").getAsJsonObject();
				if (look.has("info")) {
					JsonObject lookInfo = look.get("info").getAsJsonObject();
					look.remove("info");
					look.add("info", AvatarTranslators.translateAvatarInfoToBeta(lookInfo));
				}
			}

			// TODO: primary look stuff
			break;
		}
		}

		// Color info
		if (components.has("Colorable")) {
			JsonObject colors = components.get("Colorable").getAsJsonObject();
			for (String key : colors.keySet()) {
				if (key.startsWith("color") && key.endsWith("HSV")) {
					ColorTranslators.translateColorToBeta(colors.get(key).getAsJsonObject());
				}
			}
		}
	}

}
