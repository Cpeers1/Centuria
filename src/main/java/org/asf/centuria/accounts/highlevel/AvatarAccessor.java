package org.asf.centuria.accounts.highlevel;

import org.asf.centuria.accounts.PlayerInventory;

public abstract class AvatarAccessor extends AbstractInventoryAccessor {

	public AvatarAccessor(PlayerInventory inventory) {
		super(inventory);
	}

	/**
	 * Retrieves all avatar species types
	 * 
	 * @return Array of species types
	 */
	public abstract String[] getAllAvatarSpeciesTypes();

	/**
	 * Retrieves all default avatar body parts of a avatar species
	 * 
	 * @param type Avatar type (either name or defID)
	 * @return Array of body part IDs
	 */
	public abstract String[] getDefaultBodyPartTypes(String type);

	/**
	 * Gives the player another look slot
	 */
	public abstract void addExtraLookSlot();

	/**
	 * Unlocks a avatar species
	 * 
	 * @param type Avatar type (either name or defID)
	 */
	public abstract void unlockAvatarSpecies(String type);

	/**
	 * Checks if a avatar species is unlocked
	 * 
	 * @param type Avatar type (either name or defID)
	 * @return True if unlocked, false otherwise
	 */
	public abstract boolean isAvatarSpeciesUnlocked(String type);

	/**
	 * Checks if a avatar part is unlocked
	 * 
	 * @param defID Item ID
	 */
	public abstract boolean isAvatarPartUnlocked(String defID);

	/**
	 * Unlocks a body mod or wings
	 * 
	 * @param defID Item ID
	 */
	public abstract void unlockAvatarPart(String defID);

	/**
	 * Removes a body mod or wings
	 * 
	 * @param defID Item ID
	 */
	public abstract void lockAvatarPart(String defID);
}
