package org.asf.centuria.accounts.highlevel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import org.asf.centuria.accounts.PlayerInventory;
import org.asf.centuria.accounts.highlevel.itemdata.item.ItemComponent;
import org.asf.centuria.entities.players.Player;
import org.asf.centuria.packets.xt.gameserver.inventory.InventoryItemPacket;
import org.asf.centuria.packets.xt.gameserver.inventory.InventoryItemRemovedPacket;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class InventoryAccessor {
	private PlayerInventory inventory;

	private HashMap<String, String> typeCache = new HashMap<String, String>();

	private ArrayList<String> inventoriesToSave = new ArrayList<String>();
	private HashMap<String, ArrayList<String>> updatedItems = new HashMap<String, ArrayList<String>>();
	private HashMap<String, ArrayList<String>> removedItems = new HashMap<String, ArrayList<String>>();

	public class ItemUpdateInfo {
		public JsonArray itemUpdates;
		public String[] itemRemovals;
	}

	public InventoryAccessor(PlayerInventory inventory) {
		this.inventory = inventory;
	}

	/**
	 * Marks a specific item as changed
	 * 
	 * @param inventory Inventory ID
	 * @param itemID    Item ID to mark as changed
	 */
	public void markChanged(String inventory, String itemID) {
		synchronized (updatedItems) {
			// Check if present
			if (!updatedItems.containsKey(inventory)) {
				// Create
				synchronized (inventoriesToSave) {
					// Add to list
					if (!inventoriesToSave.contains(inventory))
						inventoriesToSave.add(inventory);
				}
				updatedItems.put(inventory, new ArrayList<String>());
				removedItems.put(inventory, new ArrayList<String>());
			}

			// Get list
			ArrayList<String> itms = updatedItems.get(inventory);
			ArrayList<String> itmsRemove = removedItems.get(inventory);

			// Add if needed
			if (!itms.contains(itemID))
				itms.add(itemID);
			if (itmsRemove.contains(itemID))
				itmsRemove.remove(itemID);
		}
	}

	/**
	 * Marks a specific item as changed
	 * 
	 * @param inventory Inventory ID
	 * @param itemID    Item ID to mark as changed
	 */
	public void markDeleted(String inventory, String itemID) {
		synchronized (updatedItems) {
			// Check if present
			if (!updatedItems.containsKey(inventory)) {
				// Create
				synchronized (inventoriesToSave) {
					// Add to list
					if (!inventoriesToSave.contains(inventory))
						inventoriesToSave.add(inventory);
				}
				updatedItems.put(inventory, new ArrayList<String>());
				updatedItems.put(inventory, new ArrayList<String>());
			}

			// Get list
			ArrayList<String> itmsRemove = removedItems.get(inventory);
			ArrayList<String> itms = updatedItems.get(inventory);

			// Add if needed
			if (itms.contains(itemID))
				itms.remove(itemID);
			if (!itmsRemove.contains(itemID))
				itmsRemove.add(itemID);
		}
	}

	/**
	 * Call this to mark items as saved, which would remove them from the item
	 * update list
	 * 
	 * @param inventory     Inventory ID
	 * @param items         Item IDs to mark as saved
	 * @param saveInventory Controls if the inventory should be saved using the
	 *                      player inventory interface
	 */
	public void saveItems(String inventory, String[] items, boolean saveInventory) {
		synchronized (updatedItems) {
			// Check if present
			if (!updatedItems.containsKey(inventory))
				return;

			// Get list
			ArrayList<String> itms = updatedItems.get(inventory);
			ArrayList<String> itmsRemove = removedItems.get(inventory);
			for (String id : items) {
				if (itms.contains(id)) {
					// Remove
					itms.remove(id);
				}
				if (itmsRemove.contains(id)) {
					// Remove
					itmsRemove.remove(id);
				}
			}

			// Check result
			if (itms.size() == 0 && itmsRemove.size() == 0) {
				// Remove inventory, its been saved
				synchronized (inventoriesToSave) {
					inventoriesToSave.remove(inventory);
					updatedItems.remove(inventory);
					removedItems.remove(inventory);
				}
			}
		}

		// Save if needed
		if (saveInventory)
			this.inventory.setItem(inventory, this.inventory.getItem(inventory));
	}

	/**
	 * Retrieves the list of changed items
	 * 
	 * @param inventory Inventory ID
	 * @param write     True to remove the changed items from the list, false to
	 *                  only retrieve items
	 * @return ItemUpdateInfo instance containing all changed item instances
	 */
	public ItemUpdateInfo saveUpdatedItems(String inventory, boolean write) {
		ItemUpdateInfo result = new ItemUpdateInfo();
		result.itemUpdates = new JsonArray();
		result.itemRemovals = new String[0];
		ArrayList<String> removals = new ArrayList<String>();
		synchronized (updatedItems) {
			// Check if present
			if (!updatedItems.containsKey(inventory))
				return result;

			// Get list
			ArrayList<String> itms = updatedItems.get(inventory);
			ArrayList<String> itmsRemove = removedItems.get(inventory);
			JsonArray items = this.inventory.getItem(inventory).getAsJsonArray();

			// Find items
			for (JsonElement ele : items) {
				JsonObject itm = ele.getAsJsonObject();
				if (!itm.has("id"))
					continue;
				String itID = itm.get("id").getAsString();
				if (itms.contains(itID)) {
					result.itemUpdates.add(itm);
				}
			}

			// Find removed
			removals.addAll(itmsRemove);

			// Remove if needed
			if (write) {
				// Clear
				itms.clear();
				itmsRemove.clear();

				// Remove inventory, its been saved
				synchronized (inventoriesToSave) {
					inventoriesToSave.remove(inventory);
					updatedItems.remove(inventory);
					removedItems.remove(inventory);
				}
			}
		}

		if (write) {
			// Save inventory
			this.inventory.setItem(inventory, this.inventory.getItem(inventory));
		}

		// Return
		result.itemRemovals = removals.toArray(t -> new String[t]);
		return result;
	}

	/**
	 * Goes through the inventor
	 * 
	 * @param player    Player instance
	 * @param inventory Inventory ID
	 * @return Array of updated elements
	 */
	public JsonArray transferUpdatedItemsToPlayer(Player player, String inventory) {
		JsonArray result = new JsonArray();
		ArrayList<String> removals = new ArrayList<String>();
		synchronized (updatedItems) {
			// Check if present
			if (!updatedItems.containsKey(inventory))
				return result;

			// Get list
			ArrayList<String> itms = updatedItems.get(inventory);
			ArrayList<String> itmsRemove = removedItems.get(inventory);
			JsonArray items = this.inventory.getItem(inventory).getAsJsonArray();

			// Find items
			for (JsonElement ele : items) {
				JsonObject itm = ele.getAsJsonObject();
				if (!itm.has("id"))
					continue;
				String itID = itm.get("id").getAsString();
				if (itms.contains(itID)) {
					result.add(itm);
				}
			}

			// Go through removed
			removals.addAll(itmsRemove);

			// Clear
			itms.clear();
			itmsRemove.clear();

			// Remove inventory, its been saved
			synchronized (inventoriesToSave) {
				inventoriesToSave.remove(inventory);
				updatedItems.remove(inventory);
				removedItems.remove(inventory);
			}
		}

		// Send to player
		if (player != null) {
			InventoryItemPacket update = new InventoryItemPacket();
			update.item = result;
			player.client.sendPacket(update);

			// Send removals if needed
			if (!removals.isEmpty()) {
				InventoryItemRemovedPacket rem = new InventoryItemRemovedPacket();
				rem.items = removals.toArray(t -> new String[t]);
				player.client.sendPacket(rem);
			}
		}

		// Save inventory
		this.inventory.setItem(inventory, this.inventory.getItem(inventory));

		// Return
		return result;
	}

	/**
	 * Checks if the given inventory has changes
	 * 
	 * @param inventory Inventory ID
	 * @return True if changes are present that need saving, false otherwise
	 */
	public boolean hasInventoryChanged(String inventory) {
		synchronized (inventoriesToSave) {
			return inventoriesToSave.contains(inventory);
		}
	}

	/**
	 * Retrieves a list of changed items in the given inventory
	 * 
	 * @param inventory Inventory ID
	 * @return Array of changed item IDs
	 */
	public String[] getChangedItemIds(String inventory) {
		synchronized (updatedItems) {
			if (!updatedItems.containsKey(inventory))
				return new String[0];
			return updatedItems.get(inventory).toArray(t -> new String[t]);
		}
	}

	/**
	 * Retrieves which inventories have unsaved changes
	 * 
	 * @return Array of inventory IDs to save
	 */
	public String[] getChangedInventories() {
		synchronized (inventoriesToSave) {
			return inventoriesToSave.toArray(t -> new String[t]);
		}
	}

	/**
	 * Checks if inventory objects are present
	 * 
	 * @param inventoryId Inventory ID
	 * @param objectId    Object UUID
	 * @return True if present, false otherwise
	 */
	public boolean hasInventoryObjectByItemId(String inventoryId, String objectId) {
		// Load the inventory object
		if (!inventory.containsItem(inventoryId))
			inventory.setItem(inventoryId, new JsonArray());
		JsonArray items = inventory.getItem(inventoryId).getAsJsonArray();

		// Find object
		for (JsonElement ele : items) {
			JsonObject itm = ele.getAsJsonObject();
			if (!itm.has("id"))
				continue;
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
	public JsonObject findInventoryObjectByItemId(String inventoryId, String objectId) {
		// Load the inventory object
		if (!inventory.containsItem(inventoryId))
			inventory.setItem(inventoryId, new JsonArray());
		JsonArray items = inventory.getItem(inventoryId).getAsJsonArray();

		// Find object
		for (JsonElement ele : items) {
			JsonObject itm = ele.getAsJsonObject();
			if (!itm.has("id"))
				continue;
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
	public boolean hasInventoryObjectByDefId(String inventoryId, String defId) {
		// Load the inventory object
		if (!inventory.containsItem(inventoryId))
			inventory.setItem(inventoryId, new JsonArray());
		JsonArray items = inventory.getItem(inventoryId).getAsJsonArray();

		// Find object
		for (JsonElement ele : items) {
			JsonObject itm = ele.getAsJsonObject();
			if (!itm.has("defId"))
				continue;
			String itID = itm.get("defId").getAsString();
			if (itID.equals(defId)) {
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
	public JsonObject findInventoryObjectByDefId(String inventoryId, String defId) {
		// Load the inventory object
		if (!inventory.containsItem(inventoryId))
			inventory.setItem(inventoryId, new JsonArray());
		JsonArray items = inventory.getItem(inventoryId).getAsJsonArray();

		// Find object
		for (JsonElement ele : items) {
			JsonObject itm = ele.getAsJsonObject();
			if (!itm.has("defId"))
				continue;
			String itID = itm.get("defId").getAsString();
			if (itID.equals(defId)) {
				// Found it
				return itm;
			}
		}

		// Could not find it
		return null;
	}

	/**
	 * Retrieves inventory objects by ID
	 * 
	 * @param inventoryId Inventory ID
	 * @param defId       Object DefID
	 * @return Item ID string
	 */
	public String findInventoryObjectItemIdByDefId(String inventoryId, String defId) {
		// Load the inventory object
		if (!inventory.containsItem(inventoryId))
			inventory.setItem(inventoryId, new JsonArray());
		JsonArray items = inventory.getItem(inventoryId).getAsJsonArray();

		// Find object
		for (JsonElement ele : items) {
			JsonObject itm = ele.getAsJsonObject();
			if (!itm.has("defId"))
				continue;
			String itID = itm.get("defId").getAsString();
			if (itID.equals(defId)) {
				// Found it
				return itm.get("id").getAsString();
			}
		}

		// Could not find it
		return null;
	}

	/**
	 * Retrieves inventory objects by ID
	 * 
	 * @param inventoryId Inventory ID
	 * @param id          Object UUID
	 * @return Item ID string
	 */
	public String findInventoryObjectDefIdByItemId(String inventoryId, String id) {
		// Load the inventory object
		if (!inventory.containsItem(inventoryId))
			inventory.setItem(inventoryId, new JsonArray());
		JsonArray items = inventory.getItem(inventoryId).getAsJsonArray();

		// Find object
		for (JsonElement ele : items) {
			JsonObject itm = ele.getAsJsonObject();
			if (!itm.has("id"))
				continue;
			String itID = itm.get("id").getAsString();
			if (itID.equals(id)) {
				// Found it
				return itm.get("defId").getAsString();
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
	public JsonObject removeInventoryObjectByItemId(String inventoryId, String objectId) {
		// Load the inventory object
		if (!inventory.containsItem(inventoryId))
			inventory.setItem(inventoryId, new JsonArray());
		JsonArray items = inventory.getItem(inventoryId).getAsJsonArray();

		// Find object
		for (JsonElement ele : items) {
			JsonObject itm = ele.getAsJsonObject();
			if (!itm.has("id"))
				continue;
			String itID = itm.get("id").getAsString();
			if (itID.equals(objectId)) {
				// Found it

				// Remove item
				items.remove(itm);

				// Remove from cache
				removeItemFromCache(objectId);

				// Add changed file
				markDeleted(inventoryId, itID);

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
	public JsonObject removeInventoryObjectByDefId(String inventoryId, String defId) {
		// Load the inventory object
		if (!inventory.containsItem(inventoryId))
			inventory.setItem(inventoryId, new JsonArray());
		JsonArray items = inventory.getItem(inventoryId).getAsJsonArray();

		// Find object
		for (JsonElement ele : items) {
			JsonObject itm = ele.getAsJsonObject();
			if (!itm.has("defId"))
				continue;
			String itID = itm.get("defId").getAsString();
			if (itID.equalsIgnoreCase(defId)) {
				// Found it

				// Remove item
				items.remove(itm);

				// Remove from cache
				removeItemFromCache(itm.get("id").getAsString());

				// Add changed file
				markDeleted(inventoryId, itm.get("id").getAsString());

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
	public String createInventoryObject(String inventoryId, int itemType, String defId,
			ItemComponent... componentData) {
		// Load the inventory object
		if (!inventory.containsItem(inventoryId))
			inventory.setItem(inventoryId, new JsonArray());
		JsonArray items = inventory.getItem(inventoryId).getAsJsonArray();

		// Generate item ID
		String iID = UUID.randomUUID().toString();
		while (inventory.getAccessor().hasInventoryObjectByItemId(inventoryId, iID)) {
			iID = UUID.randomUUID().toString();
		}

		// Build component data
		JsonObject components = new JsonObject();

		// Add components
		for (ItemComponent component : componentData) {
			components.add(component.componentName, component.componentData.deepCopy());
		}

		// Add timestamp
		JsonObject ts = new JsonObject();
		ts.addProperty("ts", System.currentTimeMillis());
		components.add("Timestamp", ts);

		// Build object
		JsonObject obj = new JsonObject();
		if (defId != null && !defId.equals("-1") && !defId.isEmpty())
			obj.addProperty("defId", defId);
		obj.add("components", components);
		obj.addProperty("id", iID);
		obj.addProperty("type", itemType);

		// Save to inventory
		items.add(obj);

		// Add changed file
		markChanged(inventoryId, iID);

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
	public String createInventoryObject(String inventoryId, String defId, ItemComponent... componentData) {
		return createInventoryObject(inventoryId, inventoryId.equals("avatars") ? 200 : Integer.parseInt(inventoryId),
				defId, componentData);
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