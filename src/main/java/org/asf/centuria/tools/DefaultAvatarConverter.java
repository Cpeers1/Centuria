package org.asf.centuria.tools;

import java.io.IOException;
import java.nio.file.*;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class DefaultAvatarConverter {

	public static void main(String[] args) throws IOException {
		// This tool generates a avatardefaultshelper.json file
		// Expected program arguments: <csv-file>

		JsonObject res = new JsonObject();
		JsonObject avatars = new JsonObject();
		String lastID = "";
		String lastData = null;
		String lastName = "";
		boolean skip = false;
		for (String line : Files.readAllLines(Path.of(args[0]))) {
			if (line.startsWith("\"") && !line.startsWith("\"\"")) {
				lastID = line.substring(1);
				lastName = lastID.substring(lastID.indexOf(",\"\"") + 3);
				lastName = lastName.substring(0, lastName.indexOf("\""));
				lastID = lastID.substring(0, lastID.indexOf(","));
				lastData = "";
				if (!lastName.startsWith("Avatar/"))
					skip = true;

				// Filter test and faun and none
				if (lastName.contains("Test") || lastName.contains("Faun") || lastName.contains("None"))
					skip = true;
			} else if (line.startsWith("\"\"")) {
				if (!skip) {
					lastData = "{\n" + lastData;
					lastData = lastData.replace("`", "\"");
					JsonObject obj = JsonParser.parseString(lastData).getAsJsonObject();
					if (obj.get("templateClass").getAsString().equals("ActorClassTemplate")) {
						for (JsonElement ele : obj.get("components").getAsJsonArray()) {
							JsonObject data = ele.getAsJsonObject();
							if (data.get("componentClass").getAsString().equals("ActorClassDefComponent")) {
								JsonArray ids = data.get("componentJSON").getAsJsonObject().get("bodyPartDefIDs")
										.getAsJsonArray();
								avatars.add(lastID, ids);
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
		res.add("AvatarDefaults", avatars);

		System.out.println(new Gson().newBuilder().setPrettyPrinting().create().toJson(res));
	}

}
