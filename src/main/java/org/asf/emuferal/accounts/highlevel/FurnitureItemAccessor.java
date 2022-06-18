package org.asf.emuferal.accounts.highlevel;

import org.asf.emuferal.accounts.PlayerInventory;
import com.google.gson.JsonObject;

public abstract class FurnitureItemAccessor extends AbstractInventoryAccessor {
	public FurnitureItemAccessor(PlayerInventory inventory) {
		super(inventory);
	}

	/**
	 * Checks if the player has a specific furniture item
	 * 
	 * @param defID Furniture defID
	 * @return True if the player has the furniture item, false otherwise
	 */
	public abstract boolean hasFurniture(int defID);

	/**
	 * Retrieves the amount of a specific furniture item the player has
	 * 
	 * @param defID Furniture defID
	 * @return Amount of the specific item
	 */
	public abstract int getFurnitureCount(int defID);

	/**
	 * Removes a furniture item
	 * 
	 * @param id Furniture item ID
	 */
	public abstract void removeFurniture(String id);

	/**
	 * Retrieves a furniture inventory object
	 * 
	 * @param id Furniture item ID
	 * @return JsonObject or null
	 */
	public abstract JsonObject getFurnitureData(String id);

	/**
	 * Adds a furniture item of a specific defID
	 * 
	 * @param defID         Furniture item defID
	 * @param isInTradeList True to add this item to trade list, false otherwise
	 * @return Item UUID
	 */
	public abstract String addFurniture(int defID, boolean isInTradeList);

	/**
	 * Retrieves the default color of a furniture color channel
	 * 
	 * @param defID   Furniture defID
	 * @param channel Channel number
	 * @return HSV string or null
	 */
	public abstract String getDefaultFurnitureChannelHSV(int defID, int channel);

	/**
	 * Retrieves the default color of a furniture color channel
	 * 
	 * @param placeableUUID   Placeable UUID for the furniture.
	 * @return The DefId for the object.
	 */
	public abstract int getDefIDFromUUID(String placeableUUID);
}
