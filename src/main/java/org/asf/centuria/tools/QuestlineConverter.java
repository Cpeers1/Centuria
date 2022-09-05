package org.asf.centuria.tools;

import java.io.IOException;
import java.nio.file.*;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class QuestlineConverter {

	public static void main(String[] args) throws IOException {
		// This tool generates a questline.json file
		// Expected program arguments: <csv-file>

		JsonObject res = new JsonObject();
		String lastID = "";
		String lastData = "";
		
		JsonObject quests = new JsonObject();
		for (String line : Files.readAllLines(Path.of(args[0]))) {
			if (line.startsWith("\"") && !line.startsWith("\"\"")) {
				lastID = line.substring(1);
				lastID = lastID.substring(0, lastID.indexOf(","));
				lastData = "";
			} else if (line.startsWith("\"\"")) {
				lastData = "{\n" + lastData;
				lastData = lastData.replace("`", "\"");
				JsonObject obj = JsonParser.parseString(lastData).getAsJsonObject();
				if (obj.get("templateClass").getAsString().equals("ListTemplate")) {
					for (JsonElement ele : obj.get("components").getAsJsonArray()) {
						JsonObject data = ele.getAsJsonObject();
						if (data.get("componentClass").getAsString().equals("ListDefComponent")) {
							data = data.get("componentJSON").getAsJsonObject();
							
							String last = null;
							JsonArray lst = data.get("list").getAsJsonObject().get("_defIDs").getAsJsonArray();
							for (JsonElement el : lst) {
								if (last != null)
									quests.addProperty(last, el.getAsString());
								last = el.getAsString();
							}
						}
					}
				}
			} else if (lastData != null) {
				lastData += line + "\n";
			}
		}
		res.add("QuestMap", quests);
		System.out.println(new Gson().newBuilder().setPrettyPrinting().create().toJson(res));
	}

}
