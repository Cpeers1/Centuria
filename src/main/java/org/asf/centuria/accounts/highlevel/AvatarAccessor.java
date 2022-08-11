package org.asf.centuria.accounts.highlevel;

import org.asf.centuria.accounts.PlayerInventory;

public abstract class AvatarAccessor extends AbstractInventoryAccessor {

	public AvatarAccessor(PlayerInventory inventory) {
		super(inventory);
	}

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
	public abstract boolean isAvatarPartUnlocked(int defID);

	/**
	 * Unlocks a body mod or wings
	 * 
	 * @param defID Item ID
	 */
	public abstract void unlockAvatarPart(int defID);

	/**
	 * Removes a body mod or wings
	 * 
	 * @param defID Item ID
	 */
	public abstract void lockAvatarPart(int defID);
}
