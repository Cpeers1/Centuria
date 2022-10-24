package org.asf.centuria.levelevents;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.function.Consumer;

import org.apache.logging.log4j.MarkerManager;
import org.asf.centuria.Centuria;

/**
 * 
 * Level Event Bus
 * 
 * @author Sky Swimmer
 *
 */
public class LevelEventBus {

	private static HashMap<String, ArrayList<Consumer<LevelEvent>>> handlers = new HashMap<String, ArrayList<Consumer<LevelEvent>>>();

	/**
	 * Registers a level event handler
	 * 
	 * @param event   Event type to register a handler for
	 * @param handler Event handler
	 */
	public static void registerHandler(String event, Consumer<LevelEvent> handler) {
		if (!handlers.containsKey(event))
			handlers.put(event, new ArrayList<Consumer<LevelEvent>>());
		handlers.get(event).add(handler);
	}

	/**
	 * Dispatches a level event
	 * 
	 * @param event Event to dispatch
	 */
	public static void dispatch(LevelEvent event) {
		String tagsStr = "none";
		for (String tag : event.getTags()) {
			if (tagsStr.equals("none"))
				tagsStr = tag;
			else
				tagsStr += ", " + tag;
		}
		Centuria.logger.debug(MarkerManager.getMarker("LEVELING"), "Event: " + event.getType() + ", tags: " + tagsStr);
		if (!handlers.containsKey(event.getType()))
			return;
		while (true) {
			try {
				@SuppressWarnings("unchecked")
				Consumer<LevelEvent>[] eventHandlers = handlers.get(event.getType()).toArray(t -> new Consumer[t]);
				for (Consumer<LevelEvent> handler : eventHandlers)
					handler.accept(event);
				break;
			} catch (ConcurrentModificationException e) {
			}
		}
	}

}
