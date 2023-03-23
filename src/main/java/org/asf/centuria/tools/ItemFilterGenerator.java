package org.asf.centuria.tools;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ItemFilterGenerator {

	public static void main(String[] args) throws IOException {
		// This tool generates a creativeitemfilter.json file
		// Expected program arguments: <localization-csv-file> <craftable-item-chart>

		String lastID = "";
		String lastData = "";
		JsonObject res = new JsonObject();
		JsonObject localization = new JsonObject();
		for (String line : Files.readAllLines(Path.of(args[0]))) {
			if (line.startsWith("\"") && !line.startsWith("\"\"")) {
				lastID = line.substring(1);
				lastID = lastID.substring(0, lastID.indexOf(","));
				lastData = "";
			} else if (line.startsWith("\"\"")) {
				lastData = "{\n" + lastData;
				lastData = lastData.replace("`", "\"");
				JsonObject obj = JsonParser.parseString(lastData).getAsJsonObject();
				if (obj.get("templateClass").getAsString().equals("LocalizationTemplate")) {
					for (JsonElement ele : obj.get("components").getAsJsonArray()) {
						JsonObject data = ele.getAsJsonObject();
						if (data.get("componentClass").getAsString().equals("LocalizationDefComponent")) {
							data = data.get("componentJSON").getAsJsonObject();
							localization.addProperty(lastID, data.get("en").getAsString());
						}
					}
				}
			} else if (lastData != null) {
				lastData += line + "\n";
			}
		}

		lastID = "";
		lastData = "";
		String lastName = "";
		boolean skip = false;

		ArrayList<String> clothingIDs = new ArrayList<String>();
		for (String line : Files.readAllLines(Path.of(args[1]))) {
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
										String channel = key.substring("color".length());
										channel = channel.substring(0, channel.lastIndexOf("HSVDefault"));
										if (channel.matches("^[0-9]+$")) {
											int channelCount = Integer.valueOf(channel);
											if (channelCount > channels)
												channels = channelCount;
										}
									}
								}
								if (channels > 0) {
									clothingIDs.add(lastID);
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

		lastID = "";
		lastData = "";
		lastName = "";

		JsonObject items = new JsonObject();
		for (String line : Files.readAllLines(Path.of(args[1]))) {
			if (line.startsWith("\"") && !line.startsWith("\"\"")) {
				lastID = line.substring(1);
				lastName = lastID.substring(lastID.indexOf(",\"\"") + 3);
				lastName = lastName.substring(0, lastName.indexOf("\""));
				lastID = lastID.substring(0, lastID.indexOf(","));
				lastData = "";
			} else if (line.startsWith("\"\"")) {
				lastData = "{\n" + lastData;
				lastData = lastData.replace("`", "\"");
				JsonObject obj = JsonParser.parseString(lastData).getAsJsonObject();

				String nameID = null;
				String descID = null;
				String inventoryType = null;
				for (JsonElement ele : obj.get("components").getAsJsonArray()) {
					JsonObject data = ele.getAsJsonObject();
					if (data.get("componentClass").getAsString().equals("LocalizedNameDefComponent")) {
						data = data.get("componentJSON").getAsJsonObject();
						String id = data.get("localizedDefID").getAsString();
						if (localization.has(id))
							nameID = id;
					} else if (data.get("componentClass").getAsString().equals("LocalizedDescriptionDefComponent")) {
						data = data.get("componentJSON").getAsJsonObject();
						String id = data.get("localizedDefID").getAsString();
						if (localization.has(id))
							descID = id;
					} else if (data.get("componentClass").getAsString().equals("ItemDefComponent")) {
						data = data.get("componentJSON").getAsJsonObject();
						inventoryType = data.get("itemType").getAsString();
					}
				}

				if (nameID == null) {
					nameID = "";
				} else
					nameID = localization.get(nameID).getAsString();
				if (descID == null)
					descID = "";
				else
					descID = localization.get(descID).getAsString();
				if (inventoryType != null) {
					if (nameID.isEmpty() || lastID.equals("9025")) {
						// Add
						if (inventoryType.equals("100") && clothingIDs.contains(lastID))
							items.addProperty(lastID, lastName);
					}
				}
			} else if (lastData != null) {
				lastData += line + "\n";
			}
		}
		res.add("Items", items);

		System.out.println(new Gson().newBuilder().setPrettyPrinting().create().toJson(res));
	}

}
