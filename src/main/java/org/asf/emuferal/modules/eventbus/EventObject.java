package org.asf.emuferal.modules.eventbus;

/**
 * 
 * Abstract event object, needs to be annotated with EventPath for registration
 * to work.
 * 
 * @author Sky Swimmer - AerialWorks Software Foundation
 *
 */
public abstract class EventObject {

	/**
	 * Defines the event path
	 */
	public abstract String eventPath();

}
