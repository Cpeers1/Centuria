package org.asf.centuria.accounts.highlevel;

import org.asf.centuria.accounts.PlayerInventory;
import org.asf.centuria.accounts.highlevel.InventoryAccessor.ItemUpdateInfo;
import org.asf.centuria.entities.players.Player;
import com.google.gson.JsonArray;

public abstract class AbstractInventoryAccessor {
	protected PlayerInventory inventory;

	public AbstractInventoryAccessor(PlayerInventory inventory) {
		this.inventory = inventory;
	}

	/**
	 * Marks a specific item as changed
	 * 
	 * @param inventory Inventory ID
	 * @param itemID    Item ID to mark as changed
	 */
	public void markChanged(String inventory, String itemID) {
		this.inventory.getAccessor().markChanged(inventory, itemID);
	}

	/**
	 * Marks a specific item as changed
	 * 
	 * @param inventory Inventory ID
	 * @param itemID    Item ID to mark as changed
	 */
	public void markDeleted(String inventory, String itemID) {
		this.inventory.getAccessor().markDeleted(inventory, itemID);
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
		this.inventory.getAccessor().saveItems(inventory, items, saveInventory);
	}

	/**
	 * Retrieves the list of changed items
	 * 
	 * @param inventory Inventory ID
	 * @param write     True to remove the changed items from the list, false to
	 *                  only retrieve items
	 * @return ItemUpdateInfo containing all changed item instances
	 */
	public ItemUpdateInfo saveUpdatedItems(String inventory, boolean write) {
		return this.inventory.getAccessor().saveUpdatedItems(inventory, write);
	}

	/**
	 * Goes through the inventor
	 * 
	 * @param player    Player instance
	 * @param inventory Inventory ID
	 * @return Array of updated elements
	 */
	public JsonArray transferUpdatedItemsToPlayer(Player player, String inventory) {
		return this.inventory.getAccessor().transferUpdatedItemsToPlayer(player, inventory);
	}

	/**
	 * Checks if the given inventory has changes
	 * 
	 * @param inventory Inventory ID
	 * @return True if changes are present that need saving, false otherwise
	 */
	public boolean hasInventoryChanged(String inventory) {
		return this.inventory.getAccessor().hasInventoryChanged(inventory);
	}

	/**
	 * Retrieves a list of changed items in the given inventory
	 * 
	 * @param inventory Inventory ID
	 * @return Array of changed item IDs
	 */
	public String[] getChangedItemIds(String inventory) {
		return this.inventory.getAccessor().getChangedItemIds(inventory);
	}

	/**
	 * Retrieves which inventories have unsaved changes
	 * 
	 * @return Array of inventory IDs to save
	 */
	public String[] getChangedInventories() {
		return this.inventory.getAccessor().getChangedInventories();
	}
}
