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

public class DyeAccessor {
	private PlayerInventory inventory;

	public DyeAccessor(PlayerInventory inventory) {
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
	 * Adds a dye to the player's inventory
	 * 
	 * @param defID Dye defID
	 * @return Object UUID
	 */
	public String addDye(int defID) {
		// Find dye
		JsonObject dye = getDyeData(defID);

		// Add one to the quantity field
		int q = dye.get("components").getAsJsonObject().get("Quantity").getAsJsonObject().get("quantity").getAsInt();
		dye.get("components").getAsJsonObject().get("Quantity").getAsJsonObject().remove("quantity");
		dye.get("components").getAsJsonObject().get("Quantity").getAsJsonObject().addProperty("quantity", q + 1);

		// Mark what files to save
		if (!inventory.getAccessor().itemsToSave.contains("111"))
			inventory.getAccessor().itemsToSave.add("111");

		// Return ID
		return dye.get("id").getAsString();
	}

	/**
	 * Removes a dye to the player's inventory
	 * 
	 * @param defID Dye defID
	 */
	public void removeDye(int defID) {
		// Find dye
		JsonObject dye = getDyeData(defID);

		// Remove one to the quantity field
		int q = dye.get("components").getAsJsonObject().get("Quantity").getAsJsonObject().get("quantity").getAsInt();
		dye.get("components").getAsJsonObject().get("Quantity").getAsJsonObject().remove("quantity");
		dye.get("components").getAsJsonObject().get("Quantity").getAsJsonObject().addProperty("quantity", q - 1);

		if (q - 1 <= 0) {
			// Remove object
			inventory.getItem("111").getAsJsonArray().remove(dye);
		}

		// Mark what files to save
		if (!inventory.getAccessor().itemsToSave.contains("111"))
			inventory.getAccessor().itemsToSave.add("111");
	}

	/**
	 * Retrieves a dye inventory object
	 * 
	 * @param id Dye item ID
	 * @return JsonObject or null
	 */
	public JsonObject getDyeData(String id) {
		// Load the inventory object
		if (!inventory.containsItem("111"))
			inventory.setItem("111", new JsonArray());
		JsonArray items = inventory.getItem("111").getAsJsonArray();

		// Find object
		for (JsonElement ele : items) {
			JsonObject itm = ele.getAsJsonObject();
			String itID = itm.get("id").getAsString();
			if (itID.equals(id)) {
				// Return dye
				return itm;
			}
		}

		return null;
	}

	/**
	 * Retrieves the HSV value of a dye
	 * 
	 * @param defID Dye defID
	 * @return HSV value or null
	 */
	public String getDyeHSV(int defID) {
		try {
			// Load helper
			InputStream strm = InventoryItemDownloadPacket.class.getClassLoader()
					.getResourceAsStream("defaultitems/dyehelper.json");
			JsonObject helper = JsonParser.parseString(new String(strm.readAllBytes(), "UTF-8")).getAsJsonObject()
					.get("Dyes").getAsJsonObject();
			strm.close();

			if (helper.has(Integer.toString(defID)))
				return helper.get(Integer.toString(defID)).getAsString();
		} catch (IOException e) {
		}

		return null;
	}

	/**
	 * Removes a dye to the player's inventory
	 * 
	 * @param id Dye item ID
	 */
	public void removeDye(String id) {
		// Load the inventory object
		if (!inventory.containsItem("111"))
			inventory.setItem("111", new JsonArray());
		JsonArray items = inventory.getItem("111").getAsJsonArray();

		// Find object
		for (JsonElement ele : items) {
			JsonObject dye = ele.getAsJsonObject();
			String itID = dye.get("id").getAsString();
			if (itID.equals(id)) {
				// Remove one to the quantity field
				int q = dye.get("components").getAsJsonObject().get("Quantity").getAsJsonObject().get("quantity")
						.getAsInt();
				dye.get("components").getAsJsonObject().get("Quantity").getAsJsonObject().remove("quantity");
				dye.get("components").getAsJsonObject().get("Quantity").getAsJsonObject().addProperty("quantity",
						q - 1);

				if (q - 1 <= 0) {
					// Remove object
					inventory.getItem("111").getAsJsonArray().remove(dye);
				}

				// Mark what files to save
				if (!inventory.getAccessor().itemsToSave.contains("111"))
					inventory.getAccessor().itemsToSave.add("111");

				// End loop
				break;
			}
		}
	}

	/**
	 * Checks if the player has a specific dye
	 * 
	 * @param defID Dye defID
	 * @return True if the player has the dye, false otherwise
	 */
	public boolean hasDye(int defID) {
		// Load the inventory object
		if (!inventory.containsItem("111"))
			inventory.setItem("111", new JsonArray());
		JsonArray items = inventory.getItem("111").getAsJsonArray();

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

	// Retrieves information objects for dyes and makes it if not present
	private JsonObject getDyeData(int defID) {
		// Load the inventory object
		if (!inventory.containsItem("111"))
			inventory.setItem("111", new JsonArray());
		JsonArray items = inventory.getItem("111").getAsJsonArray();

		// Find object
		for (JsonElement ele : items) {
			JsonObject itm = ele.getAsJsonObject();
			int itID = itm.get("defId").getAsInt();
			if (itID == defID) {
				return ele.getAsJsonObject();
			}
		}

		// Add the item
		JsonObject itm = new JsonObject();
		// Timestamp
		JsonObject ts = new JsonObject();
		ts.addProperty("ts", System.currentTimeMillis());
		// Trade thingy
		JsonObject tr = new JsonObject();
		tr.addProperty("isInTradeList", false);
		// Quantity
		JsonObject qt = new JsonObject();
		qt.addProperty("quantity", 0);
		// Build components
		JsonObject components = new JsonObject();
		components.add("Tradable", tr);
		components.add("Quantity", qt);
		components.add("Timestamp", ts);
		itm.addProperty("defId", defID);
		itm.add("components", components);
		itm.addProperty("id", UUID.nameUUIDFromBytes(Integer.toString(defID).getBytes()).toString());
		itm.addProperty("type", 111);

		// Add it
		items.add(itm);

		// Mark what files to save
		if (!inventory.getAccessor().itemsToSave.contains("111"))
			inventory.getAccessor().itemsToSave.add("111");

		return itm;
	}

}
