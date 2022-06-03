package org.asf.emuferal.modules.events.updates;

import java.util.Map;

import org.asf.emuferal.modules.eventbus.EventObject;
import org.asf.emuferal.modules.eventbus.EventPath;

/**
 * 
 * Update Event - called when a server update is started (either automated or
 * manually invoked)
 * 
 * @author Sky Swimmer - AerialWorks Software Foundation
 *
 */
@EventPath("update.start")
public class ServerUpdateEvent extends EventObject {

	private String updateVersion;
	private int minutes;

	public ServerUpdateEvent(String updateVersion, int minutes) {
		this.updateVersion = updateVersion;
		this.minutes = minutes;
	}

	@Override
	public String eventPath() {
		return "update.start";
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

	/**
	 * Checks if the update is being rolled out with a timer enabled
	 * 
	 * @return True if the update has a shutdown timer, false if its instant reboot
	 */
	public boolean hasTimer() {
		return minutes != -1;
	}

	/**
	 * Retrieves the amount of minutes before the server reboots to update
	 * 
	 * @return Minutes before update or -1
	 */
	public int getTimeRemaining() {
		return minutes;
	}

	@Override
	public Map<String, String> eventProperties() {
		return Map.of("version", updateVersion, "minutes", Integer.toString(minutes));
	}
}
