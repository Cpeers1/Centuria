package org.asf.emuferal.accounts.highlevel.itemdata.inventory;

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
	 * @param object    Item json to add
	 * @return New item JSON or null
	 */
	public abstract JsonObject addOne(PlayerInventory inventory, JsonObject object);

	/**
	 * Removes a single item from the inventory
	 * 
	 * @param inventory Inventory to remove the item from
	 * @param defID     Item defID
	 * @return True if successful, false otherwise
	 */
	public abstract boolean removeOne(PlayerInventory inventory, int defID);

}
