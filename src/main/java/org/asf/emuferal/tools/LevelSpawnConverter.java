package org.asf.emuferal.tools;

import java.io.IOException;
import java.nio.file.*;

import org.joml.Quaternionf;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class LevelSpawnConverter {

	public static void main(String[] args) throws IOException {
		// This tool generates a spawns.json file
		// Expected program arguments: <csv-file>

		JsonObject res = new JsonObject();
		JsonObject spawns = new JsonObject();
		JsonObject maps = new JsonObject();
		String lastMapName = "";
		String lastData = null;
		String lastID = null;

		for (String line : Files.readAllLines(Path.of(args[0]))) {
			if (line.startsWith("\"") && !line.startsWith("\"\"")) {
				lastID = line.substring(1);
				lastMapName = lastID.substring(lastID.indexOf(",\"\"") + 3);
				lastMapName = lastMapName.substring(0, lastMapName.indexOf("\""));
				lastID = lastID.substring(0, lastID.indexOf(","));
				lastData = "";
			} else if (line.startsWith("\"\"")) {
				lastData = "{\n" + lastData;
				lastData = lastData.replace("`", "\"");
				JsonObject obj = JsonParser.parseString(lastData).getAsJsonObject();
				if (obj.get("templateClass").getAsString().equals("LevelTemplate")) {
					for (JsonElement ele : obj.get("components").getAsJsonArray()) {
						JsonObject data = ele.getAsJsonObject();
						if (data.get("componentClass").getAsString().equals("LevelDefComponent")) {
							data = data.get("componentJSON").getAsJsonObject();
							for (JsonElement mapE : data.get("worldMapInfos").getAsJsonArray()) {
								JsonObject map = mapE.getAsJsonObject();
								String id = map.get("uniqueMapId").getAsString();
								if (lastMapName.equals("TutorialCityFera"))
									continue;
								if (spawns.has(lastID + "/" + id))
									spawns.remove(lastID + "/" + id);
								if (!maps.has(lastID))
									maps.addProperty(lastID, lastMapName);

								JsonObject spawnData = new JsonObject();
								spawnData.addProperty("worldID", lastMapName);
								spawnData.addProperty("spawnX",
										map.get("scenePosition").getAsJsonObject().get("x").getAsDouble());
								spawnData.addProperty("spawnY",
										map.get("scenePosition").getAsJsonObject().get("y").getAsDouble());
								spawnData.addProperty("spawnZ",
										map.get("scenePosition").getAsJsonObject().get("z").getAsDouble());

								float x = (float) Math
										.toRadians(map.get("sceneRotation").getAsJsonObject().get("x").getAsFloat());
								float y = (float) Math
										.toRadians(map.get("sceneRotation").getAsJsonObject().get("y").getAsFloat());
								float z = (float) Math
										.toRadians(map.get("sceneRotation").getAsJsonObject().get("z").getAsFloat());
								Quaternionf quat = new Quaternionf();
								quat.rotateXYZ((float) x, (float) y, (float) z);
								quat.normalize();
								spawnData.addProperty("spawnRotX", quat.x);
								spawnData.addProperty("spawnRotY", quat.y);
								spawnData.addProperty("spawnRotZ", quat.z);
								spawnData.addProperty("spawnRotW", quat.w);

								spawns.add(lastID + "/" + id, spawnData);
							}
						}
					}
				}
				lastData = null;
			} else if (lastData != null) {
				lastData += line + "\n";
			}
		}
		res.add("Spawns", spawns);
		res.add("Maps", maps);

		System.out.println(new Gson().newBuilder().setPrettyPrinting().create().toJson(res));
	}

}
