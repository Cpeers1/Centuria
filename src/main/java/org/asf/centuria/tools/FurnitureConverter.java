package org.asf.centuria.tools;

import java.io.IOException;
import java.nio.file.*;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class FurnitureConverter {

	public static void main(String[] args) throws IOException {
		// This tool generates a furniturehelper.json file
		// Expected program arguments: <csv-file>

		JsonObject res = new JsonObject();
		JsonObject furniture = new JsonObject();
		String lastID = "";
		String lastData = null;
		boolean skip = false;
		for (String line : Files.readAllLines(Path.of(args[0]))) {
			if (line.startsWith("\"") && !line.startsWith("\"\"")) {
				String id = line.substring(1);
				id = id.substring(0, id.indexOf(","));
				lastID = id;
				lastData = "";
			} else if (line.startsWith("\"\"")) {
				if (!skip) {
					lastData = "{\n" + lastData;
					lastData = lastData.replace("`", "\"");
					JsonObject obj = JsonParser.parseString(lastData).getAsJsonObject();
					if (obj.get("templateClass").getAsString().contains("Sanctuary")) {
						boolean valid = false;
						for (JsonElement ele : obj.get("components").getAsJsonArray()) {
							JsonObject data = ele.getAsJsonObject();
							if (data.get("componentClass").getAsString().equals("SanctuaryItemDefComponent")) {
								valid = true;
							}
						}
						if (valid)
							for (JsonElement ele : obj.get("components").getAsJsonArray()) {
								JsonObject data = ele.getAsJsonObject();
								if (data.get("componentClass").getAsString().equals("ColorableDefComponent")) {
									data = data.get("componentJSON").getAsJsonObject();
									int channels = 0;
									for (String key : data.keySet()) {
										if (key.startsWith("color") && key.endsWith("HSVDefault")) {
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
												itm.add("color" + i + "HSV", data.get("color" + i + "HSVDefault"));
											}
										}
										furniture.add(lastID, itm);
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
		res.add("Furniture", furniture);

		System.out.println(new Gson().newBuilder().setPrettyPrinting().create().toJson(res));
	}

}
