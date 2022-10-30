package org.asf.centuria.tools;

import java.io.IOException;
import java.nio.file.*;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class WthDataConverter {

	public static void main(String[] args) throws IOException {
		// This tool generates a minigames/whatthehex.json file
		// Expected program arguments: <csv-file>

		JsonObject res = new JsonObject();
		JsonArray levels = new JsonArray();
		String lastName = "";
		String lastData = null;
		String lastID = null;

		for (String line : Files.readAllLines(Path.of(args[0]))) {
			if (line.startsWith("\"") && !line.startsWith("\"\"")) {
				lastID = line.substring(1);
				if (lastID.isEmpty())
					break;
				lastName = lastID.substring(lastID.indexOf(",\"\"") + 3);
				lastName = lastName.substring(0, lastName.indexOf("\""));
				lastID = lastID.substring(0, lastID.indexOf(","));
				lastData = "";
			} else if (line.startsWith("\"\"")) {
				lastData = "{\n" + lastData;
				lastData = lastData.replace("`", "\"");
				JsonObject obj = JsonParser.parseString(lastData).getAsJsonObject();
				if (obj.get("templateClass").getAsString().equals("RunesGameTemplate")) {
					for (JsonElement ele : obj.get("components").getAsJsonArray()) {
						JsonObject data = ele.getAsJsonObject();
						if (data.get("componentClass").getAsString().equals("RunesDataDefComponent")) {
							data = data.get("componentJSON").getAsJsonObject();

							JsonArray rewards = data.get("levelRewards").getAsJsonArray();
							for (JsonElement ele2 : rewards) {
								JsonObject info = ele2.getAsJsonObject();
								JsonObject level = new JsonObject();
								level.addProperty("levelMax", info.get("levelThreshold").getAsInt());

								JsonObject rewardInfo = new JsonObject();
								rewardInfo.add("flame", reward(info.get("flameLootDefId").getAsString()));
								rewardInfo.add("flora", reward(info.get("floraLootDefId").getAsString()));
								rewardInfo.add("miasma", reward(info.get("miasmaLootDefId").getAsString()));
								level.add("rewards", rewardInfo);

								levels.add(level);
							}
						}
					}
				}
				lastData = null;
			} else if (lastData != null) {
				lastData += line + "\n";
			}
		}
		res.add("Levels", levels);

		System.out.println(new Gson().newBuilder().setPrettyPrinting().create().toJson(res));
	}

	private static JsonObject reward(String table) {
		JsonObject info = new JsonObject();
		info.addProperty("type", "loot");
		info.addProperty("id", table);
		return info;
	}

}
