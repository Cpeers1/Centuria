package org.asf.emuferal.tools;

import java.io.IOException;
import java.nio.file.*;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class DyeConverter {

	public static void main(String[] args) throws IOException {
		// This tool generates a dyehelper.json file
		// Expected program arguments: <csv-file>

		JsonObject res = new JsonObject();
		JsonObject dyes = new JsonObject();
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
					if (obj.get("templateClass").getAsString().equals("DyeTemplate")) {
						for (JsonElement ele : obj.get("components").getAsJsonArray()) {
							JsonObject data = ele.getAsJsonObject();
							if (data.get("componentClass").getAsString().equals("ColorDefComponent")) {
								data = data.get("componentJSON").getAsJsonObject();
								dyes.addProperty(lastID, data.get("color").getAsJsonObject().get("_hsv").getAsString());
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
		res.add("Dyes", dyes);

		System.out.println(new Gson().newBuilder().setPrettyPrinting().create().toJson(res));
	}

}
