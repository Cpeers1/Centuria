package org.asf.centuria.tools;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class UserVarsConverter {

	public static void main(String[] args) throws IOException {
		// This tool generates a userVars.json file
		// Expected program arguments: <csv-file>

		JsonObject res = new JsonObject();
		JsonObject userVars = new JsonObject();
		String lastData = null;
		String id = "";
		
		JsonObject userVarDefinition = new JsonObject();
		boolean skip = false;
		for (String line : Files.readAllLines(Path.of(args[0]))) {
			
			if (line.startsWith("\"") && !line.startsWith("\"\"")) {
				if (line.contains("REUSE"))
					skip = true;
				
				//start a new uservar definition
				userVarDefinition = new JsonObject();
				
				//split up the line based on ','
				String[] lineParts = line.split(",");
				
				//0 - DefID (NPC ID)
				id = lineParts[0].substring(1);
				
				//1 - DefName (Change this to userVarName, nicer)
				userVarDefinition.addProperty("userVarName", lineParts[1].replace("\"", ""));
				
				lastData = "";
			} else if (line.startsWith("\"\"")) {
				if(!skip)
				{
					System.out.println("Parsing user var ID: " + id);
					
					lastData = "{\n" + lastData;
					lastData = lastData.replace("`", "\"");
					JsonObject jsonObject = JsonParser.parseString(lastData).getAsJsonObject();
					
					var component = jsonObject.get("components").getAsJsonArray()
							.get(0).getAsJsonObject()
							.get("componentJSON").getAsJsonObject();
					
					userVarDefinition.addProperty("type", component.get("type").getAsInt());
					userVarDefinition.addProperty("clientAllowedToSet", component.get("clientAllowedToSet").getAsBoolean());
					userVarDefinition.addProperty("allowMultiples", component.get("allowMultiples").getAsBoolean());
					userVarDefinition.addProperty("defaultValue", component.get("defaultValue").getAsInt());
					
					//add the uservar to the uservar list
					userVars.add(id, userVarDefinition);				
				}
				
				//start a fresh userVar definition
				userVarDefinition = new JsonObject();	
				lastData = null;
				skip = false;
			} else if (lastData != null) {
				lastData += line + "\n";
			}
		}
		res.add("userVars", userVars);
		
		//blegh hard coded path 
        String path = "userVars.json";

        try (PrintWriter out = new PrintWriter(new FileWriter(path))) {
            String jsonString = new Gson().newBuilder().setPrettyPrinting().create().toJson(res);
            out.write(jsonString);
        } catch (Exception e) {
            e.printStackTrace();
        }
	}
}
