package org.asf.centuria.tools;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.*;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class InspirationCraftingConverter {

	public static void main(String[] args) throws IOException {
		// This tool generates a EngimaRecipes.json file
		// Expected program arguments: <csv-file>

		JsonObject res = new JsonObject();
		JsonObject craftables = new JsonObject();
		String lastData = null;
		String id = "";

		JsonObject craftableDefinition = new JsonObject();
		boolean skip = false;
		for (String line : Files.readAllLines(Path.of(args[0]))) {

			if (line.startsWith("\"") && !line.startsWith("\"\"")) {
				if (line.contains("REUSE"))
					skip = true;
				// start a new quest definition
				craftableDefinition = new JsonObject();

				// split up the line based on ','
				String[] lineParts = line.split(",");

				// 0 - DefID (NPC ID)
				id = lineParts[0].substring(1);

				// 1 - DefName (Change this to npcName, nicer)
				craftableDefinition.addProperty("itemName", lineParts[1].replace("\"", ""));

				lastData = "";
			} else if (line.startsWith("\"\"")) {
				if (!skip) {
					System.out.println("Parsing craftable ID: " + id);

					lastData = "{\n" + lastData;
					lastData = lastData.replace("`", "\"");

					// read recipe

					JsonObject obj = JsonParser.parseString(lastData).getAsJsonObject();
					var components = obj.get("components").getAsJsonArray();
					JsonObject component = null;

					for (var comp : components) {
						if (comp.getAsJsonObject().get("componentClass").getAsString().equals("EnigmaDefComponent")) {
							component = comp.getAsJsonObject();
						}
					}

					// Skip non-enigma items
					if (component == null) {
						continue;
					}

					// get recipe
					JsonObject recipe = component.get("componentJSON").getAsJsonObject().getAsJsonObject().get("recipe")
							.getAsJsonObject();

					if (recipe.get("_defIDs").getAsJsonArray().size() <= 0) {
						// skip
						craftableDefinition = new JsonObject();
						lastData = null;
						skip = false;
						System.out.println("Decided to skip craftable ID: " + id + ", no recipe");
						continue;
					}

					craftableDefinition.add("recipe", recipe);
					craftableDefinition.addProperty("resultItemId",
							component.get("componentJSON").getAsJsonObject().get("itemDefID").getAsString());

					// add the enigma to the enigma list
					craftables.add(id, craftableDefinition);
				}

				// start a fresh enigma definition
				craftableDefinition = new JsonObject();
				lastData = null;
				skip = false;
			} else if (lastData != null) {
				lastData += line + "\n";
			}
		}
		res.add("EnigmaRecipes", craftables);

		// blegh hard coded path
		String path = "enigmarecipes.json";

		try (PrintWriter out = new PrintWriter(new FileWriter(path))) {
			String jsonString = new Gson().newBuilder().setPrettyPrinting().create().toJson(res);
			out.write(jsonString);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
