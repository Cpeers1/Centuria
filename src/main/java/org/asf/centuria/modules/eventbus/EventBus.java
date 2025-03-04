package org.asf.centuria.modules.eventbus;

import org.asf.centuria.modules.eventbus.impl.EventBusImpl;

/**
 * 
 * The EventBus for Centuria modules, used to register and dispatch events.
 * 
 * @author Sky Swimmer - AerialWorks Software Foundation
 *
 */
public abstract class EventBus {

	protected static EventBus instance = new EventBusImpl();

	/**
	 * Retrieves the active event bus
	 * 
	 * @return EventBus instance
	 */
	public static EventBus getInstance() {
		return instance;
	}

	/**
	 * Subscribes all events in a IEventReceiver object
	 * 
	 * @param receiver IEventReceiver to add
	 */
	public abstract void addEventReceiver(IEventReceiver receiver);

	/**
	 * Dispatches a event
	 * 
	 * @param event Event to dispatch
	 */
	public abstract void dispatchEvent(EventObject event);

}
