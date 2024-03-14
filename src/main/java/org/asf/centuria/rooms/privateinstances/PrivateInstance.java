package org.asf.centuria.rooms.privateinstances;

import org.asf.centuria.rooms.GameRoom;

/**
 * 
 * Private instance container
 * 
 * @author Sky Swimmer
 * 
 */
public interface PrivateInstance {

	/**
	 * Retrieves the ID of the private instance
	 * 
	 * @return Private instance ID string
	 */
	public String getID();

	/**
	 * Retrieves the private instance name
	 * 
	 * @return Private instance name
	 */
	public String getName();

	/**
	 * Updates the private instance name
	 * 
	 * @param name New private instance name
	 */
	public void setName(String name);

	/**
	 * Retrieves the private instance description
	 * 
	 * @return Private instance description
	 */
	public String getDescription();

	/**
	 * Updates the private instance description
	 * 
	 * @param description New private instance description
	 */
	public void setDescription(String description);

	/**
	 * Retrieves the private instance owner ID
	 * 
	 * @return Private instance owner ID
	 */
	public String getOwnerID();

	/**
	 * Updates the private instance owner ID
	 * 
	 * @param ownerID New private instance owner ID
	 */
	public void setOwnerID(String ownerID);

	/**
	 * Retrieves the private instance participant list
	 * 
	 * @return Array of participant IDs
	 */
	public String[] getParticipants();

	/**
	 * Updates the participant list
	 * 
	 * @param participants New participant list
	 */
	public void setParticipants(String[] participants);

	/**
	 * Checks if participants are present
	 * 
	 * @param participantID Player ID to check
	 * @return True if present, false otherwise
	 */
	public boolean isParticipant(String participantID);

	/**
	 * Changes if invites are allowed
	 * 
	 * @param allow True to allow, false otherwise
	 */
	public void setAllowInvites(boolean allow);

	/**
	 * Checks if invites are allowed
	 * 
	 * @return True if allowed, false otherwise
	 */
	public boolean allowInvites();

	/**
	 * Removes participants from the private instance
	 * 
	 * @param participantID Participant to remove
	 */
	public default void removeParticipant(String participantID) {
		if (!isParticipant(participantID))
			return;
		String[] participants = getParticipants();
		String[] newParticipants = new String[participants.length - 1];
		int i = 0;
		for (String id : participants) {
			if (!id.equals(participantID))
				newParticipants[i++] = id;
		}
		setParticipants(newParticipants);
	}

	/**
	 * Adds participants to the private instance
	 * 
	 * @param participantID Participant to add
	 */
	public default void addParticipant(String participantID) {
		if (isParticipant(participantID))
			return;
		String[] participants = getParticipants();
		String[] newParticipants = new String[participants.length + 1];
		int i = 0;
		for (String id : participants)
			newParticipants[i++] = id;
		newParticipants[i++] = participantID;
		setParticipants(newParticipants);
	}

	/**
	 * Retrieves the GameRoom instance by level ID for this private instance
	 * 
	 * @param levelID Level ID
	 * @return GameRoom instance
	 */
	public GameRoom getRoom(int levelID);

	/**
	 * Deletes the private instance
	 */
	public void delete();

}
