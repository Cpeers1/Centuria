package org.asf.emuferal.tools;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.*;
import java.util.Base64;
import java.util.HashMap;
import java.util.zip.GZIPInputStream;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class NetworkedObjectsConverter {

	public static void main(String[] args) throws IOException {
		// This tool generates a networkedobjects.json file
		// Expected program arguments: <networkedobjects-csv-file>
		// <level-overrides-csv-file> <level-csv-file>

		String lastName = "";
		String lastData = null;
		String lastID = null;
		HashMap<String, String> overrideMapper = new HashMap<String, String>();
		for (String line : Files.readAllLines(Path.of(args[2]))) {
			if (line.startsWith("\"") && !line.startsWith("\"\"")) {
				lastID = line.substring(1);
				lastName = lastID.substring(lastID.indexOf(",\"\"") + 3);
				lastName = lastName.substring(0, lastName.indexOf("\""));
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
							JsonArray overrides = data.get("levelOverrides").getAsJsonObject().get("_defIDs")
									.getAsJsonArray();
							for (JsonElement ele2 : overrides) {
								overrideMapper.put(ele2.getAsString(), lastID);
							}
						}
					}
				}
				lastData = null;
			} else if (lastData != null) {
				lastData += line + "\n";
			}
		}

		lastData = null;
		lastID = null;
		lastName = null;
		HashMap<String, String> idMapper = new HashMap<String, String>();
		for (String line : Files.readAllLines(Path.of(args[1]))) {
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
				if (obj.get("templateClass").getAsString().equals("LevelOverrideTemplate")) {
					for (JsonElement ele : obj.get("components").getAsJsonArray()) {
						JsonObject data = ele.getAsJsonObject();
						if (data.get("componentClass").getAsString().equals("LevelOverrideDefComponent")) {
							data = data.get("componentJSON").getAsJsonObject();
							for (JsonElement ele2 : data.get("networkedObjects").getAsJsonObject().get("_defIDs")
									.getAsJsonArray()) {
								if (overrideMapper.containsKey(lastID)) {
									String levelID = overrideMapper.get(lastID);
									idMapper.put(ele2.getAsString(), levelID);
								}
							}
						}
					}
				}
				lastData = null;
			} else if (lastData != null) {
				lastData += line + "\n";
			}
		}

		JsonObject res = new JsonObject();
		JsonObject objects = new JsonObject();
		lastData = null;
		lastID = null;
		boolean skip = false;
		for (String line : Files.readAllLines(Path.of(args[0]))) {
			if (line.startsWith("\"") && !line.startsWith("\"\"")) {
				String id = line.substring(1);
				id = id.substring(0, id.indexOf(","));
				lastID = id;
				lastData = "";
			} else if (line.startsWith("\"\"")) {
				if (!skip && idMapper.containsKey(lastID)) {
					JsonObject worldObjects = new JsonObject();

					lastData = "{\n" + lastData;
					lastData = lastData.replace("`", "\"");
					JsonObject obj = JsonParser.parseString(lastData).getAsJsonObject();
					if (obj.get("templateClass").getAsString().equals("NetworkedObjectsTemplate")) {
						for (JsonElement ele : obj.get("components").getAsJsonArray()) {
							JsonObject data = ele.getAsJsonObject();
							if (data.get("componentClass").getAsString().equals("NetworkedObjectsDefComponent")) {
								data = data.get("componentJSON").getAsJsonObject();
								String payloadCompressed = data.get("scriptJson").getAsString();

								GZIPInputStream strm = new GZIPInputStream(
										new ByteArrayInputStream(Base64.getDecoder().decode(payloadCompressed)));
								JsonObject payload = JsonParser.parseString(new String(strm.readAllBytes(), "UTF-8"))
										.getAsJsonObject();
								strm.close();

								JsonArray actors = payload.get("actors").getAsJsonArray();
								for (JsonElement ele2 : actors) {
									JsonObject actor = ele2.getAsJsonObject();
									String uuid = actor.get("uuid").getAsString();

									JsonObject infoObject = new JsonObject();
									infoObject.addProperty("objectName", actor.get("name").getAsString());
									infoObject.addProperty("localType", actor.get("local").getAsInt());
									JsonObject primType = new JsonObject();
									primType.addProperty("type", actor.get("type").getAsInt());
									primType.addProperty("defId", actor.get("defId").getAsInt());
									infoObject.add("primaryObjectInfo", primType);
									if (actor.has("subType") && actor.has("subDefId")) {
										JsonObject subType = new JsonObject();
										if (actor.has("subType"))
											subType.addProperty("type", actor.get("subType").getAsInt());
										subType.addProperty("defId", actor.get("subDefId").getAsInt());
										infoObject.add("subObjectInfo", subType);
									}
									JsonObject locInfo = new JsonObject();
									locInfo.add("position", actor.get("position"));
									locInfo.add("rotation", actor.get("rotation"));
									infoObject.add("locationInfo", locInfo);

									JsonObject stateInfo = new JsonObject();
									if (actor.has("states") && !actor.get("states").isJsonNull())
										for (JsonElement stateE : actor.get("states").getAsJsonArray()) {
											JsonObject state = stateE.getAsJsonObject();
											addState(stateInfo, state);
										}
									infoObject.add("stateInfo", stateInfo);

									worldObjects.add(uuid, infoObject);
								}
							}
						}
					}

					objects.add(idMapper.get(lastID), worldObjects);
				}

				lastData = null;
				lastID = null;
				skip = false;
			} else if (lastData != null) {
				lastData += line + "\n";
			}
		}
		res.add("Objects", objects);

		System.out.println(new Gson().newBuilder().setPrettyPrinting().create().toJson(res));
	}

	private static void addState(JsonObject stateInfo, JsonObject state) {
		String name = state.get("name").getAsString();
		JsonObject commands = new JsonObject();
		for (JsonElement cmdE : state.get("cmds").getAsJsonArray()) {
			JsonObject cmd = cmdE.getAsJsonObject();
			JsonObject branches = new JsonObject();
			if (cmd.has("branches"))
				for (JsonElement b : cmd.get("branches").getAsJsonArray()) {
					JsonObject branch = b.getAsJsonObject();
					addState(branches, branch);
				}
			JsonObject command = new JsonObject();
			command.add("params", cmd.get("cmdParams"));
			command.addProperty("actorId", cmd.get("actor").getAsString());
			command.add("branches", branches);
			commands.add(cmd.get("cmd").getAsString(), command);
		}
		stateInfo.add(name, commands);
	}

}
