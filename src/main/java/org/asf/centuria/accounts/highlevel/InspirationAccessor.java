package org.asf.centuria.accounts.highlevel;

import org.asf.centuria.accounts.PlayerInventory;
import org.asf.centuria.entities.inspiration.InspirationCombineResult;
import org.asf.centuria.players.Player;

import com.google.gson.JsonObject;

public abstract class InspirationAccessor extends AbstractInventoryAccessor {

	public InspirationAccessor(PlayerInventory inventory) {
		super(inventory);
	}

	/**
	 * Checks if the player has a specific furniture item
	 * 
	 * @param defID Furniture defID
	 * @return True if the player has the furniture item, false otherwise
	 */
	public abstract boolean hasInspiration(int defID);

	/**
	 * Removes a furniture item
	 * 
	 * @param id Furniture item ID
	 */
	public abstract void removeInspiration(String id);

	/**
	 * Retrieves a furniture inventory object
	 * 
	 * @param id Furniture item ID
	 * @return JsonObject or null
	 */
	public abstract JsonObject getInspirationData(String id);

	/**
	 * Adds a furniture item of a specific defID
	 * 
	 * @param defID Furniture item defID
	 * @return Item UUID
	 */
	public abstract String addInspiration(int defID);

	/**
	 * Adds all default inspirations to the inventory, if they don't already exist.
	 */
	public abstract void giveDefaultInspirations();

	/**
	 * Attempts to combine inspirations in the player's inventory.
	 */
	public abstract InspirationCombineResult combineInspirations(int[] inspirations, Player player);

	/**
	 * Retrieves the result item ID of a enigma item
	 * 
	 * @param enigma Enigma defID
	 * @return Result item ID or -1
	 */
	public abstract int getEnigmaResult(int enigma);
}
