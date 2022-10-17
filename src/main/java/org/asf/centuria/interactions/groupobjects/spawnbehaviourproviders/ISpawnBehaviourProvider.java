package org.asf.centuria.interactions.groupobjects.spawnbehaviourproviders;

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
	 * @return Array of GroupObject instances
	 */
	public GroupObject[] provideCurrent(int levelID);
}
