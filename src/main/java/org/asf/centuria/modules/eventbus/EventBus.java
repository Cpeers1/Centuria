package org.asf.centuria.modules.eventbus;

import java.util.function.Consumer;

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
	 * Adds event handlers
	 * 
	 * @param <T>          Event type
	 * @param eventClass   Event class
	 * @param eventHandler Event handler to add
	 * @since Beta 1.8
	 */
	public abstract <T extends EventObject> void addEventHandler(Class<T> eventClass, Consumer<T> eventHandler);

	/**
	 * Removes event handlers
	 * 
	 * @param <T>          Event type
	 * @param eventClass   Event class
	 * @param eventHandler Event handler to add
	 * @since Beta 1.8
	 */
	public abstract <T extends EventObject> void removeEventHandler(Class<T> eventClass, Consumer<T> eventHandler);

	/**
	 * Subscribes all events in a IEventReceiver object
	 * 
	 * @param receiver IEventReceiver to add
	 * @since Beta 1.8
	 */
	public abstract void addAllEventsFromReceiver(IEventReceiver receiver);

	/**
	 * Subscribes all events in a IEventReceiver object, deprecated alias of Beta
	 * 1.8's addAllEventsFromReceiver
	 * 
	 * @param receiver IEventReceiver to add
	 * @deprecated Deprecated since Beta 1.8 EventBus overhaul update, use
	 *             addAllEventsFromReceiver(receiver) instead
	 */
	@Deprecated
	public void addEventReceiver(IEventReceiver receiver) {
		addAllEventsFromReceiver(receiver);
	}

	/**
	 * Removes all subscribed events from a IEventReceiver object
	 * 
	 * @param receiver IEventReceiver to add
	 * @since Beta 1.8
	 */
	public abstract void removeAllEventsFromReceiver(IEventReceiver receiver);

	/**
	 * Dispatches a event
	 * 
	 * @param event Event to dispatch
	 */
	public abstract void dispatchEvent(EventObject event);

	/**
	 * Creates a new event bus
	 * 
	 * @return New EventBus instance
	 * @since Beta 1.8
	 */
	public abstract EventBus createBus();

}
