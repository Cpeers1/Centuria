package org.asf.centuria.accounts.highlevel;

import org.asf.centuria.accounts.PlayerInventory;
import org.asf.centuria.entities.inspiration.InspirationCombineResult;
import org.asf.centuria.entities.players.Player;

import com.google.gson.JsonObject;

public abstract class InspirationAccessor extends AbstractInventoryAccessor {

	public InspirationAccessor(PlayerInventory inventory) {
		super(inventory);
	}

	/**
	 * Checks if the player has a specific inspiration
	 * 
	 * @param defID Inspiration defID
	 * @return True if the player has the furniture item, false otherwise
	 */
	public abstract boolean hasInspiration(String defID);

	/**
	 * Removes an inspiration item
	 * 
	 * @param id Inspiration item ID
	 */
	public abstract void removeInspiration(String id);

	/**
	 * Retrieves a inspiration inventory object
	 * 
	 * @param id Inspiration item ID
	 * @return JsonObject or null
	 */
	public abstract JsonObject getInspirationData(String id);

	/**
	 * Adds a inspiration of a specific defID
	 * 
	 * @param defID Inspiration item defID
	 * @return Item UUID
	 */
	public abstract String addInspiration(String defID);

	/**
	 * Adds all default inspirations to the inventory, if they don't already exist.
	 */
	public abstract void giveDefaultInspirations();

	/**
	 * Attempts to combine inspirations in the player's inventory.
	 */
	public abstract InspirationCombineResult combineInspirations(String[] inspirations, Player player);

	/**
	 * Retrieves the result item ID of a enigma item
	 * 
	 * @param enigma Enigma defID
	 * @return Result item ID or -1
	 */
	public abstract String getEnigmaResult(String enigma);
}
