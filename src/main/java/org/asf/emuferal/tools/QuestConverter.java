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

public class QuestConverter {

	public static void main(String[] args) throws IOException {
		// This tool generates a quests.json file
		// Expected program arguments: <csv-file>

		JsonObject res = new JsonObject();
		JsonObject quests = new JsonObject();
		String questName = "";
		String lastData = null;
		String id = "";
		
		JsonObject questDefinition = new JsonObject();
		
		for (String line : Files.readAllLines(Path.of(args[0]))) {
			
			if (line.startsWith("\"") && !line.startsWith("\"\"")) {
				//start a new quest definition
				questDefinition = new JsonObject();
				
				//split up the line based on ','
				String[] lineParts = line.split(",");
				
				//0 - DefID (Quest ID)
				id = lineParts[0].substring(1);
				
				//1 - challenge affiliation (seems unused?)
				questDefinition.addProperty("challengeAffiliation", lineParts[1]);
				
				//2 - DefName (Quest Name)
				questDefinition.addProperty("questName", lineParts[2].replace("\"", ""));
				
				//3 - imageRef (?)
				questDefinition.addProperty("imageRef", lineParts[3]);
				
			    //4 - locGiverName
				questDefinition.addProperty("locGiverName", lineParts[4]);
				
			    //5 - LocLocation
				questDefinition.addProperty("locLocation", lineParts[5]);
				
			    //6 - Difficulty
				questDefinition.addProperty("difficulty", lineParts[6]);
				
			    //7 - isPlatformer
				questDefinition.addProperty("isPlatformer", lineParts[7]);
				
			    //8 - PlayAsPet (What??)
				questDefinition.addProperty("playAsPet", lineParts[8]);
				
			    //9 - NoLobby 
				questDefinition.addProperty("noLobby", lineParts[9]);
				
			    //10 - LocTitle
				questDefinition.addProperty("locTitle", lineParts[10]);
				
			    //11 - LocDescription
				questDefinition.addProperty("locDescription", lineParts[11]);

			    //12 - BaseRooms
				questDefinition.addProperty("baseRooms", lineParts[12]);
				
				//13 - QuestLevelOverrides
				questDefinition.addProperty("questLevelOverrides", lineParts[13]);
					
				//14 - PrimaryObjectiveList
				questDefinition.addProperty("primaryObjectiveList", lineParts[14]);
								
				//15 - SecondaryObjectiveLists
				questDefinition.addProperty("secondaryObjectiveLists", lineParts[15]);
													
				//16 - MinPlayers
				questDefinition.addProperty("minPlayers", lineParts[16]);
										
				//17 - MaxPlayers
				questDefinition.addProperty("maxPlayers", lineParts[17]);
										
				//18 - GameImage
				questDefinition.addProperty("gameImage", lineParts[18]);
												
				//19 - Banner
				questDefinition.addProperty("banner", lineParts[19]);
								
				//20 - SummaryInfo
				questDefinition.addProperty("summaryInfo", lineParts[20].replace("\"", ""));
				
				//21 - AllCompleteLootRef
				questDefinition.addProperty("allCompleteLootRef", lineParts[21]);

				lastData = "";
			} else if (line.startsWith("\"\"")) {
				lastData = "{\n" + lastData;
				lastData = lastData.replace("`", "\"");
				JsonObject obj = JsonParser.parseString(lastData).getAsJsonObject();
				if (obj.get("templateClass").getAsString().equals("SocialExpanseTemplate")) {
					for (JsonElement ele : obj.get("components").getAsJsonArray()) {
						JsonObject data = ele.getAsJsonObject();
						
						if (data.get("componentClass").getAsString().equals("SocialExpanseDefComponent")) {
							data = data.get("componentJSON").getAsJsonObject();
							
							questDefinition.addProperty("linearQuestLevelOverrideDefId", data.get("linearQuestLevelOverrideDefId").getAsString());
							questDefinition.addProperty("linearQuestAudioLevelOverrideDefId", data.get("linearQuestAudioLevelOverrideDefId").getAsString());
							questDefinition.addProperty("baselevelRemoveLevelOverrideDefId", data.get("baselevelRemoveLevelOverrideDefId").getAsString());
							questDefinition.addProperty("alwaysActive", data.get("alwaysActive").getAsBoolean());	
							questDefinition.addProperty("socialExpanseArea", data.get("socialExpanseArea").getAsInt());								
							questDefinition.addProperty("questGiverNameDefId", data.get("questGiverNameDefId").getAsInt());					
							questDefinition.addProperty("questGiverIconDefId", data.get("questGiverIconDefId").getAsInt());	
							
							//primary objective object probably does not need to be changed, so ill just add it as it is
							questDefinition.add("primaryObjectives", data.get("primaryObjectives").getAsJsonArray());
							
							quests.add(id, questDefinition);
						}
					}
				}
				lastData = null;
			} else if (lastData != null) {
				lastData += line + "\n";
			}
		}
		res.add("Quests", quests);
		
		//blegh hard coded path 
        String path = "quests.json";

        try (PrintWriter out = new PrintWriter(new FileWriter(path))) {
            String jsonString = new Gson().newBuilder().setPrettyPrinting().create().toJson(res);
            out.write(jsonString);
        } catch (Exception e) {
            e.printStackTrace();
        }
	}

}
