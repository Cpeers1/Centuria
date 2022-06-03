package org.asf.emuferal.modules.eventbus;

import java.util.Map;

/**
 * 
 * Abstract event object, needs to be annotated with EventPath for registration
 * to work.
 * 
 * @author Sky Swimmer - AerialWorks Software Foundation
 *
 */
public abstract class EventObject {

	private boolean handled = false;

	/**
	 * Defines the event path
	 */
	public abstract String eventPath();

	/**
	 * Checks if the event has been marked as handled
	 * 
	 * @return True if the event has been handled, false otherwise
	 */
	public boolean isHandled() {
		return handled;
	}

	/**
	 * Marks the event as handled
	 */
	public void setHandled() {
		handled = true;
	}

	/**
	 * Generates a map of event properties
	 * 
	 * @return Map of event properties
	 */
	public abstract Map<String, String> eventProperties();

	@Override
	public String toString() {
		String data = "";
		Map<String, String> props = eventProperties();
		for (String key : props.keySet()) {
			data += " " + key + ": " + props.get(key);
		}
		return "EventObject:[ name: " + eventPath() + "," + data + " ]";
	}

}
