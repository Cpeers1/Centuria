package org.asf.emuferal.tools;

import java.io.Console;
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

public class ActorNpcConverter {

	public static void main(String[] args) throws IOException {
		// This tool generates a npcs.json file
		// Expected program arguments: <csv-file>

		JsonObject res = new JsonObject();
		JsonObject npcs = new JsonObject();
		String lastData = null;
		String id = "";
		
		JsonObject npcDefinition = new JsonObject();
		boolean skip = false;
		for (String line : Files.readAllLines(Path.of(args[0]))) {
			
			if (line.startsWith("\"") && !line.startsWith("\"\"")) {
				if (line.contains("REUSE"))
					skip = true;
				//start a new quest definition
				npcDefinition = new JsonObject();
				
				//split up the line based on ','
				String[] lineParts = line.split(",");
				
				//0 - DefID (NPC ID)
				id = lineParts[0].substring(1);
				
				//1 - DefName (Change this to npcName, nicer)
				npcDefinition.addProperty("npcName", lineParts[1].replace("\"", ""));
							
				//2 - LocalizedName
				npcDefinition.addProperty("localizedName", lineParts[2].replace("\"", ""));
				
				//3 is serialized actor json...
				//4 is data json...
		
				lastData = "";
			} else if (line.startsWith("\"\"")) {
				if(!skip)
				{
					System.out.println("Parsing npc ID: " + id);
					
					lastData = "{\n" + lastData;
					lastData = lastData.replace("`", "\"");
			
					//can it be split again?
					String[] splitResults = lastData.split("\"\",\"\"");
					
					if(splitResults.length > 1)
					{
						//two parts: actor data and npc data
						String actorData = splitResults[0];
						npcDefinition.add("actorData", JsonParser.parseString(actorData).getAsJsonObject());
						
						String npcData = splitResults[1];
						npcDefinition.add("npcData", JsonParser.parseString(npcData).getAsJsonObject());
					}
					else
					{
						//just one part.. what kind of data is it?
						String data = splitResults[0];
						
						JsonObject jsonObject = JsonParser.parseString(data).getAsJsonObject();
						
						if(jsonObject.get("LocalizedName") != null)
						{
							//its actor data
							npcDefinition.add("actorData", jsonObject);
						}
						else
						{
							//its npc data
							npcDefinition.add("npcData", jsonObject);
						}

					}
					
					//add the npc to the npcs list
					npcs.add(id, npcDefinition);
					
				
				}
				
				//start a fresh npc definition
				npcDefinition = new JsonObject();	
				lastData = null;
				skip = false;
			} else if (lastData != null) {
				lastData += line + "\n";
			}
		}
		res.add("Npcs", npcs);
		
		//blegh hard coded path 
        String path = "npcs.json";

        try (PrintWriter out = new PrintWriter(new FileWriter(path))) {
            String jsonString = new Gson().newBuilder().setPrettyPrinting().create().toJson(res);
            out.write(jsonString);
        } catch (Exception e) {
            e.printStackTrace();
        }
	}

}
