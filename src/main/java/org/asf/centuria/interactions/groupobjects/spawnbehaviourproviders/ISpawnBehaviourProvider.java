package org.asf.centuria.interactions.groupobjects.spawnbehaviourproviders;

import org.asf.centuria.entities.players.Player;
import org.asf.centuria.interactions.groupobjects.GroupObject;

public interface ISpawnBehaviourProvider {
	/**
	 * Behaviour ID
	 * 
	 * @return Behaviour ID string
	 */
	public String getID();

	/**
	 * Provides the current group object set
	 * 
	 * @param levelID Level ID
	 * @param plr Player being loaded
	 * @return Array of GroupObject instances
	 */
	public GroupObject[] provideCurrent(int levelID, Player plr);

	/**
	 *	Called after collecting a group object, this is for respawn locks
	 *
	 * @param player Player that picked up the object
	 * @param id Object ID
	 */
	public void onCollect(Player player, String id);
}
