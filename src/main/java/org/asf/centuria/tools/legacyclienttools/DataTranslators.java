package org.asf.centuria.tools.legacyclienttools;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class DataTranslators {

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
					look.add("info", JsonParser.parseString(translateAvatarInfoToBeta(lookInfo.toString())));
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
					translateColor(colors, key);
				}
			}
		}
	}

	/**
	 * Translates avatar data (beta-to-modern)
	 * 
	 * @param look Avatar look to translate
	 * @return Translated avatar
	 */
	public static String translateAvatarInfoToModern(String look) {
		JsonObject avaInfo = JsonParser.parseString(look).getAsJsonObject();

		// Translate colors
		avaInfo.keySet().forEach(t -> {
			if (t.contains("Color") && t.endsWith("HSV")) {
				translateColorToModern(avaInfo, t);
			}
		});

		// Translate decal colors
		if (avaInfo.has("bodyParts") && avaInfo.get("bodyParts").isJsonArray()) {
			JsonArray parts = avaInfo.get("bodyParts").getAsJsonArray();
			for (JsonElement part : parts) {
				if (part.isJsonObject()) {
					JsonObject partO = part.getAsJsonObject();
					if (partO.has("_decalEntries") && partO.get("_decalEntries").isJsonArray()) {
						JsonArray decals = partO.get("_decalEntries").getAsJsonArray();
						for (JsonElement decal : decals) {
							if (decal.isJsonObject()) {
								JsonObject decalO = decal.getAsJsonObject();
								decalO.keySet().forEach(t -> {
									if (t.contains("color") && t.endsWith("HSV")) {
										translateColorToModern(decalO, t);
									}
								});
							}
						}
					}
				}
			}
		}

		// Translate clothing colors
		if (avaInfo.has("clothingItems") && avaInfo.get("clothingItems").isJsonArray()) {
			JsonArray items = avaInfo.get("clothingItems").getAsJsonArray();
			for (JsonElement item : items) {
				if (item.isJsonObject()) {
					JsonObject clothingO = item.getAsJsonObject();
					clothingO.keySet().forEach(t -> {
						if (t.contains("color") && t.endsWith("HSV")) {
							translateColorToModern(clothingO, t);
						}
					});
				}
			}
		}

		return avaInfo.toString();
	}

	/**
	 * Translates avatar data (modern-to-beta)
	 * 
	 * @param look Avatar look to translate
	 * @return Translated avatar
	 */
	public static String translateAvatarInfoToBeta(String look) {
		JsonObject avaInfo = JsonParser.parseString(look).getAsJsonObject();

		// Translate colors
		avaInfo.keySet().forEach(t -> {
			if (t.contains("Color") && t.endsWith("HSV")) {
				translateColor(avaInfo, t);
			}
		});

		// Translate decal colors
		if (avaInfo.has("bodyParts") && avaInfo.get("bodyParts").isJsonArray()) {
			JsonArray parts = avaInfo.get("bodyParts").getAsJsonArray();
			for (JsonElement part : parts) {
				if (part.isJsonObject()) {
					JsonObject partO = part.getAsJsonObject();
					if (partO.has("_decalEntries") && partO.get("_decalEntries").isJsonArray()) {
						JsonArray decals = partO.get("_decalEntries").getAsJsonArray();
						for (JsonElement decal : decals) {
							if (decal.isJsonObject()) {
								JsonObject decalO = decal.getAsJsonObject();
								decalO.keySet().forEach(t -> {
									if (t.contains("color") && t.endsWith("HSV")) {
										translateColor(decalO, t);
									}
								});
							}
						}
					}
				}
			}
		}

		// Translate clothing colors
		if (avaInfo.has("clothingItems") && avaInfo.get("clothingItems").isJsonArray()) {
			JsonArray items = avaInfo.get("clothingItems").getAsJsonArray();
			for (JsonElement item : items) {
				if (item.isJsonObject()) {
					JsonObject clothingO = item.getAsJsonObject();
					clothingO.keySet().forEach(t -> {
						if (t.contains("Color") && t.endsWith("HSV")) {
							translateColor(clothingO, t);
						}
					});
				}
			}
		}

		return avaInfo.toString();
	}

	// Tool for modern-to-beta color translation
	private static void translateColor(JsonObject obj, String key) {
		if (obj.has(key) && obj.get(key).isJsonObject()) {
			JsonObject colorInfo = obj.get(key).getAsJsonObject();
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
	}

	// Tool for beta-to-modern color translation
	private static void translateColorToModern(JsonObject obj, String key) {
		if (obj.has(key) && obj.get(key).isJsonObject()) {
			JsonObject colorInfo = obj.get(key).getAsJsonObject();
			if (colorInfo.has("_hsv")) {
				String hsv = colorInfo.get("_hsv").getAsString();
				String[] hsvs = hsv.split(",");
				if (hsvs.length == 3) {
					// TODO
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
	}

}
