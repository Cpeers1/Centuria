package org.asf.emuferal.accounts.highlevel;

import org.asf.emuferal.accounts.PlayerInventory;

public abstract class AbstractInventoryAccessor {
	protected PlayerInventory inventory;

	public AbstractInventoryAccessor(PlayerInventory inventory) {
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
	 * Adds an item to save
	 * 
	 * @param item Item ID to save later
	 */
	protected void addItemToSave(String item) {
		if (!inventory.getAccessor().itemsToSave.contains(item))
			inventory.getAccessor().itemsToSave.add(item);
	}

}
