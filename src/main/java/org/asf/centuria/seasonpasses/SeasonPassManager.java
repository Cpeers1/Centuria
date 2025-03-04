package org.asf.centuria.seasonpasses;

import java.io.File;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.TimeZone;

import org.apache.logging.log4j.MarkerManager;
import org.asf.centuria.Centuria;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * 
 * Season Pass Manager
 * 
 * @author Sky Swimmer
 *
 */
public class SeasonPassManager {

	private static HashMap<String, SeasonPassDefinition> passes = new HashMap<String, SeasonPassDefinition>();

	static {
		// Load passes
		Centuria.logger.info(MarkerManager.getMarker("SeasonManager"), "Loading season passes...");
		File passDir = new File("seasonpasses");
		if (!passDir.exists())
			passDir.mkdirs();

		// Time format
		SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		fmt.setTimeZone(TimeZone.getTimeZone("UTC"));

		// Read all passes from the folder
		for (File f : passDir.listFiles(t -> t.getName().endsWith(".json") && !t.isDirectory())) {
			try {
				// Read pass
				Centuria.logger.debug(MarkerManager.getMarker("SeasonManager"), "Loading pass file: " + f.getName());
				String passJson = Files.readString(f.toPath());
				JsonObject obj = JsonParser.parseString(passJson).getAsJsonObject();

				// Load data
				SeasonPassDefinition def = new SeasonPassDefinition();
				def.name = obj.get("name").getAsString();
				def.objectName = obj.get("objectName").getAsString();
				def.passDefID = obj.get("passDefID").getAsString();
				def.startDate = fmt.parse(obj.get("startDate").getAsString()).getTime();
				def.endDate = fmt.parse(obj.get("endDate").getAsString()).getTime();
				def.premiumPassID = obj.get("premiumPassID").getAsString();
				obj.get("tiers").getAsJsonArray().forEach(t -> def.tiers.add(t.getAsString()));
				obj.get("challenges").getAsJsonArray().forEach(t -> def.challenges.add(t.getAsString()));
				passes.put(def.passDefID, def);
				Centuria.logger.info(MarkerManager.getMarker("SeasonManager"),
						"Loaded season pass " + def.passDefID + ": " + def.name);
				Centuria.logger.debug(MarkerManager.getMarker("SeasonManager"),
						def.challenges.size() + " challenges loaded, " + def.tiers.size() + " tiers loaded.");
			} catch (Exception e) {
				Centuria.logger.error(MarkerManager.getMarker("SeasonManager"),
						"Failed to load season pass: " + f.getName(), e);
			}
		}

		// Find current
		SeasonPassDefinition current = getCurrentPass();
		if (current == null)
			Centuria.logger.info(MarkerManager.getMarker("SeasonManager"), "Current season pass: none");
		else
			Centuria.logger.info(MarkerManager.getMarker("SeasonManager"),
					"Current season pass: " + current.passDefID + ", name: " + current.name + ", tiers: "
							+ current.tiers.size() + ", challenges: " + current.challenges.size());
	}

	/**
	 * Retrieves season passes by ID
	 * 
	 * @param defID Pass definition ID
	 * @return Season pass definition or null
	 */
	public static SeasonPassDefinition getPass(int defID) {
		return getPass(Integer.toString(defID));
	}

	/**
	 * Retrieves season passes by ID
	 * 
	 * @param defID Pass definition ID
	 * @return Season pass definition or null
	 */
	public static SeasonPassDefinition getPass(String defID) {
		return passes.get(defID);
	}

	/**
	 * Retrieves the active season pass
	 * 
	 * @return Season pass definition or null
	 */
	public static SeasonPassDefinition getCurrentPass() {
		// Find pass
		for (SeasonPassDefinition pass : passes.values()) {
			if (System.currentTimeMillis() >= pass.startDate && System.currentTimeMillis() < pass.endDate)
				return pass; // Found it
		}

		// Not found
		return null;
	}

}
