package org.asf.emuferal.accounts;

import com.google.gson.JsonElement;

public abstract class PlayerInventory {

	/**
	 * Retrieves a item from the player's inventory
	 * 
	 * @param itemID Inventory item ID
	 * @return JsonElement instance or null
	 */
	public abstract JsonElement getItem(String itemID);

	/**
	 * Saves a item to the player inventory
	 * 
	 * @param itemID   Inventory item ID
	 * @param itemData Item data
	 */
	public abstract void setItem(String itemID, JsonElement itemData);

	/**
	 * Deletes a item from the player inventory
	 * 
	 * @param itemID Inventory item ID
	 */
	public abstract void deleteItem(String itemID);

	/**
	 * Checks if a inventory item is present
	 * 
	 * @param itemID Inventory item ID
	 * @return True if the item is present, false otherwise
	 */
	public abstract boolean containsItem(String itemID);

}
