package org.asf.emuferal.tools;

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

public class DialogConverter {

	public static void main(String[] args) throws IOException {
		// This tool generates a quests.json file
		// Expected program arguments: <csv-file>

		JsonObject res = new JsonObject();
		JsonObject dialogues = new JsonObject();
		String dialogueName = "";
		String lastData = null;
		String id = "";
		
		JsonObject dialogueDefinition = new JsonObject();
		
		boolean skip = false;
		for (String line : Files.readAllLines(Path.of(args[0]))) {
			
			if (line.startsWith("\"") && !line.startsWith("\"\"")) {
				if (line.contains("REUSE"))
					skip = true;
				
				//start a new quest definition
				dialogueDefinition = new JsonObject();
				
				//split up the line based on ','
				String[] lineParts = line.split(",");
				
				//0 - DefID (Dialogue ID)
				id = lineParts[0].substring(1).replace("\"", "");
				
				//1 - DefName (Dialogue Name)
				dialogueDefinition.addProperty("dialogueName", lineParts[1].replace("\"", ""));

				lastData = "";
			} else if (line.startsWith("\"\"")) {
				if(!skip)
				{
					System.out.println("Parsing dialogue ID: " + id);
					
					lastData = "{\n" + lastData;
					lastData = lastData.replace("`", "\"");
					JsonObject obj = JsonParser.parseString(lastData).getAsJsonObject();
					if (obj.get("templateClass").getAsString().equals("DialogEventTemplate")) {
						for (JsonElement ele : obj.get("components").getAsJsonArray()) {
							JsonObject data = ele.getAsJsonObject();
							
							if (data.get("componentClass").getAsString().equals("DialogDefComponent")) {
								JsonObject dialogueData = data.get("componentJSON").getAsJsonObject();
								
								//add it in as 'dialogueData'
								
								dialogueDefinition.add("dialogueData", dialogueData);
							}
														
							if (data.get("componentClass").getAsString().equals("ExpressionDefComponent")) {
								JsonObject expressionData = data.get("componentJSON").getAsJsonObject();
								
								//add it in as 'expressionData'
								
								dialogueDefinition.add("expressionData", expressionData);
							}
							
							if (data.get("componentClass").getAsString().equals("AudioDefComponent")) {
								JsonObject audioData = data.get("componentJSON").getAsJsonObject();
								
								//add it in as 'audioData'
								
								dialogueDefinition.add("audioData", audioData);
							}
							
							if (data.get("componentClass").getAsString().equals("AnimationTriggerDefComponent")) {
								JsonObject animationData = data.get("componentJSON").getAsJsonObject();
								
								//add it in as 'animationData'
								
								dialogueDefinition.add("animationData", animationData);
							}
							
							if (data.get("componentClass").getAsString().equals("AvatarActionDefComponent")) {
								JsonObject actionData = data.get("componentJSON").getAsJsonObject();
								
								//add it in as 'actionData'
								
								dialogueDefinition.add("actionData", actionData);
							}
							
						}
						
										
						dialogues.add(id, dialogueDefinition);
					}			
				}
				else
				{
					System.out.println("skipping interactable ID: " + id);
				}
				
				dialogueDefinition = new JsonObject();
				skip = false;
				lastData = null;
			} else if (lastData != null) {
				lastData += line + "\n";
			}
		}
		res.add("dialogues", dialogues);
		
		//blegh hard coded path 
        String path = "dialogues.json";

        try (PrintWriter out = new PrintWriter(new FileWriter(path))) {
            String jsonString = new Gson().newBuilder().setPrettyPrinting().create().toJson(res);
            out.write(jsonString);
        } catch (Exception e) {
            e.printStackTrace();
        }
	}

}
