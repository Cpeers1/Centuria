package org.asf.emuferal.tools;

import java.io.IOException;
import java.nio.file.*;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ItemDefinitionConverter {

	public static void main(String[] args) throws IOException {
		// This tool generates a itemdefinitions.json file
		//
		// Expected program arguments:
		// <craftableitemchart-csv-file>

		String lastName = "";
		String lastData = null;
		String lastID = null;

		JsonObject res = new JsonObject();
		JsonObject definitions = new JsonObject();
		for (String line : Files.readAllLines(Path.of(args[0]))) {
			if (line.startsWith("\"") && !line.startsWith("\"\"") && !line.startsWith("\"DefID")) {
				lastID = line.substring(1);
				lastName = lastID.substring(lastID.indexOf(",\"\"") + 3);
				lastName = lastName.substring(0, lastName.indexOf("\""));
				lastID = lastID.substring(0, lastID.indexOf(","));
				lastData = "";
			} else if (line.startsWith("\"\"")) {
				lastData = "{\n" + lastData;
				lastData = lastData.replace("`", "\"");
				JsonObject obj = JsonParser.parseString(lastData).getAsJsonObject();

				int itemType = -1;
				for (JsonElement ele : obj.get("components").getAsJsonArray()) {
					JsonObject data = ele.getAsJsonObject();
					if (data.get("componentClass").getAsString().equals("ItemDefComponent")) {
						data = data.get("componentJSON").getAsJsonObject();
						itemType = data.get("itemType").getAsInt();
					}
				}

				if (itemType != -1) {
					JsonObject itm = new JsonObject();
					itm.addProperty("inventory", itemType);
					itm.addProperty("objectName", lastName);
					definitions.add(lastID, itm);
				}

				lastData = null;
			} else if (lastData != null) {
				lastData += line + "\n";
			}
		}
		res.add("Definitions", definitions);

		System.out.println(new Gson().newBuilder().setPrettyPrinting().create().toJson(res));
	}

}
