package org.asf.centuria.accounts;

import org.asf.centuria.Centuria;

import com.google.gson.JsonObject;

/**
 * 
 * Player Data Save Settings Container
 * 
 * @since Beta 1.5.3
 * @author Sky Swimmer
 *
 */
public class SaveSettings {

	public int sanctuaryLimitOverride = -1;
	public boolean giveAllAvatars = true;
	public boolean giveAllMods = true;
	public boolean giveAllClothes = true;
	public boolean giveAllWings = true;
	public boolean giveAllFurnitureItems = true;
	public boolean giveAllSanctuaryTypes = true;
	public boolean giveAllCurrency = true;
	public boolean giveAllResources = true;
	public JsonObject saveColors = null;
	public String tradeLockID = "default";

	public SaveSettings() {
		this.giveAllAvatars = Centuria.defaultGiveAllAvatars;
		this.giveAllClothes = Centuria.defaultGiveAllClothes;
		this.giveAllMods = Centuria.defaultGiveAllMods;
		this.giveAllWings = Centuria.defaultGiveAllWings;
		this.giveAllFurnitureItems = Centuria.defaultGiveAllFurnitureItems;
		this.giveAllSanctuaryTypes = Centuria.defaultGiveAllSanctuaryTypes;
		this.giveAllCurrency = Centuria.defaultGiveAllCurrency;
		this.giveAllResources = Centuria.defaultGiveAllResources;
	}

	/**
	 * Loads save settings
	 * 
	 * @param data Save settings object
	 */
	public void load(JsonObject data) {
		this.sanctuaryLimitOverride = data.get("sanctuaryLimitOverride").getAsInt();
		this.giveAllAvatars = data.get("giveAllAvatars").getAsBoolean();
		this.giveAllClothes = data.get("giveAllClothes").getAsBoolean();
		this.giveAllMods = data.get("giveAllMods").getAsBoolean();
		this.giveAllWings = data.get("giveAllWings").getAsBoolean();
		this.giveAllFurnitureItems = data.get("giveAllFurnitureItems").getAsBoolean();
		this.giveAllSanctuaryTypes = data.get("giveAllSanctuaryTypes").getAsBoolean();
		this.giveAllCurrency = data.get("giveAllCurrency").getAsBoolean();
		this.giveAllResources = data.get("giveAllResources").getAsBoolean();
		this.tradeLockID = data.get("tradeLockID").getAsString();
		if (data.has("saveColors") && !data.get("saveColors").isJsonNull())
			this.saveColors = data.get("saveColors").getAsJsonObject();
	}

	/**
	 * Saves save settings
	 * 
	 * @return Save settings JSON
	 */
	public JsonObject writeToObject() {
		JsonObject obj = new JsonObject();
		obj.addProperty("sanctuaryLimitOverride", sanctuaryLimitOverride);
		obj.addProperty("giveAllAvatars", giveAllAvatars);
		obj.addProperty("giveAllClothes", giveAllClothes);
		obj.addProperty("giveAllMods", giveAllMods);
		obj.addProperty("giveAllWings", giveAllWings);
		obj.addProperty("giveAllFurnitureItems", giveAllFurnitureItems);
		obj.addProperty("giveAllSanctuaryTypes", giveAllSanctuaryTypes);
		obj.addProperty("giveAllCurrency", giveAllCurrency);
		obj.addProperty("giveAllResources", giveAllResources);
		obj.addProperty("tradeLockID", tradeLockID);
		if (saveColors != null)
			obj.add("saveColors", saveColors);
		return obj;
	}

}
