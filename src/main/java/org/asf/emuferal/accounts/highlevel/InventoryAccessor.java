package org.asf.emuferal.accounts.highlevel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import org.asf.emuferal.accounts.PlayerInventory;
import org.asf.emuferal.accounts.highlevel.itemdata.ItemComponent;

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
	 * Removes inventory objects by ID
	 * 
	 * @param inventoryId Inventory ID
	 * @param objectId    Object UUID to delete
	 * @return JsonObject instance or null if deletion failed
	 */
	public JsonObject removeInventoryObject(String inventoryId, String objectId) {
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

				// Remove item
				items.remove(itm);

				// Remove from cache
				removeItemFromCache(objectId);

				// Add changed file
				if (!itemsToSave.contains(inventoryId))
					itemsToSave.add(inventoryId);

				// Return old item
				return itm;
			}
		}

		// Could not find it
		return null;
	}

	/**
	 * Removes inventory objects by ID
	 * 
	 * @param inventoryId Inventory ID
	 * @param defId       Object DefID to delete
	 * @return JsonObject instance or null if deletion failed
	 */
	public JsonObject removeInventoryObject(String inventoryId, int defId) {
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

				// Remove item
				items.remove(itm);

				// Remove from cache
				removeItemFromCache(itm.get("id").getAsString());

				// Add changed file
				if (!itemsToSave.contains(inventoryId))
					itemsToSave.add(inventoryId);

				// Return old item
				return itm;
			}
		}

		// Could not find it
		return null;
	}

	/**
	 * Creates a new inventory object and saves it (<b>warning:</b> this is very
	 * low-level!)
	 * 
	 * @param inventoryId   Inventory ID
	 * @param itemType      Inventory type value
	 * @param defId         Object defID
	 * @param componentData Object components
	 * @return New item ID
	 */
	public String createInventoryObject(String inventoryId, int itemType, int defId, ItemComponent... componentData) {
		// Load the inventory object
		if (!inventory.containsItem(inventoryId))
			inventory.setItem(inventoryId, new JsonArray());
		JsonArray items = inventory.getItem(inventoryId).getAsJsonArray();

		// Generate item ID
		String iID = UUID.randomUUID().toString();
		while (inventory.getAccessor().hasInventoryObject(inventoryId, iID)) {
			iID = UUID.randomUUID().toString();
		}

		// Build component data
		JsonObject components = new JsonObject();

		// Add components
		for (ItemComponent component : componentData) {
			components.add(component.componentName, component.componentData);
		}

		// Add timestamp
		JsonObject ts = new JsonObject();
		ts.addProperty("ts", System.currentTimeMillis());
		components.add("Timestamp", ts);

		// Build object
		JsonObject obj = new JsonObject();
		obj.addProperty("defId", defId);
		obj.add("components", components);
		obj.addProperty("id", iID);
		obj.addProperty("type", itemType);

		// Save to inventory
		items.add(obj);

		// Add changed file
		if (!itemsToSave.contains(inventoryId))
			itemsToSave.add(inventoryId);

		// Add to cache
		cacheItem(iID, inventoryId);

		// Return item ID
		return iID;
	}

	/**
	 * Creates a new inventory object and saves it (<b>warning:</b> this is very
	 * low-level!)
	 * 
	 * @param inventoryId   Inventory ID
	 * @param defId         Object defID
	 * @param componentData Object components
	 * @return New item ID
	 */
	public String createInventoryObject(String inventoryId, int defId, ItemComponent... componentData) {
		return createInventoryObject(inventoryId, Integer.parseInt(inventoryId), defId, componentData);
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
	 * @param inventoryId Inventory ID
	 */
	public void cacheItem(String itemUUID, String inventoryId) {
		typeCache.put(itemUUID, inventoryId);
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