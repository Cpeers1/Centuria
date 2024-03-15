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
	public boolean allowGiveItemAvatars = true;
	public boolean allowGiveItemMods = true;
	public boolean allowGiveItemSanctuaryTypes = true;
	public boolean allowGiveItemClothes = true;
	public boolean allowGiveItemFurnitureItems = true;
	public boolean allowGiveItemResources = true;
	public boolean allowGiveItemCurrency = true;
	public JsonObject saveColors = null;
	public JsonObject saveNamePrefixes = null;
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
		this.allowGiveItemAvatars = Centuria.defaultAllowGiveItemAvatars;
		this.allowGiveItemClothes = Centuria.defaultAllowGiveItemClothes;
		this.allowGiveItemMods = Centuria.defaultAllowGiveItemMods;
		this.allowGiveItemFurnitureItems = Centuria.defaultAllowGiveItemFurnitureItems;
		this.allowGiveItemSanctuaryTypes = Centuria.defaultAllowGiveItemSanctuaryTypes;
		this.allowGiveItemCurrency = Centuria.defaultAllowGiveItemCurrency;
		this.allowGiveItemResources = Centuria.defaultAllowGiveItemResources;
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
		if (data.has("allowGiveItemAvatars"))
			this.allowGiveItemAvatars = data.get("allowGiveItemAvatars").getAsBoolean();
		else
			this.allowGiveItemAvatars = giveAllAvatars;
		if (data.has("allowGiveItemlothes"))
			this.allowGiveItemClothes = data.get("allowGiveItemClothes").getAsBoolean();
		else
			this.allowGiveItemClothes = giveAllClothes;
		if (data.has("allowGiveItemMods"))
			this.allowGiveItemMods = data.get("allowGiveItemMods").getAsBoolean();
		else
			this.allowGiveItemMods = giveAllMods;
		if (data.has("allowGiveItemFurnitureItems"))
			this.allowGiveItemFurnitureItems = data.get("allowGiveItemFurnitureItems").getAsBoolean();
		else
			this.allowGiveItemFurnitureItems = giveAllFurnitureItems;
		if (data.has("allowGiveItemSanctuaryTypes"))
			this.allowGiveItemSanctuaryTypes = data.get("allowGiveItemSanctuaryTypes").getAsBoolean();
		else
			this.allowGiveItemSanctuaryTypes = giveAllSanctuaryTypes;
		if (data.has("allowGiveItemCurrency"))
			this.allowGiveItemSanctuaryTypes = data.get("allowGiveItemCurrency").getAsBoolean();
		else
			this.allowGiveItemCurrency = giveAllCurrency;
		if (data.has("allowGiveItemResources"))
			this.allowGiveItemResources = data.get("allowGiveItemResources").getAsBoolean();
		else
			this.allowGiveItemResources = giveAllResources;
		if (data.has("saveColors") && !data.get("saveColors").isJsonNull())
			this.saveColors = data.get("saveColors").getAsJsonObject();
		if (data.has("saveNamePrefixes") && !data.get("saveNamePrefixes").isJsonNull())
			this.saveNamePrefixes = data.get("saveNamePrefixes").getAsJsonObject();
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
		obj.addProperty("allowGiveItemAvatars", allowGiveItemAvatars);
		obj.addProperty("allowGiveItemMods", allowGiveItemMods);
		obj.addProperty("allowGiveItemSanctuaryTypes", allowGiveItemSanctuaryTypes);
		obj.addProperty("allowGiveItemClothes", allowGiveItemClothes);
		obj.addProperty("allowGiveItemFurnitureItems", allowGiveItemFurnitureItems);
		obj.addProperty("allowGiveItemResources", allowGiveItemResources);
		obj.addProperty("allowGiveItemCurrency", allowGiveItemCurrency);
		obj.addProperty("tradeLockID", tradeLockID);
		if (saveColors != null)
			obj.add("saveColors", saveColors);
		if (saveNamePrefixes != null)
			obj.add("saveNamePrefixes", saveNamePrefixes);
		return obj;
	}

}
