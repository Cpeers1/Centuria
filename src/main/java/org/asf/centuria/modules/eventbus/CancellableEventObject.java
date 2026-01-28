package org.asf.centuria.modules.eventbus;

/**
 * 
 * Abstract event object, needs to be annotated with EventPath for registration
 * to work.
 * 
 * @author Sky Swimmer - AerialWorks Software Foundation
 *
 */
public abstract class CancellableEventObject extends EventObject {

	private boolean cancelled = false;

	/**
	 * Checks if the event has been marked as cancelled
	 * 
	 * @return True if the event has been cancelled, false otherwise
	 */
	public boolean isCancelled() {
		return cancelled;
	}

	/**
	 * Marks the event as cancelled
	 */
	public void setCancelled() {
		cancelled = true;
		setHandled();
	}

}
