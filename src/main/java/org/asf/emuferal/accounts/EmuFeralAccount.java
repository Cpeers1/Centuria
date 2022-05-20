package org.asf.emuferal.accounts;

public abstract class EmuFeralAccount {

	/**
	 * Retrieves the account login name
	 * 
	 * @return Login name string
	 */
	public abstract String getLoginName();

	/**
	 * Retrieves the account display name
	 * 
	 * @return Display name string
	 */
	public abstract String getDisplayName();

	/**
	 * Retrieves the account ID
	 * 
	 * @return Account ID (usually a UUID)
	 */
	public abstract String getAccountID();

	/**
	 * Retrieves the account numeric ID
	 * 
	 * @return Account numeric ID
	 */
	public abstract int getAccountNumericID();

	/**
	 * Checks if the player completed the tutorial
	 * 
	 * @return True if the player hasn't finished the tutorial, false otherwise.
	 */
	public abstract boolean isPlayerNew();

	/**
	 * Used to mark the tutorial as finished
	 */
	public abstract void finishedTutorial();

	/**
	 * Used to update the player display name
	 * 
	 * @param name New display name
	 * @return True if valid, false otherwise
	 */
	public abstract boolean updateDisplayName(String name);

	/**
	 * Retrieves the player inventory
	 * 
	 * @return PlayerInventory instance
	 */
	public abstract PlayerInventory getPlayerInventory();

}
