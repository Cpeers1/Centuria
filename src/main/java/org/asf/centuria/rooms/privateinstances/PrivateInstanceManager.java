package org.asf.centuria.rooms.privateinstances;

/**
 * 
 * Private instance management system - for private server instances
 * 
 * @author Sky Swimmer
 * 
 */
public abstract class PrivateInstanceManager {

	/**
	 * Creates private instances
	 * 
	 * @param owner       Private instance owner ID
	 * @param name        Private instance name
	 * @param description Private instance description
	 * @return PrivateInstance instance
	 */
	public abstract PrivateInstance createPrivateInstance(String owner, String name, String description);

	/**
	 * Retrieves private instances by ID
	 * 
	 * @param id Private instance ID
	 * @return PrivateInstance instance or null
	 */
	public abstract PrivateInstance getPrivateInstance(String id);

	/**
	 * Checks if private instances exist
	 * 
	 * @param id Private instance ID
	 * @return True if the instance exists, false otherwise
	 */
	public abstract boolean privateInstanceExists(String id);

	/**
	 * Deletes private instances
	 * 
	 * @param id Private instance ID to delete
	 */
	public void deleteInstance(String id) {
		if (!privateInstanceExists(id))
			return;
		getPrivateInstance(id).delete();
	}

	/**
	 * Retrieves the private instances a player is in
	 * 
	 * @param participant Participant ID
	 * @return Array of PrivateInstance objects the player is in
	 */
	public abstract PrivateInstance[] getJoinedInstancesOf(String participant);

	/**
	 * Retrieves the active private instance a player is in
	 * 
	 * @param participant Participant ID
	 * @return PrivateInstance instance or null
	 */
	public abstract PrivateInstance getSelectedInstanceOf(String participant);

	/**
	 * Assigns the selected private instance of a player
	 * 
	 * @param participant Participant ID
	 * @param instance    Private instance to assign as selected instance
	 */
	public abstract void setSelectedInstanceOf(String participant, PrivateInstance instance);

}
