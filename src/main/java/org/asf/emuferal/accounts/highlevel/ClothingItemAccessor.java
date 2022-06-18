package org.asf.emuferal.accounts.highlevel;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import org.asf.emuferal.accounts.PlayerInventory;
import org.asf.emuferal.packets.xt.gameserver.inventory.InventoryItemDownloadPacket;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ClothingItemAccessor {
	private PlayerInventory inventory;

	public ClothingItemAccessor(PlayerInventory inventory) {
		this.inventory = inventory;
	}

	/**
	 * Call this after saving items
	 */
	public void completedSave() {
		inventory.getAccessor().itemsToSave.clear();
	}

	/**
	 * Retrieves which items to save
	 * 
	 * @return Array of item IDs to save
	 */
	public String[] getItemsToSave() {
		return inventory.getAccessor().getItemsToSave();
	}

	/**
	 * Checks if the player has a specific clothing item
	 * 
	 * @param defID Clothing defID
	 * @return True if the player has the clothing item, false otherwise
	 */
	public boolean hasClothing(int defID) {
		return inventory.getAccessor().hasInventoryObject("100", defID);
	}

	/**
	 * Retrieves the amount of a specific clothing item the player has
	 * 
	 * @param defID Clothing defID
	 * @return Amount of the specific item
	 */
	public int getClothingCount(int defID) {
		int count = 0;

		// Load the inventory object
		if (!inventory.containsItem("100"))
			inventory.setItem("100", new JsonArray());
		JsonArray items = inventory.getItem("100").getAsJsonArray();

		// Find object
		for (JsonElement ele : items) {
			JsonObject itm = ele.getAsJsonObject();
			int itID = itm.get("defId").getAsInt();
			if (itID == defID) {
				count++;
			}
		}

		return count;
	}

	/**
	 * Removes a clothing item
	 * 
	 * @param id Clothing item ID
	 */
	public void removeClothing(String id) {
		// Load the inventory object
		if (!inventory.containsItem("100"))
			inventory.setItem("100", new JsonArray());
		JsonArray items = inventory.getItem("100").getAsJsonArray();

		// Find object
		for (JsonElement ele : items) {
			JsonObject itm = ele.getAsJsonObject();
			String itID = itm.get("id").getAsString();
			if (itID.equals(id)) {
				// Remove item
				items.remove(ele);

				// Mark what files to save
				if (!inventory.getAccessor().itemsToSave.contains("100"))
					inventory.getAccessor().itemsToSave.add("100");

				// End loop
				break;
			}
		}
	}

	/**
	 * Retrieves a clothing inventory object
	 * 
	 * @param id Clothing item ID
	 * @return JsonObject or null
	 */
	public JsonObject getClothingData(String id) {
		return inventory.getAccessor().findInventoryObject("100", id);
	}

	/**
	 * Adds a clothing item of a specific defID
	 * 
	 * @param defID         Clothing item defID
	 * @param isInTradeList True to add this item to trade list, false otherwise
	 * @return Item UUID
	 */
	public String addClothing(int defID, boolean isInTradeList) {
		// Load the inventory object
		if (!inventory.containsItem("100"))
			inventory.setItem("100", new JsonArray());
		JsonArray items = inventory.getItem("100").getAsJsonArray();

		// Generate item ID
		String cID = UUID.randomUUID().toString();
		while (inventory.getAccessor().hasInventoryObject("100", cID)) {
			cID = UUID.randomUUID().toString();
		}

		// Generate object
		try {
			// Load helper
			InputStream strm = InventoryItemDownloadPacket.class.getClassLoader()
					.getResourceAsStream("defaultitems/clothinghelper.json");
			JsonObject helper = JsonParser.parseString(new String(strm.readAllBytes(), "UTF-8")).getAsJsonObject()
					.get("Clothing").getAsJsonObject();
			strm.close();

			// Check existence
			if (helper.has(Integer.toString(defID))) {
				// Create the item
				JsonObject itm = new JsonObject();
				// Timestamp
				JsonObject ts = new JsonObject();
				ts.addProperty("ts", System.currentTimeMillis());
				// Trade thingy
				JsonObject tr = new JsonObject();
				tr.addProperty("isInTradeList", isInTradeList);
				// Build components
				JsonObject components = new JsonObject();
				components.add("Tradable", tr);
				components.add("Colorable", helper.get(Integer.toString(defID)));
				components.add("Timestamp", ts);
				itm.addProperty("defId", defID);
				itm.add("components", components);
				itm.addProperty("id", cID);
				itm.addProperty("type", 100);

				// Add it
				items.add(itm);

				// Mark what files to save
				if (!inventory.getAccessor().itemsToSave.contains("100"))
					inventory.getAccessor().itemsToSave.add("100");
			}
		} catch (IOException e) {
		}

		// Return ID
		return cID;
	}

	/**
	 * Retrieves the default color of a clothing color channel
	 * 
	 * @param defID   Clothing defID
	 * @param channel Channel number
	 * @return HSV string or null
	 */
	public String getDefaultClothingChannelHSV(int defID, int channel) {
		try {
			// Load helper
			InputStream strm = InventoryItemDownloadPacket.class.getClassLoader()
					.getResourceAsStream("defaultitems/clothinghelper.json");
			JsonObject helper = JsonParser.parseString(new String(strm.readAllBytes(), "UTF-8")).getAsJsonObject()
					.get("Clothing").getAsJsonObject();
			strm.close();

			// Check existence
			if (helper.has(Integer.toString(defID))) {
				// Find channel
				JsonObject data = helper.get(Integer.toString(defID)).getAsJsonObject();
				if (data.has("color" + channel + "HSV"))
					return data.get("color" + channel + "HSV").getAsJsonObject().get("_hsv").getAsString();
			}
		} catch (IOException e) {
		}
		return null;
	}

}
