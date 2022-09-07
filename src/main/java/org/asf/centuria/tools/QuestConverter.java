package org.asf.centuria.tools;

import java.io.IOException;
import java.nio.file.*;
import java.util.HashMap;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class QuestConverter {

	public static void main(String[] args) throws IOException {
		// This tool generates a quests.json file
		// Expected program arguments: <csv-file> <localization-csv-file>

		HashMap<String, String> localization = new HashMap<String, String>();
		String lastID = "";
		String lastData = "";
		for (String line : Files.readAllLines(Path.of(args[1]))) {
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
							localization.put(lastID, data.get("en").getAsString());
						}
					}
				}
			} else if (lastData != null) {
				lastData += line + "\n";
			}
		}

		JsonObject res = new JsonObject();
		lastID = "";
		String lastName = "";
		lastData = "";
		JsonObject quests = new JsonObject();
		for (String line : Files.readAllLines(Path.of(args[0]))) {
			if (line.startsWith("\"") && !line.startsWith("\"\"")) {
				lastID = line.substring(1);
				lastName = lastID.substring(lastID.indexOf(",") + 1);
				lastName = lastName.substring(lastName.indexOf(",") + 1);
				lastName = lastName.substring(0, lastName.indexOf(","));
				lastName = lastName.substring(2, lastName.length() - 2);
				lastID = lastID.substring(0, lastID.indexOf(","));
				lastData = "";
			} else if (line.startsWith("\"\"")) {
				lastData = "{\n" + lastData;
				lastData = lastData.replace("`", "\"");
				JsonObject obj = JsonParser.parseString(lastData).getAsJsonObject();
				if (obj.get("templateClass").getAsString().equals("SocialExpanseTemplate")) {
					for (JsonElement ele : obj.get("components").getAsJsonArray()) {
						JsonObject data = ele.getAsJsonObject();
						if (data.get("componentClass").getAsString().equals("SocialExpanseDefComponent")) {
							data = data.get("componentJSON").getAsJsonObject();

							// Quest object
							JsonObject quest = new JsonObject();
							quest.addProperty("defID", lastID);
							quest.addProperty("name", lastName);
							quest.addProperty("levelOverrideID",
									data.get("linearQuestLevelOverrideDefId").getAsNumber());

							// Objectives
							JsonArray objs = new JsonArray();
							JsonArray _objs = data.get("primaryObjectives").getAsJsonArray();
							for (JsonElement objectiveDataE : _objs) {
								JsonObject objInfo = objectiveDataE.getAsJsonObject();

								// Objective block
								JsonObject objective = new JsonObject();
								objective.addProperty("title",
										localization.get(objInfo.get("localizedTitleDefID").getAsString()));
								objective.addProperty("isLastObjective",
										objInfo.get("isFinalObjective").getAsBoolean());

								// Tasks
								JsonArray tasks = new JsonArray();
								JsonArray objTasks = objInfo.get("tasks").getAsJsonArray();
								for (JsonElement tEle : objTasks) {
									JsonObject tskInfo = tEle.getAsJsonObject();
									JsonObject task = new JsonObject();
									task.addProperty("targetProgress", tskInfo.get("targetProgress").getAsNumber());
									tasks.add(task);
								}

								objective.add("tasks", tasks);

								objs.add(objective);
							}
							quest.add("objectives", objs);

							quests.add(lastID, quest);
						} else if (data.get("componentClass").getAsString().equals("LocalizedNameDefComponent")) {
							data = data.get("componentJSON").getAsJsonObject();
							String localizedID = data.get("localizedDefID").getAsString();

							JsonObject quest = quests.get(lastID).getAsJsonObject();
							quest.addProperty("name", localization.get(localizedID));
						}
					}
				}
			} else if (lastData != null) {
				lastData += line + "\n";
			}
		}
		res.add("Quests", quests);
		System.out.println(new Gson().newBuilder().setPrettyPrinting().create().toJson(res));
	}

}
