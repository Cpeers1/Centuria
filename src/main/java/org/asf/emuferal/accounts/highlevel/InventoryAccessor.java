package org.asf.emuferal.accounts.highlevel;

import java.util.ArrayList;
import java.util.HashMap;

import org.asf.emuferal.accounts.PlayerInventory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class InventoryAccessor {
	private PlayerInventory inventory;
	private HashMap<String, String> typeCache = new HashMap<String, String>();
	ArrayList<String> itemsToSave = new ArrayList<String>();

	public InventoryAccessor(PlayerInventory inventory) {
		this.inventory = inventory;
	}

	/**
	 * Call this after saving items
	 */
	public void completedSave() {
		itemsToSave.clear();
	}

	/**
	 * Retrieves which items to save
	 * 
	 * @return Array of item IDs to save
	 */
	public String[] getItemsToSave() {
		return itemsToSave.toArray(t -> new String[t]);
	}

	/**
	 * Checks if inventory objects are present
	 * 
	 * @param inventoryId Inventory ID
	 * @param objectId    Object UUID
	 * @return True if present, false otherwise
	 */
	public boolean hasInventoryObject(String inventoryId, String objectId) {
		// Load the inventory object
		if (!inventory.containsItem(inventoryId))
			inventory.setItem(inventoryId, new JsonArray());
		JsonArray items = inventory.getItem(inventoryId).getAsJsonArray();

		// Find object
		for (JsonElement ele : items) {
			JsonObject itm = ele.getAsJsonObject();
			String itID = itm.get("id").getAsString();
			if (itID.equals(objectId)) {
				// Found it
				return true;
			}
		}

		// Could not find it
		return false;
	}

	/**
	 * Retrieves inventory objects by ID
	 * 
	 * @param inventoryId Inventory ID
	 * @param objectId    Object UUID
	 * @return JsonObject instance or null
	 */
	public JsonObject findInventoryObject(String inventoryId, String objectId) {
		// Load the inventory object
		if (!inventory.containsItem(inventoryId))
			inventory.setItem(inventoryId, new JsonArray());
		JsonArray items = inventory.getItem(inventoryId).getAsJsonArray();

		// Find object
		for (JsonElement ele : items) {
			JsonObject itm = ele.getAsJsonObject();
			String itID = itm.get("id").getAsString();
			if (itID.equals(objectId)) {
				// Found it
				return itm;
			}
		}

		// Could not find it
		return null;
	}

	/**
	 * Checks if inventory objects are present
	 * 
	 * @param inventoryId Inventory ID
	 * @param defId       Object DefID
	 * @return True if present, false otherwise
	 */
	public boolean hasInventoryObject(String inventoryId, int defId) {
		// Load the inventory object
		if (!inventory.containsItem(inventoryId))
			inventory.setItem(inventoryId, new JsonArray());
		JsonArray items = inventory.getItem(inventoryId).getAsJsonArray();

		// Find object
		for (JsonElement ele : items) {
			JsonObject itm = ele.getAsJsonObject();
			int itID = itm.get("defId").getAsInt();
			if (itID == defId) {
				// Found it
				return true;
			}
		}

		// Could not find it
		return false;
	}

	/**
	 * Retrieves inventory objects by ID
	 * 
	 * @param inventoryId Inventory ID
	 * @param defId       Object DefID
	 * @return JsonObject instance or null
	 */
	public JsonObject findInventoryObject(String inventoryId, int defId) {
		// Load the inventory object
		if (!inventory.containsItem(inventoryId))
			inventory.setItem(inventoryId, new JsonArray());
		JsonArray items = inventory.getItem(inventoryId).getAsJsonArray();

		// Find object
		for (JsonElement ele : items) {
			JsonObject itm = ele.getAsJsonObject();
			int itID = itm.get("defId").getAsInt();
			if (itID == defId) {
				// Found it
				return itm;
			}
		}

		// Could not find it
		return null;
	}

	/**
	 * Retrieves the inventory ID of a specific inventory object (<b>only works if
	 * the inventory had been loaded</b>, inventories are loaded if getItem() is run
	 * in the inventory object)
	 * 
	 * @param itemUUID Object UUID
	 * @return Inventory ID or null
	 */
	public String getInventoryIDOfItem(String itemUUID) {
		return typeCache.get(itemUUID);
	}

	/**
	 * Adds a item ID and its inventory ID to the cache
	 * 
	 * @param itemUUID    Object UUID
	 * @param inventoryID Inventory ID
	 */
	public void cacheItem(String itemUUID, String inventoryID) {
		typeCache.put(itemUUID, inventoryID);
	}

	/**
	 * Removes a item ID from the cache
	 * 
	 * @param itemUUID Object UUID
	 */
	public void removeItemFromCache(String itemUUID) {
		typeCache.remove(itemUUID);
	}
}