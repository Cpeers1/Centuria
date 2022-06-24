package org.asf.emuferal.tools;

import java.nio.file.*;
import java.io.IOException;
import java.util.HashMap;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ShopConverter {

	public static void main(String[] args) throws IOException {
		// This tool generates a shops.json file
		//
		// Expected program arguments:
		// <shop-csv-file> <shoplist-csv-file> <craftableitemchart-csv-file>
		// <listchart-csv-file> <bundlepack-csv-file>

		String lastName = "";
		String lastData = null;
		String lastID = null;
		HashMap<String, JsonArray> lists = new HashMap<String, JsonArray>();
		HashMap<String, String> objectNames = new HashMap<String, String>();
		for (String line : Files.readAllLines(Path.of(args[3]))) {
			if (line.startsWith("\"") && !line.startsWith("\"\"") && !line.startsWith("\"DefID")) {
				lastID = line.substring(1);
				lastName = lastID.substring(lastID.indexOf(",\"\"") + 3);
				lastName = lastName.substring(0, lastName.indexOf("\""));
				lastID = lastID.substring(0, lastID.indexOf(","));
				lastData = "";

				objectNames.put(lastID, lastName);
			} else if (line.startsWith("\"\"")) {
				lastData = "{\n" + lastData;
				lastData = lastData.replace("`", "\"");
				JsonObject obj = JsonParser.parseString(lastData).getAsJsonObject();

				for (JsonElement ele : obj.get("components").getAsJsonArray()) {
					JsonObject data = ele.getAsJsonObject();
					if (data.get("componentClass").getAsString().equals("ListDefComponent")) {
						data = data.get("componentJSON").getAsJsonObject();
						if (data.has("list") && data.get("list").getAsJsonObject().has("_defIDs")) {
							JsonArray items = new JsonArray();
							for (JsonElement ele2 : data.get("list").getAsJsonObject().get("_defIDs").getAsJsonArray())
								items.add(ele2.getAsString());
							lists.put(lastID, items);
						}
					}
				}

				lastData = null;
			} else if (lastData != null) {
				lastData += line + "\n";
			}
		}

		HashMap<String, HashMap<String, Integer>> costs = new HashMap<String, HashMap<String, Integer>>();
		HashMap<String, HashMap<String, Integer>> eurekaItems = new HashMap<String, HashMap<String, Integer>>();

		lastName = "";
		lastData = null;
		lastID = null;
		for (String line : Files.readAllLines(Path.of(args[2]))) {
			if (line.startsWith("\"") && !line.startsWith("\"\"") && !line.startsWith("\"DefID")) {
				lastID = line.substring(1);
				lastName = lastID.substring(lastID.indexOf(",\"\"") + 3);
				lastName = lastName.substring(0, lastName.indexOf("\""));
				lastID = lastID.substring(0, lastID.indexOf(","));
				lastData = "";

				objectNames.put(lastID, lastName);
			} else if (line.startsWith("\"\"")) {
				lastData = "{\n" + lastData;
				lastData = lastData.replace("`", "\"");
				JsonObject obj = JsonParser.parseString(lastData).getAsJsonObject();

				for (JsonElement ele : obj.get("components").getAsJsonArray()) {
					JsonObject data = ele.getAsJsonObject();
					if (data.get("componentClass").getAsString().equals("PurchaseableDefComponent")) {
						data = data.get("componentJSON").getAsJsonObject();
						HashMap<String, Integer> cost = new HashMap<String, Integer>();
						for (JsonElement elem : data.get("_cost").getAsJsonArray()) {
							JsonObject costObj = elem.getAsJsonObject();
							String itm = costObj.get("itemDefID").getAsString();
							int count = costObj.get("count").getAsInt();
							cost.put(itm, count);
						}
						costs.put(lastID, cost);
					} else if (data.get("componentClass").getAsString().equals("PurchaseRareChanceDefComponent")) {
						data = data.get("componentJSON").getAsJsonObject();
						if (!data.get("itemDefID").getAsString().isEmpty()) {
							HashMap<String, Integer> itemChances;
							if (!eurekaItems.containsKey(lastID)) {
								itemChances = new HashMap<String, Integer>();
								eurekaItems.put(lastID, itemChances);
							} else
								itemChances = eurekaItems.get(lastID);

							String id = data.get("itemDefID").getAsString();
							int chance = data.get("rareChance").getAsInt();
							itemChances.put(id, chance);
						}
					}
				}

				lastData = null;
			} else if (lastData != null) {
				lastData += line + "\n";
			}
		}

		lastName = "";
		lastData = null;
		lastID = null;
		HashMap<String, HashMap<String, Integer>> bundles = new HashMap<String, HashMap<String, Integer>>();
		for (String line : Files.readAllLines(Path.of(args[4]))) {
			if (line.startsWith("\"") && !line.startsWith("\"\"") && !line.startsWith("\"DefID")) {
				lastID = line.substring(1);
				lastName = lastID.substring(lastID.indexOf(",\"\"") + 3);
				lastName = lastName.substring(0, lastName.indexOf("\""));
				lastID = lastID.substring(0, lastID.indexOf(","));
				lastData = "";

				objectNames.put(lastID, lastName);
			} else if (line.startsWith("\"\"")) {
				lastData = "{\n" + lastData;
				lastData = lastData.replace("`", "\"");
				JsonObject obj = JsonParser.parseString(lastData).getAsJsonObject();

				for (JsonElement ele : obj.get("components").getAsJsonArray()) {
					JsonObject data = ele.getAsJsonObject();
					if (data.get("componentClass").getAsString().equals("PurchaseableDefComponent")) {
						data = data.get("componentJSON").getAsJsonObject();
						HashMap<String, Integer> cost = new HashMap<String, Integer>();
						for (JsonElement elem : data.get("_cost").getAsJsonArray()) {
							JsonObject costObj = elem.getAsJsonObject();
							String itm = costObj.get("itemDefID").getAsString();
							int count = costObj.get("count").getAsInt();
							cost.put(itm, count);
						}
						costs.put(lastID, cost);
					} else if (data.get("componentClass").getAsString().equals("BundlePackDefComponent")) {
						data = data.get("componentJSON").getAsJsonObject();
						HashMap<String, Integer> items = new HashMap<String, Integer>();
						for (JsonElement elem : data.get("_craftableItems").getAsJsonArray()) {
							JsonObject itmObj = elem.getAsJsonObject();
							String itm = itmObj.get("itemDefID").getAsString();
							int count = itmObj.get("count").getAsInt();
							items.put(itm, count);
						}
						bundles.put(lastID, items);
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

		HashMap<String, JsonObject> contents = new HashMap<String, JsonObject>();
		for (String line : Files.readAllLines(Path.of(args[1]))) {
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
					if (obj.get("templateClass").getAsString().equals("ShopContentTemplate")) {
						for (JsonElement ele : obj.get("components").getAsJsonArray()) {
							JsonObject data = ele.getAsJsonObject();
							if (data.get("componentClass").getAsString().equals("ListDefComponent")) {
								data = data.get("componentJSON").getAsJsonObject();

								JsonObject items = new JsonObject();
								for (JsonElement lI : data.get("list").getAsJsonObject().get("_defIDs")
										.getAsJsonArray()) {
									String id = lI.getAsString();
									if (costs.containsKey(id)
											&& !objectNames.get(id).startsWith("AstralShop/ALL/SeasonPass/")) {
										JsonObject costObj = new JsonObject();
										costs.get(id).forEach((costId, amount) -> {
											costObj.addProperty(costId, amount);
										});

										JsonObject itms = new JsonObject();
										addItems(itms, id, bundles, 1);

										JsonObject entry = new JsonObject();
										entry.addProperty("object", objectNames.get(id));

										if (lastID.equals("30549")) {
											// Astrale shop
											// Single-time purchases
											entry.addProperty("stock", 1);
										} else {
											// Other shop
											// Set to -1 to disable
											entry.addProperty("stock", -1);
										}

										entry.addProperty("requiredLevel", -1);
										entry.add("items", itms);
										entry.add("cost", costObj);

										if (eurekaItems.containsKey(id) && !eurekaItems.get(id).isEmpty()) {
											HashMap<String, Integer> itemChances = eurekaItems.get(id);
											JsonObject chanceObj = new JsonObject();
											itemChances.forEach((itmId, chance) -> {
												chanceObj.addProperty(itmId, chance);
											});
											entry.add("eurekaItems", chanceObj);
										}

										items.add(id, entry);
									}
								}

								if (items.size() != 0) {
									contents.put(lastID, items);
								}
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

		JsonObject res = new JsonObject();
		JsonObject shops = new JsonObject();

		lastData = null;
		lastID = null;
		lastName = null;
		skip = false;
		for (String line : Files.readAllLines(Path.of(args[0]))) {
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
					if (obj.get("templateClass").getAsString().equals("ShopTemplate")) {
						for (JsonElement ele : obj.get("components").getAsJsonArray()) {
							JsonObject data = ele.getAsJsonObject();
							if (data.get("componentClass").getAsString().equals("ShopContentDefComponent")) {
								data = data.get("componentJSON").getAsJsonObject();
								if (!data.has("isDebug") || !data.get("isDebug").getAsBoolean()) {
									if ((data.has("shopContentDefID")
											&& !data.get("shopContentDefID").getAsString().isEmpty()
											&& !data.get("shopContentDefID").getAsString().equals("-1"))
											|| (data.has("enigmaUnlockListDefID")
													&& !data.get("enigmaUnlockListDefID").getAsString().isEmpty()
													&& !data.get("enigmaUnlockListDefID").getAsString().equals("-1"))) {
										JsonObject shopInfo = new JsonObject();
										shopInfo.addProperty("object", lastName);
										shopInfo.addProperty("restockTime", -1);
										if (data.has("shopContentDefID")
												&& !data.get("shopContentDefID").getAsString().isEmpty()
												&& !data.get("shopContentDefID").getAsString().equals("-1")) {
											String id = data.get("shopContentDefID").getAsString();
											if (contents.containsKey(id))
												shopInfo.add("contents", contents.get(id));
											else
												shopInfo.add("contents", new JsonObject());
										} else
											shopInfo.add("contents", new JsonObject());
										if (data.has("enigmaUnlockListDefID")
												&& !data.get("enigmaUnlockListDefID").getAsString().isEmpty()
												&& !data.get("enigmaUnlockListDefID").getAsString().equals("-1")) {
											String id = data.get("enigmaUnlockListDefID").getAsString();
											if (lists.containsKey(id))
												shopInfo.add("enigmas", lists.get(id));
											else
												shopInfo.add("enigmas", new JsonArray());
										} else
											shopInfo.add("enigmas", new JsonArray());
										shops.add(lastID, shopInfo);
									}
								}
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
		res.add("Shops", shops);

		System.out.println(new Gson().newBuilder().setPrettyPrinting().create().toJson(res));
	}

	private static void addItems(JsonObject itms, String id, HashMap<String, HashMap<String, Integer>> bundles,
			int count) {
		if (!bundles.containsKey(id))
			itms.addProperty(id, count);
		else
			bundles.get(id).forEach((itm, c) -> addItems(itms, itm, bundles, c));
	}

}
