package org.asf.emuferal.accounts.highlevel.itemdata.inventory;

import java.util.ArrayList;

import org.asf.emuferal.accounts.PlayerInventory;

import com.google.gson.JsonObject;

public abstract class AbstractInventoryInteractionHelper {

	/**
	 * Adds a single item to the inventory
	 * 
	 * @param inventory Inventory to add the item to
	 * @param defID     Item defID
	 * @return New item JSON or null
	 */
	public abstract JsonObject addOne(PlayerInventory inventory, int defID);

	/**
	 * Adds a single item to the inventory
	 * 
	 * @param inventory Inventory to add the item to
	 * @param object    Object to add
	 * @return New item JSON or null
	 */
	public abstract JsonObject addOne(PlayerInventory inventory, JsonObject object);

	/**
	 * Adds a amount of a item to the inventory
	 * 
	 * @param inventory Inventory to add the item to
	 * @param defID     Item defID
	 * @param count     Item count
	 * @return New item JSONs
	 */
	public JsonObject[] addMultiple(PlayerInventory inventory, int defID, int count) {
		ArrayList<JsonObject> items = new ArrayList<JsonObject>();
		for (int i = 0; i < count; i++) {
			items.add(addOne(inventory, defID));
		}
		return items.toArray(t -> new JsonObject[t]);
	}

	/**
	 * Removes a single item from the inventory
	 * 
	 * @param inventory Inventory to remove the item from
	 * @param defID     Item defID
	 * @return True if successful, false otherwise
	 */
	public abstract boolean removeOne(PlayerInventory inventory, int defID);

	/**
	 * Removes a single item from the inventory
	 * 
	 * @param inventory Inventory to remove the item from
	 * @param object    Object to remove
	 * @return True if successful, false otherwise
	 */
	public abstract boolean removeOne(PlayerInventory inventory, JsonObject object);

	/**
	 * Removes a amount of a item from the inventory
	 * 
	 * @param inventory Inventory to remove the item from
	 * @param defID     Item defID
	 * @param count     Item count
	 * @return True if successful, false otherwise
	 */
	public boolean removeMultiple(PlayerInventory inventory, int defID, int count) {
		for (int i = 0; i < count; i++) {
			if (!removeOne(inventory, defID))
				return false;
		}
		return true;
	}

}
