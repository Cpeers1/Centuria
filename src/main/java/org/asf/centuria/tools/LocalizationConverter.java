package org.asf.centuria.tools;

import java.io.IOException;
import java.nio.file.*;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class LocalizationConverter {

	public static void main(String[] args) throws IOException {
		// This tool generates a localization.json file
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
		res.add("Localization", localization);

		lastID = "";
		lastData = "";
		JsonObject items = new JsonObject();
		JsonObject itemdescirptions = new JsonObject();
		String csv = "Item ID,Name,Description\n";
		for (String line : Files.readAllLines(Path.of(args[1]))) {
			if (line.startsWith("\"") && !line.startsWith("\"\"")) {
				lastID = line.substring(1);
				lastID = lastID.substring(0, lastID.indexOf(","));
				lastData = "";
			} else if (line.startsWith("\"\"")) {
				lastData = "{\n" + lastData;
				lastData = lastData.replace("`", "\"");
				JsonObject obj = JsonParser.parseString(lastData).getAsJsonObject();

				String nameID = null;
				String descID = null;
				for (JsonElement ele : obj.get("components").getAsJsonArray()) {
					JsonObject data = ele.getAsJsonObject();
					if (data.get("componentClass").getAsString().equals("LocalizedNameDefComponent")) {
						data = data.get("componentJSON").getAsJsonObject();
						String id = data.get("localizedDefID").getAsString();
						if (localization.has(id)) {
							items.addProperty(lastID, id);
							nameID = id;
						}
					} else if (data.get("componentClass").getAsString().equals("LocalizedDescriptionDefComponent")) {
						data = data.get("componentJSON").getAsJsonObject();
						String id = data.get("localizedDefID").getAsString();
						if (localization.has(id)) {
							descID = id;
							itemdescirptions.addProperty(lastID, id);
						}
					}
				}
				
				if (nameID != null) {
					if (descID == null)
						descID = "No description";
					else
						descID = localization.get(descID).getAsString();
					csv += lastID + ",\"" + localization.get(nameID).getAsString() + "\",\"" + descID + "\"\n";
				}				
			} else if (lastData != null) {
				lastData += line + "\n";
			}
		}
		res.add("ItemNames", items);
		res.add("ItemDescriptions", itemdescirptions);

		System.out.println(new Gson().newBuilder().setPrettyPrinting().create().toJson(res));
//		System.out.println(csv);
	}

}
