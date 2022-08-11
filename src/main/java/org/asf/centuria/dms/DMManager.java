package org.asf.centuria.dms;

import org.asf.centuria.dms.impl.FileBasedDMManager;

public abstract class DMManager {

	protected static DMManager implementation = new FileBasedDMManager();

	/**
	 * Retrieves the DM manager
	 * 
	 * @return AbstractDMManager instance
	 */
	public static DMManager getInstance() {
		return implementation;
	}

	/**
	 * Creates a new DM on disk
	 * 
	 * @param dmID         Conversation ID
	 * @param participants Array of participant IDs
	 */
	public abstract void openDM(String dmID, String[] participants);

	/**
	 * Checks if a specific DM is saved on disk
	 * 
	 * @param dmID Conversation ID
	 * @return True if the DM was saved on disk, false otherwise
	 */
	public abstract boolean dmExists(String dmID);;

	/**
	 * Retrieves the participants of a DM
	 * 
	 * @param dmID Conversation ID
	 * @return Array of Account ID Strings
	 */
	public abstract String[] getDMParticipants(String dmID);

	/**
	 * Retrieves the messages sent in a DM
	 * 
	 * @param dmID      Conversation ID
	 * @param requester Player requesting the history
	 * @return Array of PrivateChatMessage instances
	 */
	public abstract PrivateChatMessage[] getDMHistory(String dmID, String requester);

	/**
	 * Saves a DM message to disk or memory
	 * 
	 * @param dmID    Conversation ID
	 * @param message PrivateChatMessage to save
	 */
	public abstract void saveDMMessge(String dmID, PrivateChatMessage message);

	/**
	 * Deletes a DM
	 * 
	 * @param dmID Conversation ID to delete
	 */
	public abstract void deleteDM(String dmID);

}
