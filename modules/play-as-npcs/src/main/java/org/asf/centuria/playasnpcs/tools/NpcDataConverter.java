package org.asf.centuria.playasnpcs.tools;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class NpcDataConverter {

	public static void main(String[] args) throws IOException {
		// This tool generates a npcs.json file
		// Expected program arguments: <csv-file>

		JsonObject res = new JsonObject();
		JsonObject npcs = new JsonObject();
		String lastID = "";
		String lastData = null;

		int opening = 0;
		boolean skip = false;
		for (String line : Files.readAllLines(Path.of(args[0]))) {
			if (opening == 1 && line.contains("`templateClass`:`")) {
				opening = 0;
			}
			if (opening != 0) {
				if (line.contains("{"))
					opening++;
				if (line.contains("}"))
					opening--;
			} else if (line.startsWith("\"") && !line.startsWith("\"\"")) {
				if (line.contains("Avatar/REUSE_ME"))
					skip = true;
				String id = line.substring(1);
				id = id.substring(0, id.indexOf(","));
				lastID = id;
				lastData = "";

				if (line.endsWith("{")) {
					// Keep reading
					opening = 1;
				}
			} else if (line.startsWith("\"\"")) {
				if (!skip) {
					lastData = "{\n" + lastData;
					lastData = lastData.replace("`", "\"");
					if (lastData.contains("}")) {
						JsonObject obj = JsonParser.parseString(lastData).getAsJsonObject();
						if (obj.get("templateClass").getAsString().equals("ActorNPCTemplate")) {
							for (JsonElement ele : obj.get("components").getAsJsonArray()) {
								JsonObject data = ele.getAsJsonObject();
								if (data.get("componentClass").getAsString().equals("ActorInfoDefComponent")) {
									data = data.get("componentJSON").getAsJsonObject();

									JsonObject actorData = data.get("serializedActorInfo").getAsJsonObject();
									npcs.add(lastID, actorData);
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
		res.add("NpcData", npcs);

		System.out.println(new Gson().newBuilder().setPrettyPrinting().create().toJson(res));
	}

}
