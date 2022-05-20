package org.asf.emuferal.accounts;

import com.google.gson.JsonObject;

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

	/**
	 * Retrieves or creates the account privacy settings
	 * 
	 * @return JsonObject instance
	 */
	public abstract JsonObject getPrivacySettings();

	/**
	 * Saves privacy settings
	 * 
	 * @param settings JsonObject instance containing privacy settings
	 */
	public abstract void savePrivacySettings(JsonObject settings);

	/**
	 * Retrieves the active character look
	 * 
	 * @return Character look ID
	 */
	public abstract String getActiveLook();

	/**
	 * Retrieves the active sanctuary look
	 * 
	 * @return Sanctuary look ID
	 */
	public abstract String getActiveSanctuaryLook();

	/**
	 * Assigns the active character look
	 * 
	 * @param lookID Character look ID
	 */
	public abstract void setActiveLook(String lookID);

	/**
	 * Assigns the active sanctuary look
	 * 
	 * @param lookID Sanctuary look ID
	 */
	public abstract void setActiveSanctuaryLook(String lookID);

}
