package org.asf.centuria.tools;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

import org.joml.Quaternionf;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class InteractableConverter {

	public static void main(String[] args) throws IOException {
		// This tool generates a interactables.json file
		// Expected program arguments: <csv-file>

		JsonObject res = new JsonObject();
		JsonObject interactables = new JsonObject();
		String interactableName = "";
		String lastData = null;
		String id = "";
		
		JsonObject interactableDefinition = new JsonObject();
		
		boolean skip = false;
		for (String line : Files.readAllLines(Path.of(args[0]))) {
			
			if (line.startsWith("\"") && !line.startsWith("\"\"")) {
				if (line.contains("REUSE"))
					skip = true;
				
				//start a new quest definition
				interactableDefinition = new JsonObject();
				
				//split up the line based on ','
				String[] lineParts = line.split(",");
				
				//0 - DefID (Interactable ID)
				id = lineParts[0].substring(1).replace("\"", "");
				
				//1 - DefName (Interactable Name)
				interactableDefinition.addProperty("interactableName", lineParts[1].replace("\"", ""));

				lastData = "";
			} else if (line.startsWith("\"\"")) {
				if(!skip)
				{
					System.out.println("Parsing interactable ID: " + id);
					
					lastData = "{\n" + lastData;
					lastData = lastData.replace("`", "\"");
					JsonObject obj = JsonParser.parseString(lastData).getAsJsonObject();
					if (obj.get("templateClass").getAsString().equals("InteractableTemplate")) {
						for (JsonElement ele : obj.get("components").getAsJsonArray()) {
							JsonObject data = ele.getAsJsonObject();
							
							if (data.get("componentClass").getAsString().equals("InteractableDefComponent")) {
								JsonObject interactableData = data.get("componentJSON").getAsJsonObject();
								
								//add it in as 'InteractableData'
								
								interactableDefinition.add("interactableData", interactableData);
							}
							
							if (data.get("componentClass").getAsString().equals("RespawnDefComponent")) {
								JsonObject respawnData = data.get("componentJSON").getAsJsonObject();
								
								//add it in as 'RespawnData'
								interactableDefinition.add("respawnData", respawnData);
							}
						}
						
										
						interactables.add(id, interactableDefinition);
					}			
				}
				else
				{
					System.out.println("skipping interactable ID: " + id);
				}
				
				interactableDefinition = new JsonObject();
				skip = false;
				lastData = null;
			} else if (lastData != null) {
				lastData += line + "\n";
			}
		}
		res.add("interactables", interactables);
		
		//blegh hard coded path 
        String path = "interactables.json";

        try (PrintWriter out = new PrintWriter(new FileWriter(path))) {
            String jsonString = new Gson().newBuilder().setPrettyPrinting().create().toJson(res);
            out.write(jsonString);
        } catch (Exception e) {
            e.printStackTrace();
        }
	}

}
