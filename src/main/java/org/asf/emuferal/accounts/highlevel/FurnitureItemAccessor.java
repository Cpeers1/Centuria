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

public class FurnitureItemAccessor {
	private PlayerInventory inventory;

	public FurnitureItemAccessor(PlayerInventory inventory) {
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
	 * Checks if the player has a specific furniture item
	 * 
	 * @param defID Furniture defID
	 * @return True if the player has the furniture item, false otherwise
	 */
	public boolean hasFurniture(int defID) {
		// Load the inventory object
		if (!inventory.containsItem("102"))
			inventory.setItem("102", new JsonArray());
		JsonArray items = inventory.getItem("102").getAsJsonArray();

		// Find object
		for (JsonElement ele : items) {
			JsonObject itm = ele.getAsJsonObject();
			int itID = itm.get("defId").getAsInt();
			if (itID == defID) {
				return true;
			}
		}

		// Item was not found
		return false;
	}

	/**
	 * Retrieves the amount of a specific furniture item the player has
	 * 
	 * @param defID Furniture defID
	 * @return Amount of the specific item
	 */
	public int getFurnitureCount(int defID) {
		int count = 0;

		// Load the inventory object
		if (!inventory.containsItem("102"))
			inventory.setItem("102", new JsonArray());
		JsonArray items = inventory.getItem("102").getAsJsonArray();

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
	 * Removes a furniture item
	 * 
	 * @param id Furniture item ID
	 */
	public void removeFurniture(String id) {
		// Load the inventory object
		if (!inventory.containsItem("102"))
			inventory.setItem("102", new JsonArray());
		JsonArray items = inventory.getItem("102").getAsJsonArray();

		// Find object
		for (JsonElement ele : items) {
			JsonObject itm = ele.getAsJsonObject();
			String itID = itm.get("id").getAsString();
			if (itID.equals(id)) {
				// Remove item
				items.remove(ele);

				// Mark what files to save
				if (!inventory.getAccessor().itemsToSave.contains("102"))
					inventory.getAccessor().itemsToSave.add("102");

				// End loop
				break;
			}
		}
	}

	/**
	 * Retrieves a furniture inventory object
	 * 
	 * @param id Furniture item ID
	 * @return JsonObject or null
	 */
	public JsonObject getFurnitureData(String id) {
		return inventory.getAccessor().findInventoryObject("102", id);
	}

	/**
	 * Adds a furniture item of a specific defID
	 * 
	 * @param defID         Furniture item defID
	 * @param isInTradeList True to add this item to trade list, false otherwise
	 * @return Item UUID
	 */
	public String addFurniture(int defID, boolean isInTradeList) {
		// Load the inventory object
		if (!inventory.containsItem("102"))
			inventory.setItem("102", new JsonArray());
		JsonArray items = inventory.getItem("102").getAsJsonArray();

		// Generate item ID
		String cID = UUID.randomUUID().toString();
		while (inventory.getAccessor().hasInventoryObject("102", cID)) {
			cID = UUID.randomUUID().toString();
		}

		// Generate object
		try {
			// Load helper
			InputStream strm = InventoryItemDownloadPacket.class.getClassLoader()
					.getResourceAsStream("defaultitems/furniturehelper.json");
			JsonObject helper = JsonParser.parseString(new String(strm.readAllBytes(), "UTF-8")).getAsJsonObject()
					.get("Furniture").getAsJsonObject();
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
				components.add("Placeable", new JsonObject());
				components.add("Timestamp", ts);
				itm.addProperty("defId", defID);
				itm.add("components", components);
				itm.addProperty("id", cID);
				itm.addProperty("type", 102);

				// Add it
				items.add(itm);

				// Mark what files to save
				if (!inventory.getAccessor().itemsToSave.contains("102"))
					inventory.getAccessor().itemsToSave.add("102");
			}
		} catch (IOException e) {
		}

		// Return ID
		return cID;
	}

	/**
	 * Retrieves the default color of a furniture color channel
	 * 
	 * @param defID   Furniture defID
	 * @param channel Channel number
	 * @return HSV string or null
	 */
	public String getDefaultFurnitureChannelHSV(int defID, int channel) {
		try {
			// Load helper
			InputStream strm = InventoryItemDownloadPacket.class.getClassLoader()
					.getResourceAsStream("defaultitems/furniturehelper.json");
			JsonObject helper = JsonParser.parseString(new String(strm.readAllBytes(), "UTF-8")).getAsJsonObject()
					.get("Furniture").getAsJsonObject();
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
