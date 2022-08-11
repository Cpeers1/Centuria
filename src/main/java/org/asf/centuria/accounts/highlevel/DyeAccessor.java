package org.asf.centuria.accounts.highlevel;

import org.asf.centuria.accounts.PlayerInventory;
import com.google.gson.JsonObject;

public abstract class DyeAccessor extends AbstractInventoryAccessor {
	public DyeAccessor(PlayerInventory inventory) {
		super(inventory);
	}

	/**
	 * Adds a dye to the player's inventory
	 * 
	 * @param defID Dye defID
	 * @return Object UUID
	 */
	public abstract String addDye(int defID);

	/**
	 * Removes a dye to the player's inventory
	 * 
	 * @param defID Dye defID
	 */
	public abstract void removeDye(int defID);

	/**
	 * Retrieves a dye inventory object
	 * 
	 * @param id Dye item ID
	 * @return JsonObject or null
	 */
	public abstract JsonObject getDyeData(String id);

	/**
	 * Retrieves the HSV value of a dye
	 * 
	 * @param defID Dye defID
	 * @return HSV value or null
	 */
	public abstract String getDyeHSV(int defID);

	/**
	 * Removes a dye to the player's inventory
	 * 
	 * @param id Dye item ID
	 */
	public abstract void removeDye(String id);

	/**
	 * Checks if the player has a specific dye
	 * 
	 * @param defID Dye defID
	 * @return True if the player has the dye, false otherwise
	 */
	public abstract boolean hasDye(int defID);

}
