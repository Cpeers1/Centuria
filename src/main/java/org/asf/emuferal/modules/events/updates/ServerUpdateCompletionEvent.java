package org.asf.emuferal.modules.events.updates;

import java.util.Map;

import org.asf.emuferal.modules.eventbus.EventObject;
import org.asf.emuferal.modules.eventbus.EventPath;

/**
 * 
 * Update Completion Event - called when a server update finishes and when the
 * server is about to restart.
 * 
 * @author Sky Swimmer - AerialWorks Software Foundation
 *
 */
@EventPath("update.complete")
public class ServerUpdateCompletionEvent extends EventObject {

	private String updateVersion;

	public ServerUpdateCompletionEvent(String updateVersion) {
		this.updateVersion = updateVersion;
	}

	@Override
	public String eventPath() {
		return "update.complete";
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

	@Override
	public Map<String, String> eventProperties() {
		return Map.of("version", updateVersion);
	}
}
