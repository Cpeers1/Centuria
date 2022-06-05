package org.asf.emuferal.tools;

import java.io.IOException;
import java.nio.file.*;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ClothingConverter {

	public static void main(String[] args) throws IOException {
		// This tool generates a clothinghelper.json file
		// Expected program arguments: <csv-file>

		JsonObject res = new JsonObject();
		JsonObject clothing = new JsonObject();
		String lastID = "";
		String lastData = null;
		boolean skip = false;
		for (String line : Files.readAllLines(Path.of(args[0]))) {
			if (line.startsWith("\"") && !line.startsWith("\"\"")) {
				if (line.contains("Avatar/REUSE_ME"))
					skip = true;
				String id = line.substring(1);
				id = id.substring(0, id.indexOf(","));
				lastID = id;
				lastData = "";
			} else if (line.startsWith("\"\"")) {
				if (!skip) {
					lastData = "{\n" + lastData;
					lastData = lastData.replace("`", "\"");
					JsonObject obj = JsonParser.parseString(lastData).getAsJsonObject();
					if (obj.get("templateClass").getAsString().equals("ActorClothingTemplate")) {
						for (JsonElement ele : obj.get("components").getAsJsonArray()) {
							JsonObject data = ele.getAsJsonObject();
							if (data.get("componentClass").getAsString().equals("ColorableDefComponent")) {
								data = data.get("componentJSON").getAsJsonObject();
								int channels = 0;
								for (String key : data.keySet()) {
									if (key.startsWith("color") && key.endsWith("HSVDefault")) {
										if (data.get(key).getAsJsonObject().get("_hsv").getAsString().equals("0,0,0"))
											continue;
										String channel = key.substring("color".length());
										channel = channel.substring(0, channel.lastIndexOf("HSVDefault"));
										if (channel.matches("^[0-9]+$")) {
											int channelCount = Integer.valueOf(channel);
											if (channelCount > channels)
												channels = channelCount;
										}
									}
								}
								if (channels > 0 && data.get("channelCount").getAsInt() > 0) {
									JsonObject itm = new JsonObject();
									itm.addProperty("availableChannels", channels);
									for (int i = 1; i <= channels; i++) {
										if (data.has("color" + i + "HSVDefault")) {
											JsonObject hsv = new JsonObject();
											hsv.addProperty("_hsv", data.get("color" + i + "HSVDefault")
													.getAsJsonObject().get("_hsv").getAsString());
											itm.add("color" + i + "HSV", hsv);
										}
									}
									clothing.add(lastID, itm);
								}
							}
						}
					}
				}
				lastData = null;
				lastID = "";
				skip = false;
			} else if (lastData != null) {
				lastData += line + "\n";
			}
		}
		res.add("Clothing", clothing);

		System.out.println(new Gson().newBuilder().setPrettyPrinting().create().toJson(res));
	}

}
