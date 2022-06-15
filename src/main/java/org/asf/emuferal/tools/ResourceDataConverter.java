package org.asf.emuferal.tools;

import java.io.IOException;
import java.nio.file.*;
import java.util.HashMap;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ResourceDataConverter {

	private static enum ResourceType {
		HARVEST, LOOT
	}

	public static void main(String[] args) throws IOException {
		// This tool generates a resourcecollection.json file
		//
		// Expected program arguments:
		// <interactables-csv-file> <harvestpoint-csv-file> <loot-csv-file>

		String lastName = "";
		String lastData = null;
		String lastID = null;
		JsonObject res = new JsonObject();
		JsonObject harvestTables = new JsonObject();
		HashMap<String, Integer> harvestCounts = new HashMap<String, Integer>();
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
				if (obj.get("templateClass").getAsString().equals("HarvestPointTemplate")) {
					for (JsonElement ele : obj.get("components").getAsJsonArray()) {
						JsonObject data = ele.getAsJsonObject();
						if (data.get("componentClass").getAsString().equals("HarvestPointDefComponent")) {
							data = data.get("componentJSON").getAsJsonObject();

							int i = 0;
							JsonArray rewards = new JsonArray();
							for (JsonElement ele2 : data.get("craftableItemRefArray").getAsJsonObject().get("_defIDs")
									.getAsJsonArray()) {
								String id = ele2.getAsString();
								int min = data.get("minimumCraftableItemsPerHarvest").getAsJsonArray().get(i)
										.getAsInt();
								int max = data.get("maximumCraftableItemsPerHarvest").getAsJsonArray().get(i)
										.getAsInt();
								int percentage = data.get("percentChance").getAsJsonArray().get(i).getAsInt();

								JsonObject itm = new JsonObject();
								itm.addProperty("itemId", id);
								itm.addProperty("minCount", min);
								itm.addProperty("maxCount", max);
								itm.addProperty("weight", percentage);
								rewards.add(itm);

								i++;
							}

							JsonObject info = new JsonObject();
							info.addProperty("objectName", lastName);
							info.add("rewards", rewards);
							harvestTables.add(lastID, info);
							harvestCounts.put(lastID, data.get("harvests").getAsInt());
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
		boolean skip = false;
		JsonObject lootTables = new JsonObject();
		for (String line : Files.readAllLines(Path.of(args[2]))) {
			if (line.startsWith("\"") && !line.startsWith("\"\"") && !line.startsWith("\"DefID")) {
				String id = line.substring(1);
				lastName = id.substring(id.indexOf(",\"\"") + 3);
				lastName = lastName.substring(0, lastName.indexOf("\""));
				id = id.substring(0, id.indexOf(","));
				lastID = id;
				lastData = "";
			} else if (line.startsWith("\"\"")) {
				if (!skip) {
					lastData = "{\n" + lastData;
					lastData = lastData.replace("`", "\"");
					JsonObject obj = JsonParser.parseString(lastData).getAsJsonObject();
					if (obj.get("templateClass").getAsString().equals("LootTemplate")) {
						for (JsonElement ele : obj.get("components").getAsJsonArray()) {
							JsonObject data = ele.getAsJsonObject();
							if (data.get("componentClass").getAsString().equals("LootDefComponent")) {
								data = data.get("componentJSON").getAsJsonObject();

								JsonArray rewards = new JsonArray();
								for (JsonElement choiceE : data.get("choices").getAsJsonArray()) {
									JsonObject choice = choiceE.getAsJsonObject();

									JsonObject itm = new JsonObject();
									boolean hasItem = !choice.get("itemDefID").getAsString().isEmpty()
											&& choice.get("itemDefID").getAsInt() != -1;
									boolean hasTableReference = !choice.get("lootTableDefID").getAsString().isEmpty()
											&& choice.get("lootTableDefID").getAsInt() != -1;
									itm.addProperty("hasItem", hasItem);
									itm.addProperty("hasTableReference", hasTableReference);
									if (hasItem)
										itm.addProperty("itemId", choice.get("itemDefID").getAsString());
									if (hasTableReference)
										itm.addProperty("referencedTableId",
												choice.get("lootTableDefID").getAsString());
									itm.addProperty("minCount", choice.get("minCount").getAsInt());
									itm.addProperty("maxCount", choice.get("maxCount").getAsInt());
									itm.addProperty("weight", choice.get("weight").getAsInt());
									rewards.add(itm);
								}

								JsonObject info = new JsonObject();
								info.addProperty("objectName", lastName);
								info.add("rewards", rewards);

								lootTables.add(lastID, info);
							}
						}
					}
				}

				lastData = null;
				lastID = null;
				skip = false;
			} else if (lastData != null) {
				lastData += line + "\n";
			}
		}
		lastData = null;
		lastID = null;
		lastName = null;
		JsonObject resources = new JsonObject();
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
				if (obj.get("templateClass").getAsString().equals("InteractableTemplate")) {
					JsonObject interactionInfo = null;
					JsonObject respawnInfo = null;
					for (JsonElement ele : obj.get("components").getAsJsonArray()) {
						JsonObject data = ele.getAsJsonObject();
						if (data.get("componentClass").getAsString().equals("InteractableDefComponent")) {
							interactionInfo = data.get("componentJSON").getAsJsonObject();
						} else if (data.get("componentClass").getAsString().equals("RespawnDefComponent")) {
							respawnInfo = data.get("componentJSON").getAsJsonObject();
						}
					}

					if (interactionInfo != null) {
						boolean isResource = ((!interactionInfo.get("harvestPointRef").isJsonNull()
								&& !interactionInfo.get("harvestPointRef").getAsString().isEmpty()
								&& interactionInfo.get("harvestPointRef").getAsInt() != -1)
								|| (!interactionInfo.get("lootPointRef").isJsonNull()
										&& !interactionInfo.get("lootPointRef").getAsString().isEmpty()
										&& interactionInfo.get("lootPointRef").getAsInt() != -1));
						if (isResource) {
							ResourceType type = (interactionInfo.get("harvestPointRef").getAsInt() != -1
									? ResourceType.HARVEST
									: ResourceType.LOOT);

							double respawnSecs = -1;
							if (respawnInfo != null)
								respawnSecs = respawnInfo.get("respawnTime").getAsDouble();
							int tableId;
							if (type == ResourceType.HARVEST)
								tableId = interactionInfo.get("harvestPointRef").getAsInt();
							else
								tableId = interactionInfo.get("lootPointRef").getAsInt();

							int harvests = 1;
							if (harvestCounts.containsKey(Integer.toString(tableId))) {
								harvests = harvestCounts.get(Integer.toString(tableId));
							}

							boolean valid = true;
							if (type == ResourceType.HARVEST && !harvestTables.has(Integer.toString(tableId))) {
								valid = false;
							} else if (type == ResourceType.LOOT && !lootTables.has(Integer.toString(tableId))) {
								valid = false;
							}

							if (valid) {
								JsonObject info = new JsonObject();
								info.addProperty("objectName", lastName);
								info.addProperty("lootType", type.toString());
								info.addProperty("lootTableId", tableId);
								info.addProperty("respawnSeconds", respawnSecs);
								info.addProperty("interactionsBeforeDespawn", harvests);
								resources.add(lastID, info);
							}
						}
					}
				}
				lastData = null;
			} else if (lastData != null) {
				lastData += line + "\n";
			}
		}

		res.add("Resources", resources);
		res.add("HarvestTables", harvestTables);
		res.add("LootTables", lootTables);

		System.out.println(new Gson().newBuilder().setPrettyPrinting().create().toJson(res));
	}

}
