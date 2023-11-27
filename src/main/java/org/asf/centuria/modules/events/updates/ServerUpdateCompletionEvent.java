package org.asf.centuria.modules.events.updates;

import org.asf.centuria.modules.eventbus.EventObject;

/**
 * 
 * Update Completion Event - called when a server update finishes and when the
 * server is about to restart.
 * 
 * @author Sky Swimmer - AerialWorks Software Foundation
 *
 */
public class ServerUpdateCompletionEvent extends EventObject {

	private String updateVersion;

	public ServerUpdateCompletionEvent(String updateVersion) {
		this.updateVersion = updateVersion;
	}

	/**
	 * Checks if the version property is present in the event object
	 * 
	 * @return True if the property is present, false otherwise
	 */
	public boolean hasVersionInfo() {
		return updateVersion != null;
	}

	/**
	 * Retrieves the version that will be installed
	 * 
	 * @return Update version string or null
	 */
	public String getUpdateVersion() {
		return updateVersion;
	}

}
