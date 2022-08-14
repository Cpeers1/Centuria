package org.asf.centuria.tools;

import java.io.IOException;
import java.nio.file.*;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class SanctuaryClassDataConverter {

	public static void main(String[] args) throws IOException {
		// This tool generates a sanctruaryclasseshelper.json file
		//
		// Expected program arguments:
		// <csv-file>

		String lastName = "";
		String lastData = null;
		String lastID = null;

		JsonObject res = new JsonObject();
		JsonObject sanctuaryClasses = new JsonObject();
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
				for (JsonElement ele : obj.get("components").getAsJsonArray()) {
					JsonObject data = ele.getAsJsonObject();
					if (data.get("componentClass").getAsString().equals("SanctuaryClassDefComponent")) {
						data = data.get("componentJSON").getAsJsonObject();

						JsonObject def = new JsonObject();
						def.addProperty("lookDefId", data.get("sanctuaryLookDefID").getAsString());
						def.addProperty("houseDefId",
								data.get("serializedSanctuaryInfo").getAsJsonObject().get("houseDefID").getAsString());
						def.addProperty("islandDefId",
								data.get("serializedSanctuaryInfo").getAsJsonObject().get("islandDefID").getAsString());
						sanctuaryClasses.add(lastID, def);
					}
				}

				lastData = null;
			} else if (lastData != null) {
				lastData += line + "\n";
			}
		}
		res.add("SanctuaryClasses", sanctuaryClasses);

		System.out.println(new Gson().newBuilder().setPrettyPrinting().create().toJson(res));
	}

}
