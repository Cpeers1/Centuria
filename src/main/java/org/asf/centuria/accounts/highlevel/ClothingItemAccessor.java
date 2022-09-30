package org.asf.centuria.accounts.highlevel;

import org.asf.centuria.accounts.PlayerInventory;
import com.google.gson.JsonObject;

public abstract class ClothingItemAccessor extends AbstractInventoryAccessor {
	public ClothingItemAccessor(PlayerInventory inventory) {
		super(inventory);
	}

	/**
	 * Checks if the player has a specific clothing item
	 * 
	 * @param defID Clothing defID
	 * @return True if the player has the clothing item, false otherwise
	 */
	public abstract boolean hasClothing(int defID);

	/**
	 * Retrieves the amount of a specific clothing item the player has
	 * 
	 * @param defID Clothing defID
	 * @return Amount of the specific item
	 */
	public abstract int getClothingCount(int defID);

	/**
	 * Removes a clothing item
	 * 
	 * @param id Clothing item ID
	 */
	public abstract void removeClothing(String id);

	/**
	 * Retrieves a clothing inventory object
	 * 
	 * @param id Clothing item ID
	 * @return JsonObject or null
	 */
	public abstract JsonObject getClothingData(String id);

	/**
	 * Adds a clothing item of a specific defID
	 * 
	 * @param defID         Clothing item defID
	 * @param isInTradeList True to add this item to trade list, false otherwise
	 * @return Item UUID
	 */
	public abstract String addClothing(int defID, boolean isInTradeList);

	/**
	 * Retrieves the default color of a clothing color channel
	 * 
	 * @param defID   Clothing defID
	 * @param channel Channel number
	 * @return HSV string or null
	 */
	public abstract JsonObject getDefaultClothingChannelHSV(int defID, int channel);

}
