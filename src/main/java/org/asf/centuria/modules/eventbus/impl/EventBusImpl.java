package org.asf.centuria.modules.eventbus.impl;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;

import org.asf.centuria.modules.eventbus.EventBus;
import org.asf.centuria.modules.eventbus.EventListener;
import org.asf.centuria.modules.eventbus.EventObject;
import org.asf.centuria.modules.eventbus.EventPath;
import org.asf.centuria.modules.eventbus.IEventReceiver;

public class EventBusImpl extends EventBus {

	private HashMap<String, HashMap<Method, IEventReceiver>> listeners = new HashMap<String, HashMap<Method, IEventReceiver>>();

	@Override
	public void addEventReceiver(IEventReceiver receiver) {
		// Log subscription
		System.out.println("Registering all events in " + receiver.getClass().getTypeName() + "...");

		// Loop through the class and register events
		for (Method meth : receiver.getClass().getMethods()) {
			if (meth.isAnnotationPresent(EventListener.class) && Modifier.isPublic(meth.getModifiers())
					&& !Modifier.isAbstract(meth.getModifiers())) {
				// Find the event object
				if (meth.getParameterCount() == 1 && EventObject.class.isAssignableFrom(meth.getParameterTypes()[0])) {

					// Find event path
					Class<?> eventType = meth.getParameterTypes()[0];
					if (eventType.isAnnotationPresent(EventPath.class)) {
						EventPath info = eventType.getAnnotation(EventPath.class);

						// Add listener
						meth.setAccessible(true);
						String path = info.value();
						if (!listeners.containsKey(path))
							listeners.put(path, new HashMap<Method, IEventReceiver>());
						listeners.get(path).put(meth, receiver);
					}

				}
			}
		}
	}

	@Override
	public void dispatchEvent(EventObject event) {
		if (listeners.containsKey(event.eventPath())) {
			// Dispatch event
			HashMap<Method, IEventReceiver> listeners = this.listeners.get(event.eventPath());
			for (Method mth : listeners.keySet()) {
				if (event.isHandled())
					return; // End loop, event was handled

				try {
					mth.invoke(listeners.get(mth), event);
				} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					throw new RuntimeException(e);
				}
			}
		}
	}
}
