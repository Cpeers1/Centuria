package org.asf.centuria.tools.legacyclienttools.translation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class AvatarTranslators {

	/**
	 * Translates avatar data (beta-to-modern)
	 * 
	 * @param avaInfo Avatar look to translate
	 * @return Translated avatar
	 */
	public static JsonObject translateAvatarInfoToModern(JsonObject avaInfo) {
		// Translate colors
		avaInfo.keySet().forEach(t -> {
			if (t.contains("Color") && t.endsWith("HSV")) {
				ColorTranslators.translateColorToModern(avaInfo.get(t).getAsJsonObject());
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
										ColorTranslators.translateColorToModern(decalO.get(t).getAsJsonObject());
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
							ColorTranslators.translateColorToModern(clothingO.get(t).getAsJsonObject());
						}
					});
				}
			}
		}

		return avaInfo;
	}

	/**
	 * Translates avatar data (modern-to-beta)
	 * 
	 * @param avaInfo Avatar look to translate
	 * @return Translated avatar
	 */
	public static JsonObject translateAvatarInfoToBeta(JsonObject avaInfo) {
		// Translate colors
		avaInfo.keySet().forEach(t -> {
			if (t.contains("Color") && t.endsWith("HSV")) {
				ColorTranslators.translateColorToBeta(avaInfo.get(t).getAsJsonObject());
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
										ColorTranslators.translateColorToBeta(decalO.get(t).getAsJsonObject());
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
							ColorTranslators.translateColorToBeta(clothingO.get(t).getAsJsonObject());
						}
					});
				}
			}
		}

		return avaInfo;
	}

}
